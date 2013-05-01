/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.classloader.resource;

import java.io.File;
import java.net.URL;
import java.security.AccessController;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class are used as cache for resources downloaded from a driver or client, using the JPPF class loader APIs.
 * @author Laurent Cohen
 * @exclude
 */
public class ResourceCache
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ResourceCache.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * A map of all resource caches to their uuid.
   */
  private static final Map<String, ResourceCache> cacheMap = new Hashtable<String, ResourceCache>();
  /**
   * Name of the resource cache root.
   */
  private static String ROOT_NAME = ".jppf";
  /**
   * Map of resource names to temporary file names to which their content is stored.
   */
  private Map<String, List<Resource>> cache = new Hashtable<String, List<Resource>>();
  /**
   * List of temp folders used by this cache.
   */
  private List<String> tempFolders = new LinkedList<String>();
  /**
   * The unique identifier for this resource cache.
   */
  private final String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL_CHAR, 32).toString();
  /**
   * Shutdown hook for cleaning up this cache instance.
   */
  //private final Thread shutdownHook = ;

  /**
   * Default initializations.
   */
  public ResourceCache()
  {
    Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownHook(tempFolders, uuid)));
    cacheMap.put(uuid, this);
    initTempFolders();
  }

  /**
   * Get the list of locations for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @return a list of file paths, or null if the resource is not found in the cache.
   */
  public synchronized List<Resource> getResourcesLocations(final String name)
  {
    return cache.get(name);
  }

  /**
   * Get the list of locations for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @return a list of file paths, or null if the resource is not found in the cache.
   */
  public synchronized List<URL> getResourcesURLs(final String name)
  {
    List<Resource> resources = getResourcesLocations(name);
    if (resources == null) return null;
    List<URL> urls = new ArrayList<URL>(resources.size());
    int count = 0;
    for (Resource res: resources)
    {
      URL url = getResourceURL(name, res, count++);
      if (url != null) urls.add(url);
    }
    return urls;
  }

  /**
   * Get a location for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @return a file path, or null if the resource is not found in the cache.
   */
  private synchronized Resource getResourceLocation(final String name)
  {
    List<Resource> locations = cache.get(name);
    if ((locations == null) || locations.isEmpty()) return null;
    return locations.get(0);
  }

  /**
   * Set the list of locations for the resource with the specified name.
   * @param name the name of the resource to lookup.
   * @param locations a list of file paths.
   */
  private synchronized void setResourcesLocations(final String name, final List<Resource> locations)
  {
    cache.put(name, locations);
  }

  /**
   * Save the definitions for a resource to temporary files, and register their location with this cache.
   * @param name the name of the resource to register.
   * @param definitions a list of byte array definitions.
   */
  public synchronized void registerResources(final String name, final List<byte[]> definitions)
  {
    if (isAbsolutePath(name)) return;
    List<Resource> locations = new LinkedList<Resource>();
    for (byte[] def: definitions)
    {
      try
      {
        locations.add(saveToTempFile(name, def));
      }
      catch (Exception e)
      {
        String s = "Exception caught while saving resource named '" + name + "' : ";
        if (debugEnabled) log.debug(s, e);
        else log.warn(s + ExceptionUtils.getMessage(e));
      }
    }
    if (!locations.isEmpty()) setResourcesLocations(name, locations);
  }

  /**
   * Save the specified resource definition to a temporary file.
   * @param name the original name of the resource to save.
   * @param definition the definition to save, specified as a byte array.
   * @return the path to the created file.
   * @throws Exception if any I/O error occurs.
   */
  private Resource saveToTempFile(final String name, final byte[] definition) throws Exception
  {
    SaveResourceAction action = new SaveResourceAction(tempFolders, name, definition);
    Resource file = AccessController.doPrivileged(action);
    if (action.getException() != null) throw action.getException();
    if (traceEnabled) log.trace("saved resource [" + name + "] to file " + file);
    return file;
  }

  /**
   * Get the URL for a cached resource.
   * @param name the name of the resource to find.
   * @return resource location expressed as a URL.
   */
  public URL getResourceURL(final String name)
  {
    return getResourceURL(name,  getResourceLocation(name), 0);
  }

  /**
   * Get the URL for a cached resource.
   * @param name the name of the resource to find.
   * @param res the cached resource.
   * @param id the position of the url to fetch.
   * @return resource location expressed as a URL.
   */
  private URL getResourceURL(final String name, final Resource res, final int id)
  {
    if (res instanceof FileResource)
    {
      File path = ((FileResource) res).getPath();
      if (path == null) return null;
      return FileUtils.getURLFromFilePath(path);
    }
    else if (res instanceof MemoryResource)
    {
      String s = "jppfres://" + uuid + '/' + name + "?id=" + id;
      try
      {
        return new URL(s);
      }
      catch(Exception e)
      {
        return null;
      }
    }
    return null;
  }

  /**
   * Initializations of the temps folders.
   */
  private void initTempFolders()
  {
    try
    {
      String base = JPPFConfiguration.getProperties().getString("jppf.resource.cache.dir", null);
      if (base == null)
      {
        base = System.getProperty("java.io.tmpdir");
        if (base == null) base = System.getProperty("user.home");
        if (base == null) base = System.getProperty("user.dir");
        if (base != null)
        {
          if (!base.endsWith(File.separator)) base += File.separator;
          base += ROOT_NAME;
        }
      }
      if (base == null) base = "." + File.separator + ROOT_NAME;
      if (traceEnabled) log.trace("base = " + base);
      String s = base + File.separator + uuid;
      File baseDir = new File(s + File.separator);
      FileUtils.mkdirs(baseDir);
      tempFolders.add(s);
      if (traceEnabled) log.trace("added temp folder " + s);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Determine whether the specified path is absolute, in a system-independent way.
   * @param path the path to verify.
   * @return true if the path is absolute, false otherwise
   */
  private boolean isAbsolutePath(final String path)
  {
    if (path.startsWith("/") || path.startsWith("\\")) return true;
    if (path.length() < 3) return false;
    char c = path.charAt(0);
    if ((((c >= 'A') && (c <='Z')) || ((c >= 'a') && (c <= 'z'))) && (path.charAt(1) == ':')) return true;
    return false;
  }

  /**
   * A runnable invoked whenever this resource cache is garbage collected or the JVM shuts down,
   * so as to cleanup all cached resources on the file system.
   */
  private final static class ShutdownHook extends Thread
  {
    /**
     * The list of folders to delete.
     */
    private final List<String> tempFolders;
    /**
     * The uuid of the cache to clear.
     */
    private final String uuid;

    /**
     * Initialize this shutdown hook with the specified list of folders to delete.
     * @param tempFolders the list of folders to delete.
     * @param uuid the unique id of the cahce to remove.
     */
    private ShutdownHook(final List<String> tempFolders, final String uuid)
    {
      this.tempFolders = tempFolders;
      this.uuid = uuid;
    }

    @Override
    public void run()
    {
      System.out.println("cleaning up resource cache [uuid=" + uuid + "]");
      cacheMap.remove(uuid);
      while (!tempFolders.isEmpty()) FileUtils.deletePath(new File(tempFolders.remove(0)));
      
    }
  }

  /**
   * Close this resource cache and clean all resources it uses.
   */
  public synchronized void close()
  {
    /*
    try
    {
      Runtime.getRuntime().removeShutdownHook(shutdownHook);
    }
    catch (Exception ignore)
    {
    }
    */
    new ShutdownHook(tempFolders, uuid).run();
  }

  @Override
  protected void finalize() throws Throwable
  {
    close();
    super.finalize();
  }

  /**
   * Get the unique identifier for this resource cache.
   * @return the uuid as a string.
   */
  public String getUuid()
  {
    return uuid;
  }

  /**
   * Get a cache instance form its uuid.
   * @param uuid the uuid of the cache to find.
   * @return a <code>ResourceCache</code> instance.
   */
  public static ResourceCache getCacheInstance(final String uuid)
  {
    return cacheMap.get(uuid);
  }
}
