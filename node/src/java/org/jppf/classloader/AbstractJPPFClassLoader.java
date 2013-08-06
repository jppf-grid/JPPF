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
package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * This class is a custom class loader serving the purpose of dynamically loading the JPPF classes and the client
 * application classes, to avoid costly redeployment system-wide.
 * @author Laurent Cohen
 */
public abstract class AbstractJPPFClassLoader extends AbstractJPPFClassLoaderLifeCycle {
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
   * Initialize this class loader with a parent class loader.
   * @param connection the connection to the driver.
   * @param parent a ClassLoader instance.
   */
  public AbstractJPPFClassLoader(final ClassLoaderConnection connection, final ClassLoader parent) {
    super(connection, parent, null);
  }

  /**
   * Initialize this class loader with a parent class loader.
   * @param connection the connection to the driver.
   * @param parent a ClassLoader instance.
   * @param uuidPath unique identifier for the submitting application.
   */
  public AbstractJPPFClassLoader(final ClassLoaderConnection connection, final ClassLoader parent, final List<String> uuidPath) {
    super(connection, parent, uuidPath);
  }

  /**
   * Load a JPPF class from the server. This method bypasses the class loader delegation model
   * and the URL classpath and attempts to load the class definition directly from the server.
   * <p>In principle, this method is only used to load the class that performs object serialization
   * and deserialization, to ensure that classes available only remotely are properly downloaded from the server or client.
   * @param name the binary name of the class
   * @return the resulting <tt>Class</tt> object
   * @throws ClassNotFoundException if the class could not be found
   * @exclude
   */
  public synchronized Class<?> loadJPPFClass(final String name) throws ClassNotFoundException {
    if (debugEnabled) log.debug(build("looking up resource [", name, "]"));
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      if (debugEnabled) log.debug(build("resource [", name, "] not already loaded"));
      c = isOffline() ? Class.forName(name, true, this) : findClass(name, false);
    }
    if (debugEnabled) log.debug(build("definition for resource [", name, "] : ", c));
    if ((c != null) && debugEnabled) log.debug("class '" + name + "' loaded by " + c.getClassLoader());
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
  protected Class<?> findClass(final String name) throws ClassNotFoundException {
    return findClass(name, true);
  }

  /**
   * Find a class in this class loader's classpath.
   * @param name binary name of the resource to find.
   * @param lookupClasspath specifies whether the class should be looked up in the URL classpath as well.
   * @return a defined <code>Class</code> instance.
   * @throws ClassNotFoundException if the class could not be loaded.
   * @see java.lang.ClassLoader#findClass(java.lang.String)
   * @exclude
   */
  protected synchronized Class<?> findClass(final String name, final boolean lookupClasspath) throws ClassNotFoundException {
    Class<?> c = null;
    if (notFoundCache.has(name)) throw new ClassNotFoundException(build("Could not load class '", name, "'"));
    c = findLoadedClass(name);
    if (c != null) return c;
    if (lookupClasspath) {
      c = findClassInURLClasspath(name, false);
      if (c != null) {
        fireEvent(c, null, true);
        return c;
      }
    }
    int i = name.lastIndexOf('.');
    if (i >= 0) {
      String pkgName = name.substring(0, i);
      synchronized(this) {
        Package pkg = getPackage(pkgName);
        if (pkg == null) definePackage(pkgName, null, null, null, null, null, null, null);
      }
    }
    if (isOffline()) {
      notFoundCache.add(name);
      throw new ClassNotFoundException(build("Could not load class '", name, "'"));
    }
    if (debugEnabled) log.debug(build("looking up definition for resource [", name, "]"));
    byte[] b = null;
    String resName = name.replace('.', '/') + ".class";
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("name", resName);
    JPPFResourceWrapper resource = null;
    synchronized(this) {
      resource = loadResource(map);
    }
    if (resource != null) b = resource.getDefinition();
    if ((b == null) || (b.length == 0)) {
      if (debugEnabled) log.debug("definition for resource [" + name + "] not found");
      if ((resource != null) && (resource.getState() != JPPFResourceWrapper.State.NODE_RESPONSE_ERROR)) notFoundCache.add(name);
      fireEvent(null, name, false);
      throw new ClassNotFoundException(build("Could not load class '", name, "'"));
    }
    if (debugEnabled) log.debug(build("found definition for resource [", name, ", definitionLength=", b.length, "]"));
    synchronized(this) {
      c = findLoadedClass(name);
      if (c == null) c = defineClass(name, b, 0, b.length);
      fireEvent(c, null, false);
      return c;
    }
  }

  /**
   * Compute a value on the client-side, as the result of the execution of a {@link JPPFCallable}.
   * <p>Any {@link Throwable} raised in the callable's <code>call()</code> method will be thrown as the result of this method.
   * If the Throwable is an instance of <code>Exception</code> or one of its subclasses, it is thrown as such, otherwise it is wrapped
   * into a {@link org.jppf.JPPFException}.
   * @param <V> the type of results returned by the callable.
   * @param callable the callable to execute on the client side.
   * @return the value computed on the client, or null if the value could not be computed.
   * @throws Exception if the execution of the callable in the client resulted in a {@link Throwable} being raised.
   */
  @SuppressWarnings("unchecked")
  public <V> V computeCallable(final JPPFCallable<V> callable) throws Exception {
    V result = null;
    Object returned = null;
    Class clazz = loadJPPFClass("org.jppf.utils.ObjectSerializerImpl");
    ClassLoader cl = clazz.getClassLoader();
    ObjectSerializer ser = (ObjectSerializer) clazz.newInstance();
    byte[] bytes = ser.serialize(callable).getBuffer();
    bytes = computeRemoteData(bytes);
    if (bytes == null) return null;
    returned = ser.deserialize(bytes);
    if (returned instanceof Exception) throw (Exception) returned;
    return (V) returned;
  }

  /**
   * Request the remote computation of a <code>JPPFCallable</code> on the client.
   * @param callable the serialized callable to execute remotely.
   * @return an array of bytes containing the result of the callable's execution.
   * @throws Exception if the connection was lost and could not be reestablished.
   * @exclude
   */
  public byte[] computeRemoteData(final byte[] callable) throws Exception {
    if (debugEnabled) log.debug(build(this, " requesting remote computation, requestUuid = ", requestUuid));
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("name", "callable");
    map.put("callable", callable);
    JPPFResourceWrapper resource = connection.loadResource(map, dynamic, requestUuid, uuidPath);
    byte[] b = null;
    if ((resource != null) && (resource.getState() == JPPFResourceWrapper.State.NODE_RESPONSE)) b = resource.getCallable();
    if (debugEnabled) log.debug(build(this, " remote definition for callable resource ", b==null ? "not " : "", "found"));
    return b;
  }

  /**
   * Finds the resource with the specified name.
   * The resource lookup order is the same as the one specified by {@link #getResourceAsStream(String)}
   * @param name the name of the resource to find.
   * @return the URL of the resource.
   */
  @Override
  public URL findResource(final String name) {
    URL url = null;
    if (notFoundCache.has(name)) return null;
    url = resourceCache.getResourceURL(name);
    if (debugEnabled) log.debug(build(this, " resource [", name, "] ", url == null ? "not " : "", "found in local cache"));
    if (url == null) {
      url = super.findResource(name);
      if (debugEnabled) log.debug(build(this, " resource [", name, "] ", url == null ? "not " : "", "found in URL classpath"));
      if (!isOffline() && (url == null)) {
        if (debugEnabled) log.debug(build(this, " resource [", name, "] not found locally, attempting remote lookup"));
        try {
          List<URL> urlList = findRemoteResources(name);
          if ((urlList != null) && !urlList.isEmpty()) url = urlList.get(0);
        } catch(Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.warn(ExceptionUtils.getMessage(e));
        }
        if (debugEnabled) log.debug(build(this, " resource [", name, "] ", url == null ? "not " : "", "found remotely"));
      }
    }
    if (url == null) notFoundCache.add(name);
    return url;
  }

  /**
   * Get a stream from a resource file accessible from this class loader.
   * @param name name of the resource to obtain a stream from.
   * @return an <code>InputStream</code> instance, or null if the resource was not found.
   */
  @Override
  public InputStream getResourceAsStream(final String name) {
    InputStream is = null;
    try {
      URL url = getResource(name);
      if (url != null) {
        URLConnection connection = url.openConnection();
        is = connection.getInputStream();
      }
      if (debugEnabled) log.debug(build(this, " lookup for '", name, "' = ", url, " for ", this));
    } catch(IOException e) {
    }
    return is;
  }

  /**
   * Find all resources with the specified name.
   * @param name name of the resources to find in the class loader's classpath.
   * @return An enumeration of URLs pointing to the resources found.
   * @throws IOException if an error occurs.
   */
  @Override
  @SuppressWarnings("unchecked")
  public Enumeration<URL> findResources(final String name) throws IOException {
    List<URL> urlList = new ArrayList<URL>();
    if (!notFoundCache.has(name)) {
      if (debugEnabled) log.debug(build(this, " resource [", name, "] not found locally, attempting remote lookup"));
      try {
        urlList = resourceCache.getResourcesURLs(name);
        if (urlList == null) urlList = new ArrayList<URL>();
        if (!isOffline()) {
          List<URL> tempList = findRemoteResources(name);
          if (tempList != null) urlList.addAll(tempList);
        }
        Enumeration<URL> tempEnum = super.findResources(name);
        if (tempEnum != null) {
          while (tempEnum.hasMoreElements()) urlList.add(tempEnum.nextElement());
        }
      } catch(Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
        throw (e instanceof IOException) ? (IOException) e : new IOException(e);
      }
    }
    if (urlList.isEmpty()) notFoundCache.add(name);
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
  private List<URL> findRemoteResources(final String name) throws Exception {
    List<URL> urlList = new ArrayList<URL>();
    JPPFResourceWrapper resource = null;
    if (!notFoundCache.has(name)) {
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("name", name);
      map.put("multiple", "true");
      resource = loadResource(map);
      List<byte[]> dataList = null;
      if (resource != null) dataList = (List<byte[]>) resource.getData("resource_list");
      boolean found = (dataList != null) && !dataList.isEmpty();
      if (debugEnabled) log.debug(build(this, "resource [", name, "] ", found ? "" : "not ", "found remotely"));
      if (found) {
        resourceCache.registerResources(name, dataList);
        urlList = resourceCache.getResourcesURLs(name);
      }
    }
    if ((urlList == null) || urlList.isEmpty() &&
        ((resource != null) && (resource.getState() != JPPFResourceWrapper.State.NODE_RESPONSE_ERROR))) notFoundCache.add(name);
    return urlList;
  }

  /**
   * {@inheritDoc}
   * @exclude
   */
  @Override
  protected synchronized Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
    DelegationModel model = getDelegationModel();
    switch(model) {
      case URL_FIRST: return loadClassLocalFirst(name, resolve);
      case PARENT_FIRST: return super.loadClass(name, resolve);
    }
    throw new IllegalStateException("unknown class loader delegation model " + model);
  }

  /**
   * Load the class with the specified binary name, searching in the local (to the JVM) URL classpath first.
   * @param name the binary name of the class.
   * @param resolve if true then resolve the class.
   * @return the resulting Class object.
   * @throws ClassNotFoundException if the class could not be found.
   */
  private synchronized Class<?> loadClassLocalFirst(final String name, final boolean resolve) throws ClassNotFoundException {
    Class<?> c = findLoadedClass(name);
    if(c == null) {
      ClassLoader cl = initSystemClassLoader();
      if(cl != null) {
        try {
          c = cl.loadClass(name);
        } catch (ClassNotFoundException ignore) {
        }
      }
    }
    if (c == null) {
      ClassLoader p = getParent();
      boolean jppfCL = p instanceof AbstractJPPFClassLoader;
      if (!jppfCL) {
        try {
          c = p.loadClass(name);
        } catch(ClassNotFoundException ignore) {
        }
      }
      else c = ((AbstractJPPFClassLoader) p).findClassInURLClasspath(name, false);
      if (c == null) c = findClassInURLClasspath(name, false);
      if ((c == null) && jppfCL) {
        try {
          c = ((AbstractJPPFClassLoader) p).findClass(name, false);
        } catch(ClassNotFoundException ignore){
        }
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
  protected synchronized ClassLoader initSystemClassLoader() {
    if(!systemClassLoaderInitialized) {
      systemClassLoaderInitialized = true;
      try {
        systemClassLoader = getSystemClassLoader();
      } catch (Exception e) {
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
  protected synchronized Class<?> findClassInURLClasspath(final String name, final boolean recursive){
    if (debugEnabled) log.debug(build("looking up up resource [", name, "] in the URL classpath for ", this));
    Class<?> c = findLoadedClass(name);
    if (c == null) {
      if (recursive && (getParent() instanceof AbstractJPPFClassLoader)) {
        c = ((AbstractJPPFClassLoader) getParent()).findClassInURLClasspath(name, recursive);
      }
      if (c == null) {
        try {
          c = super.findClass(name);
        } catch(ClassNotFoundException ignore) {
        }
      }
    }
    if (debugEnabled) log.debug(build("resource [", name, "] ", c == null ? "not " : "", "found in the URL classpath for ", this));
    return c;
  }

  /**
   * Determine the class loading delegation model currently in use.
   * @return an int value representing the model, either {@link DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link DelegationModel#URL_FIRST URL_FIRST}.
   */
  public static synchronized DelegationModel getDelegationModel() {
    return delegationModel;
  }

  /**
   * Specify the class loading delegation model to use.
   * @param model an int value, either {@link DelegationModel#PARENT_FIRST PARENT_FIRST} or {@link DelegationModel#URL_FIRST URL_FIRST}.
   * If any other value is specified then calling this method has no effect.
   */
  public static synchronized void setDelegationModel(final DelegationModel model) {
    if (model != null) AbstractJPPFClassLoader.delegationModel = model;
  }

  /**
   * Initialize the delegation model from the JPPF configuration.
   * @return the delegation model indicator as computed from the configuration.
   */
  private static synchronized DelegationModel initDelegationModel() {
    String s = JPPFConfiguration.getProperties().getString("jppf.classloader.delegation", "parent");
    DelegationModel model = "url".equalsIgnoreCase(s) ? DelegationModel.URL_FIRST : DelegationModel.PARENT_FIRST;
    if (debugEnabled) log.debug(build("Using ", model, " class loader delegation model"));
    return model;
  }

  /**
   * Clear the cache of resources not found.
   * The main usage for this method is when libraries or folders have been dynamically added to this class loader's classpath.
   */
  public void clearNotFoundCache() {
    notFoundCache.clear();
  }

  @Override
  public void close() {
    resourceCache.close();
    notFoundCache.clear();
    super.close();
  }

  /**
   * Notify all listeners that a class was either loaded or not found.
   * @param c the class object if the class was successfully loaded, <code>null</code> otherwise.
   * @param name the name of the class if it was not found, this parameter is ignored if the class was found.
   * @param foundInURLClassPath <code>true</code> if the class was loaded from the class loader's URL classpath,
   * <code>false</code> if it was loaded from a remote JPPF driver or client.
   */
  protected void fireEvent(final Class<?> c, final String name, final boolean foundInURLClassPath) {
    boolean found = c != null;
    ClassLoaderEvent event = found ? new ClassLoaderEvent(this, c, foundInURLClassPath) : new ClassLoaderEvent(this, name);
    HookFactory.invokeHook(ClassLoaderListener.class, found ? "classLoaded" : "classNotFound", event);
  }

  /**
   * Determine whether this class loader accesses the classpath of a remote client.
   * @return <code>true</code> if this class loader is a client class loader, <code>false</code> if it is a server class loader.
   */
  public boolean isClientClassLoader() {
    return dynamic;
  }

  /**
   * Determine whether this class loader accesses the classpath of a remote driver/server.
   * @return <code>true</code> if this class loader is a server class loader, <code>false</code> if it is a client class loader.
   */
  public boolean isServerClassLoader() {
    return !dynamic;
  }
}
