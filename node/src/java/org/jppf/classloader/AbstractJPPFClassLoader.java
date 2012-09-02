/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
package org.jppf.classloader;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClassLoader extends AbstractJPPFClassLoaderLifeCycle
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJPPFClassLoader.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines the class loading delegation model to use.
   */
  private static DelegationModel delegationModel = initDelegationModel();
  /**
   * System classloader for URL_FIRST delegation model.
   */
  private ClassLoader systemClassLoader = null;
  /**
   * Determines whether system classloader was initialized.
   */
  private boolean systemClassLoaderInitialized = false;
  /**
   * Provides a lock in <code>findClass()</code> to avoid duplicate class definition attempts.
   */
  private Object findClassLock = new Object();

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   */
  public AbstractJPPFClassLoader(final ClassLoader parent)
  {
    super(parent);
  }

  /**
   * Initialize this class loader with a parent class loader.
   * @param parent a ClassLoader instance.
   * @param uuidPath unique identifier for the submitting application.
   */
  public AbstractJPPFClassLoader(final ClassLoader parent, final List<String> uuidPath)
  {
    super(parent, uuidPath);
  }

  /**
   * Load a JPPF class from the server.
   * @param name the binary name of the class
   * @return the resulting <tt>Class</tt> object
   * @throws ClassNotFoundException if the class could not be found
   * @exclude
   */
  public /*synchronized*/ Class<?> loadJPPFClass(final String name) throws ClassNotFoundException
  {
    if (debugEnabled) log.debug("looking up resource [" + name + ']');
    Class<?> c = findLoadedClass(name);
    if ((c != null) && debugEnabled) log.debug("classloader = " + c.getClassLoader());
    if (c == null)
    {
      if (debugEnabled) log.debug("resource [" + name + "] not already loaded");
      c = findClass(name, true);
    }
    if (debugEnabled) log.debug("definition for resource [" + name + "] : " + c);
    return c;
  }

  /**
   * Find a class in this class loader's classpath.
   * @param name binary name of the resource to find.
   * @return a defined <code>Class</code> instance.
   * @throws ClassNotFoundException if the class could not be loaded.
   * @see java.lang.ClassLoader#findClass(java.lang.String)
   */
  @Override
  protected Class<?> findClass(final String name) throws ClassNotFoundException
  {
    return findClass(name, true);
  }

  /**
   * Find a class in this class loader's classpath.
   * @param name binary name of the resource to find.
   * @param lookupClasspath specifies whether the class should be looked up in the URL classpath as well.
   * @return a defined <code>Class</code> instance.
   * @throws ClassNotFoundException if the class could not be loaded.
   * @see java.lang.ClassLoader#findClass(java.lang.String)
   */
  protected Class<?> findClass(final String name, final boolean lookupClasspath) throws ClassNotFoundException
  {
    synchronized(findClassLock)
    {
      Class<?> c = findLoadedClass(name);
      if (c != null) return c;
      if (lookupClasspath) c = findClassInURLClasspath(name, false);
      if (c != null) return c;
      int i = name.lastIndexOf('.');
      if (i >= 0)
      {
        String pkgName = name.substring(0, i);
        Package pkg = getPackage(pkgName);
        if (pkg == null)
        {
          definePackage(pkgName, null, null, null, null, null, null, null);
        }
      }
      if (debugEnabled) log.debug("looking up definition for resource [" + name + ']');
      byte[] b = null;
      String resName = name.replace('.', '/') + ".class";
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("name", resName);
      JPPFResourceWrapper resource = loadResourceData(map, false);
      if (resource == null) throw new ClassNotFoundException("could not find resource " + name);
      b = resource.getDefinition();
      if ((b == null) || (b.length == 0))
      {
        if (debugEnabled) log.debug("definition for resource [" + name + "] not found");
        throw new ClassNotFoundException("Could not load class '" + name + '\'');
      }
      if (debugEnabled) log.debug("found definition for resource [" + name + ", definitionLength=" + b.length + ']');
      return defineClass(name, b, 0, b.length);
    }
  }

  /**
   * Request the remote computation of a <code>JPPFCallable</code> on the client.
   * @param callable the serialized callable to execute remotely.
   * @return an array of bytes containing the result of the callable's execution.
   * @throws Exception if the connection was lost and could not be reestablished.
   */
  public byte[] computeRemoteData(final byte[] callable) throws Exception
  {
    if (debugEnabled) log.debug("requesting remote computation, requestUuid = " + requestUuid);
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("name", "callable");
    map.put("callable", callable);
    byte[] b = loadRemoteData(map, false).getCallable();
    if (debugEnabled) log.debug("remote definition for callable resource "+ (b==null ? "not " : "") + "found");
    return b;
  }

  /**
   * Finds the resource with the specified name.
   * The resource lookup order is the same as the one specified by {@link #getResourceAsStream(String)}
   * @param name the name of the resource to find.
   * @return the URL of the resource.
   * @see java.lang.ClassLoader#findResource(java.lang.String)
   */
  @Override
  public URL findResource(final String name)
  {
    URL url = null;
    url = cache.getResourceURL(name);
    if (debugEnabled) log.debug("resource [" + name + "] " + (url == null ? "not " : "") + "found in local cache");
    if (url == null)
    {
      url = super.findResource(name);
      if (debugEnabled) log.debug("resource [" + name + "] " + (url == null ? "not " : "") + "found in URL classpath");
      if (url == null)
      {
        if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
        try
        {
          List<URL> urlList = findRemoteResources(name);
          if ((urlList != null) && !urlList.isEmpty()) url = urlList.get(0);
        }
        catch(Exception e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
        }
        if (debugEnabled) log.debug("resource [" + name + "] " + (url == null ? "not " : "") + "found remotely");
      }
    }
    return url;
  }

  /**
   * Get a stream from a resource file accessible form this class loader.
   * The lookup order is defined as follows:
   * <ul>
   * <li>locally, in the classpath for this class loader, such as specified by {@link java.lang.ClassLoader#getResourceAsStream(java.lang.String) ClassLoader.getResourceAsStream(String)}<br>
   * <li>if the parent of this class loader is NOT an instance of {@link AbstractJPPFClassLoader},
   * in the classpath of the <i>JPPF driver</i>, such as specified by {@link org.jppf.classloader.ResourceProvider#getResourceAsBytes(java.lang.String, java.lang.ClassLoader) ResourceProvider.getResourceAsBytes(String, ClassLoader)}</li>
   * (the search may eventually be sped up by looking up the driver's resource cache first)</li>
   * <li>if the parent of this class loader IS an instance of {@link AbstractJPPFClassLoader},
   * in the <i>classpath of the JPPF client</i>, such as specified by {@link org.jppf.classloader.ResourceProvider#getResourceAsBytes(java.lang.String, java.lang.ClassLoader) ResourceProvider.getResourceAsBytes(String, ClassLoader)}
   * (the search may eventually be sped up by looking up the driver's resource cache first)</li>
   * </ul>
   * @param name name of the resource to obtain a stream from.
   * @return an <code>InputStream</code> instance, or null if the resource was not found.
   * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
   */
  @Override
  public InputStream getResourceAsStream(final String name)
  {
    InputStream is = null;
    try
    {
      URL url = getResource(name);
      if (url != null) is = url.openStream();
    }
    catch(IOException e)
    {
    }
    return is;
  }

  /**
   * Find all resources with the specified name.
   * @param name name of the resources to find in the class loader's classpath.
   * @return An enumeration of URLs pointing to the resources found.
   * @throws IOException if an error occurs.
   * @see java.lang.ClassLoader#findResources(java.lang.String)
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration<URL> findResources(final String name) throws IOException
  {
    List<URL> urlList = new ArrayList<URL>();
    if (debugEnabled) log.debug("resource [" + name + "] not found locally, attempting remote lookup");
    try
    {
      List<String> locationsList = cache.getResourcesLocations(name);
      if (locationsList != null)
      {
        urlList = new ArrayList<URL>();
        for (String path: locationsList) urlList.add(new File(path).toURI().toURL());
      }
      else
      {
        List<URL> tempList = findRemoteResources(name);
        if (tempList != null) urlList.addAll(tempList);
      }
      Enumeration<URL> tempEnum = super.findResources(name);
      if (tempEnum != null)
      {
        while (tempEnum.hasMoreElements()) urlList.add(tempEnum.nextElement());
      }
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
      throw (e instanceof IOException) ? (IOException) e : new IOException(e);
    }
    return new IteratorEnumeration<URL>(urlList.iterator());
  }

  /**
   * Find all resources with the specified name.
   * @param name name of the resources to find in the class loader's classpath.
   * @return A list of URLs pointing to the resources found.
   * @throws Exception if an error occurs.
   * @see java.lang.ClassLoader#findResources(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  private List<URL> findRemoteResources(final String name) throws Exception
  {
    List<URL> urlList = null;
    List<String> locationsList = null;
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("name", name);
    map.put("multiple", "true");
    JPPFResourceWrapper resource = loadResourceData(map, true);
    List<byte[]> dataList = (List<byte[]>)resource.getData("resource_list");
    boolean found = (dataList != null) && !dataList.isEmpty();
    if (debugEnabled) log.debug("resource [" + name + "] " + (found ? "" : "not ") + "found remotely");
    if (found)
    {
      cache.registerResources(name, dataList);
      urlList = new ArrayList<URL>();
      locationsList = cache.getResourcesLocations(name);
    }
    if (locationsList != null)
    {
      if (urlList == null) urlList = new ArrayList<URL>();
      for (String path: locationsList) urlList.add(new File(path).toURI().toURL());
      if (debugEnabled) log.debug("found the following URLs for resource [" + name + "] : " + urlList);
    }
    return urlList;
  }

  /**
   * Get multiple resources, specified by their names, from the classpath.
   * This method functions like #getResource(String), except that it look up and returns multiple URLs.
   * @param names the names of te resources to find.
   * @return an array of URLs, one for each looked up resources. Some URLs may be null, however the returned array
   * is never null, and results are in the same order as the specified resource names.
   */
  @SuppressWarnings("unchecked")
  protected URL[] findMultipleResources(final String...names)
  {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    URL[] results = new URL[names.length];
    for (int i=0; i<results.length; i++) results[i] = null;
    try {
      List<Integer> indices = new ArrayList<Integer>();
      for (int i=0; i<names.length; i++) {
        String name = names[i];
        List<String> locationsList = cache.getResourcesLocations(name);
        if ((locationsList != null) && !locationsList.isEmpty()) {
          results[i] = cache.getURLFromPath(locationsList.get(0));
          if (debugEnabled) log.debug("resource " + name + " found in local cache as " + results[i]);
        } else {
          URL url = super.findResource(names[i]);
          if (url != null) {
            results[i] = url;
            if (debugEnabled) log.debug("resource " + name + " found in URL classpath as " + results[i]);
          } else {
            if (debugEnabled) log.debug("resource " + name + " not found locally");
            indices.add(i);
          }
        }
      }
      if (indices.isEmpty()) {
        if (debugEnabled) log.debug("all resources were found locally");
        return results;
      }
      Map<String, Object> map = new HashMap<String, Object>();
      String[] namesToLookup = new String[indices.size()];
      for (int i=0; i<indices.size(); i++) namesToLookup[i] = names[indices.get(i)];
      map.put("name", StringUtils.arrayToString(namesToLookup, ", ", null, null));
      map.put("multiple.resources.names", namesToLookup);
      JPPFResourceWrapper resource = loadResourceData(map, true);
      Map<String, List<byte[]>> dataMap = (Map<String, List<byte[]>>) resource.getData("resource_map");
      for (Integer index : indices) {
        String name = names[index];
        List<byte[]> dataList = dataMap.get(name);
        boolean found = (dataList != null) && !dataList.isEmpty();
        if (debugEnabled && !found) log.debug("resource [" + name + "] not found remotely");
        if (found) {
          cache.registerResources(name, dataList);
          URL url = cache.getResourceURL(name);
          results[index] = url;
          if (debugEnabled) log.debug("resource [" + name + "] found remotely as " + url);
        }
      }
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    }
    return results;
  }

  /**
   * Get multiple resources, specified by their names, from the classpath.
   * This method functions like #getResource(String), except that it looks up and returns multiple URLs.
   * @param names the names of te resources to find.
   * @return an array of URLs, one for each looked up resources. Some URLs may be null, however the returned array
   * is never null, and results are in the same order as the specified resource names.
   */
  public URL[] getMultipleResources(final String...names)
  {
    if ((names == null) || (names.length <= 0)) return StringUtils.ZERO_URL;
    int length = names.length;
    URL[] results = new URL[length];
    for (int i=0; i<length; i++) results[i] = null;
    try
    {
      ClassLoader parent = getParent();
      if (parent == null)
      {
        for (int i=0; i<length; i++) results[i] = getSystemResource(names[i]);
      }
      else if (!(parent instanceof AbstractJPPFClassLoader))
      {
        for (int i=0; i<length; i++) results[i] = parent.getResource(names[i]);
      }
      else
      {
        results = ((AbstractJPPFClassLoader) parent).getMultipleResources(names);
      }
      for (int i=0; i<length; i++) if (results[i] == null) results[i] = super.getResource(names[i]);
      List<Integer> indices = new ArrayList<Integer>();
      for (int i=0; i<length; i++) if (results[i] == null) indices.add(i);
      if (!indices.isEmpty())
      {
        String[] namesToFind = new String[indices.size()];
        for (int i=0; i<namesToFind.length; i++) namesToFind[i] = names[indices.get(i)];
        URL[] foundURLs = findMultipleResources(namesToFind);
        for (int i=0; i<namesToFind.length; i++) results[indices.get(i)] = foundURLs[i];
      }
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return results;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  protected /*synchronized*/ Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException
  {
    if (getDelegationModel() == DelegationModel.URL_FIRST)
      return loadClassLocalFirst(name, resolve);
    else
      return super.loadClass(name, resolve);
  }

  /**
   * Load the class with the specified binary name, searching in the local (to the JVM) URL classpath first.
   * @param name the binary name of the class.
   * @param resolve if true then resolve the class.
   * @return the resulting Class object.
   * @throws ClassNotFoundException if the class could not be found.
   */
  private /*synchronized*/ Class<?> loadClassLocalFirst(final String name, final boolean resolve) throws ClassNotFoundException
  {
    Class<?> c = findLoadedClass(name);
    if(c == null)
    {
      ClassLoader cl = initSystemClassLoader();
      if(cl != null) {
        try
        {
          c = cl.loadClass(name);
        } catch (ClassNotFoundException e)
        {
          // ignore
        }
      }
    }
    if (c == null)
    {
      ClassLoader p = getParent();
      boolean jppfCL = p instanceof AbstractJPPFClassLoader;
      if (!jppfCL)
      {
        try
        {
          c = p.loadClass(name);
        }
        catch(ClassNotFoundException ignore){}
      }
      else c = ((AbstractJPPFClassLoader) p).findClassInURLClasspath(name, false);
      if (c == null) c = findClassInURLClasspath(name, false);
      if ((c == null) && jppfCL)
      {
        try
        {
          c = ((AbstractJPPFClassLoader) p).findClass(name, false);
        }
        catch(ClassNotFoundException ignore){}
      }
      if (c == null) c = findClass(name, false);

    }
    if (resolve) resolveClass(c);
    return c;
  }

  /**
   * Initializes system classloader for URL_FIRST delegation model.
   * @return instance of system ClassLoader or null if not available.
   * @exclude
   */
  protected synchronized ClassLoader initSystemClassLoader()
  {
    if(!systemClassLoaderInitialized) {
      systemClassLoaderInitialized = true;
      try
      {
        systemClassLoader = getSystemClassLoader();
      } catch (Exception e)
      {
        if (debugEnabled) log.debug(e.getMessage(), e);
      }
    }
    return systemClassLoader;
  }

  /**
   * Find a class in this class loader's classpath.
   * @param name binary name of the resource to find.
   * @param recursive if true then look recursively into the hierarchy of parents that are instances of <code>AbstractJPPFClassLoader</code>.
   * @return a <code>Class</code> instance, or null if the class could not be found in the URL classpath.
   * @exclude
   */
  protected /*synchronized*/ Class<?> findClassInURLClasspath(final String name, final boolean recursive)
  {
    if (debugEnabled) log.debug("looking up up resource [" + name + "] in the URL classpath for " + this);
    Class<?> c = findLoadedClass(name);
    if (c == null)
    {
      if (recursive && (getParent() instanceof AbstractJPPFClassLoader))
      {
        c = ((AbstractJPPFClassLoader) getParent()).findClassInURLClasspath(name, recursive);
      }
      if (c == null)
      {
        try
        {
          c = super.findClass(name);
        }
        catch(ClassNotFoundException ignore){}
      }
    }
    if (debugEnabled) log.debug("resource [" + name + "] " + (c == null ? "not " : "") + "found in the URL classpath for " + this);
    return c;
  }

  /**
   * Determine the class loading delegation model currently in use.
   * @return an int value representing the model, either {@link DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link DelegationModel#URL_FIRST URL_FIRST}.
   */
  public static synchronized DelegationModel getDelegationModel()
  {
    return delegationModel;
  }

  /**
   * Specify the class loading delegation model to use.
   * @param model an int value, either {@link DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link DelegationModel#URL_FIRST URL_FIRST}.
   * If any other value is specified then calling this method has no effect.
   */
  public static synchronized void setDelegationModel(final DelegationModel model)
  {
    if (model != null) AbstractJPPFClassLoader.delegationModel = model;
  }

  /**
   * Initialize the delegation model from the JPPF configuration.
   * @return the delegation model indicator as computed from the configuration.
   */
  private static synchronized DelegationModel initDelegationModel()
  {
    String s = JPPFConfiguration.getProperties().getString("jppf.classloader.delegation", "parent");
    DelegationModel model = "url".equalsIgnoreCase(s) ? DelegationModel.URL_FIRST : DelegationModel.PARENT_FIRST;
    if (debugEnabled) log.debug("Using " + model + " class loader delegation model");
    return model;
  }
}
