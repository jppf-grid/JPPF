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
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Instances of this class are dedicated to reading resource files form the JVM's classpath and converting them into
 * arrays of bytes.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class ResourceProvider
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ResourceProvider.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Default constructor.
   */
  public ResourceProvider()
  {
  }

  /**
   * Load a resource file (including class files) from the class path into an array of byte.<br>
   * This method simply calls {@link #getResourceAsBytes(java.lang.String, java.lang.ClassLoader) getResourceAsBytes(String, ClassLoader)}
   * with a null class loader.
   * @param resName the name of the resource to load.
   * @return an array of bytes, or nll if the resource could not be found.
   */
  public byte[] getResourceAsBytes(final String resName)
  {
    return getResourceAsBytes(resName, null);
  }

  /**
   * Load a resource file (including class files) from the class path or the file system into an array of byte.
   * The search order is defined as follows:<br>
   * - first the search is performed in the order specified by {@link java.lang.ClassLoader#getResourceAsStream(java.lang.String) ClassLoader.getResourceAsStream(String)}<br>
   * - if the resource is not found, it will be looked up in the file system <br>
   * @param resName the name of the resource to load.
   * @param classLoader the class loader to use to load the request resource.
   * @return an array of bytes, or nll if the resource could not be found.
   */
  public byte[] getResourceAsBytes(final String resName, final ClassLoader classLoader)
  {
    ClassLoader cl = classLoader;
    try
    {
      if (cl == null) cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) cl = getClass().getClassLoader();
      InputStream is = cl.getResourceAsStream(resName);
      if ((is == null) && JPPFConfiguration.getProperties().getBoolean("jppf.classloader.lookup.file", true))
      {
        File file = new File(resName);
        if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
      }
      if (is != null)
      {
        if (debugEnabled) log.debug("resource [" + resName + "] found");
        return StreamUtils.getInputStreamAsByte(is);
      }
    }
    catch (Exception e)
    {
      log.error(e.getMessage(), e);
    }
    if (debugEnabled) log.debug("resource [" + resName + "] not found");
    return null;
  }

  /**
   * Get a resource as an array of byte using a call to <b>ClassLoader#getResource()</b>.
   * This method simply calls {@link #getResource(java.lang.String, java.lang.ClassLoader) getResource(String, ClassLoader)}
   * with a null class loader.
   * @param resName the name of the resource to find.
   * @return the content of the resource as an array of bytes.
   */
  public byte[] getResource(final String resName)
  {
    return getResource(resName, null);
  }

  /**
   * Get a resource as an array of byte using a call to <b>ClassLoader#getResource()</b>.
   * @param resName the name of the resource to find.
   * @param classLoader the class loader to use to load the request resource.
   * @return the content of the resource as an array of bytes.
   */
  public byte[] getResource(final String resName, final ClassLoader classLoader)
  {
    ClassLoader cl = classLoader;
    if (cl == null) cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = getClass().getClassLoader();
    InputStream is = null;
    try
    {
      URL url = cl.getResource(resName);
      if (url != null) is = url.openStream();
      if ((is == null) && JPPFConfiguration.getProperties().getBoolean("jppf.classloader.lookup.file", true))
      {
        File file = new File(resName);
        if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
      }
      if (is != null)
      {
        if (debugEnabled) log.debug("resource [" + resName + "] found");
        return StreamUtils.getInputStreamAsByte(is);
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }

    if (debugEnabled) log.debug("resource [" + resName + "] not found");
    return null;
  }

  /**
   * Compute a callable sent through the JPPF class loader.
   * @param serializedCallable the callable to execute in serialized form.
   * @return the serialized result of the callable's execution, or of an eventually resulting exception.
   */
  public byte[] computeCallable(final byte[] serializedCallable)
  {
    JPPFCallable callable = null;
    ObjectSerializer ser = new ObjectSerializerImpl();
    Object result = null;
    try
    {
      callable = (JPPFCallable) ser.deserialize(serializedCallable);
      result = callable.call();
    }
    catch(Exception e)
    {
      result = e;
    }
    byte[] bytes = null;
    try
    {
      bytes = ser.serialize(result).getBuffer();
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
      try
      {
        bytes = ser.serialize(e).getBuffer();
      }
      catch(Exception e2)
      {
        log.error(e2.getMessage(), e2);
      }
    }
    return bytes;
  }

  /**
   * Get all resources associated with the specified resource name.
   * @param name the name of the resources to look for.
   * @param classLoader the class loader used to load the resources.
   * @return the content of all found resources as a list of byte arrays.
   */
  public List<byte[]> getMultipleResourcesAsBytes(final String name, final ClassLoader classLoader)
  {
    ClassLoader cl = classLoader;
    List<byte[]> result = null;
    if (cl == null) cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) cl = this.getClass().getClassLoader();
    try
    {
      Enumeration<URL> urlEnum = cl.getResources(name);
      if (urlEnum.hasMoreElements())
      {
        result = new ArrayList<byte[]>();
        while (urlEnum.hasMoreElements())
        {
          URL url = urlEnum.nextElement();
          InputStream is = url.openStream();
          byte[] b = StreamUtils.getInputStreamAsByte(is);
          result.add(b);
        }
      }
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    if (JPPFConfiguration.getProperties().getBoolean("jppf.classloader.lookup.file", true))
    {
      try
      {
        File file = new File(name);
        if (file.exists())
        {
          if (result == null) result = new ArrayList<byte[]>();
          result.add(FileUtils.getFileAsByte(file));
        }
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
    return result;
  }

  /**
   * Get all resources associated with each specified resource name.
   * @param cl the class loader used to load the resources.
   * @param names the names of all the resources to look for.
   * @return A mapping of each resource names with a list of the byte content of corresponding resources in the classpath.
   */
  public Map<String, List<byte[]>> getMultipleResourcesAsBytes(final ClassLoader cl, final String...names)
  {
    Map<String, List<byte[]>> result = new HashMap<String, List<byte[]>>();
    for (String name: names)
    {
      List<byte[]> resources = getMultipleResourcesAsBytes(name, cl);
      if (resources != null) result.put(name, resources);
    }
    return result;
  }
}
