/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import org.jppf.JPPFException;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * Instances of this class are dedicated to reading resource files form the JVM's classpath and converting them into
 * arrays of bytes.
 * @author Laurent Cohen
 * @author Domingos Creado
 * @since 5.0
 * @exclude
 */
public abstract class AbstractResourceProvider implements ResourceProvider {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractResourceProvider.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Default constructor.
   */
  public AbstractResourceProvider() {
  }

  @Override
  public byte[] getResource(final String resName, final boolean lookupInFileSystem) {
    return getResource(resName, (ClassLoader) null, lookupInFileSystem);
  }

  @Override
  public byte[] getResource(final String resName, final ClassLoader classloader, final boolean lookupInFileSystem) {
    ClassLoader cl = resolveClassLoader(classloader);
    InputStream is = null;
    try {
      Enumeration<URL> urls = cl.getResources(resName);
      if ((urls != null) && urls.hasMoreElements()) {
        while (urls.hasMoreElements() && (is == null)) {
          URL url = urls.nextElement();
          if (url != null) is = url.openStream();
        }
      } else {
        is = cl.getResourceAsStream(resName);
      }
      if ((is == null) && lookupInFileSystem) {
        File file = new File(resName);
        if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
      }
      if (is != null) {
        if (debugEnabled) log.debug("resource [" + resName + "] found");
        return StreamUtils.getInputStreamAsByte(is);
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    if (debugEnabled) log.debug("resource [{}] not found for class laoder {}", resName, classloader);
    return null;
  }

  @Override
  public byte[] getResource(final String resName, final Collection<ClassLoader> classLoaders, final boolean lookupInFileSystem) {
    if ((classLoaders == null) || classLoaders.isEmpty()) return getResource(resName, (ClassLoader) null, lookupInFileSystem);
    byte[] result = null;
    for (ClassLoader cl: classLoaders) {
      result = getResource(resName, cl, lookupInFileSystem);
      if (result != null) break;
    }
    return result;
  }

  @Override
  public byte[] computeCallable(final byte[] serializedCallable) {
    if (debugEnabled) log.debug("before deserialization");
    JPPFCallable callable = null;
    ObjectSerializer ser = new ObjectSerializerImpl();
    Object result = null;
    try {
      callable = (JPPFCallable) ser.deserialize(serializedCallable);
      result = callable.call();
    } catch(Throwable t) {
      result = (t instanceof Exception) ? t : new JPPFException(t);
    }
    byte[] bytes = null;
    try {
      bytes = ser.serialize(result).getBuffer();
    } catch(Exception e) {
      log.error(e.getMessage(), e);
      try {
        bytes = ser.serialize(e).getBuffer();
      } catch(Exception e2) {
        log.error(e2.getMessage(), e2);
      }
    }
    return bytes;
  }

  @Override
  public List<byte[]> getMultipleResourcesAsBytes(final String name, final ClassLoader classloader, final boolean lookupInFileSystem) {
    ClassLoader cl = resolveClassLoader(classloader);
    if (debugEnabled) log.debug(String.format("before lookup: name=%s, resolved classloader=%s, lookupInFileSystem=%b", name, cl, lookupInFileSystem));
    List<byte[]> result = null;
    try {
      Enumeration<URL> urlEnum = cl.getResources(name);
      if ((urlEnum != null) && urlEnum.hasMoreElements()) {
        while (urlEnum.hasMoreElements()) {
          URL url = urlEnum.nextElement();
          if (url != null) {
            InputStream is = url.openStream();
            byte[] b = StreamUtils.getInputStreamAsByte(is);
            if (result == null) result = new ArrayList<>();
            result.add(b);
          }
        }
      } else {
        InputStream is = cl.getResourceAsStream(name);
        if (is != null) {
          byte[] b = StreamUtils.getInputStreamAsByte(is);
          if (result == null) result = new ArrayList<>();
          result.add(b);
        }
      }
    }
    catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    if (lookupInFileSystem) {
      try {
        File file = new File(name);
        if (file.exists()) {
          if (result == null) result = new ArrayList<>();
          result.add(FileUtils.getFileAsByte(file));
        }
      }
      catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    if (debugEnabled) log.debug(String.format("after lookup: result for %s is %s", name, result));
    return result;
  }

  @Override
  public List<byte[]> getMultipleResourcesAsBytes(final String name, final Collection<ClassLoader> classLoaders, final boolean lookupInFileSystem) {
    if ((classLoaders == null) || classLoaders.isEmpty()) return getMultipleResourcesAsBytes(name, (ClassLoader) null, lookupInFileSystem);
    List<byte[]> result = new ArrayList<>();
    for (ClassLoader cl: classLoaders) {
      result = getMultipleResourcesAsBytes(name, cl, lookupInFileSystem);
      if ((result != null) && !result.isEmpty()) break;
    }
    return result;
  }

  @Override
  public Map<String, List<byte[]>> getMultipleResourcesAsBytes(final ClassLoader classloader, final boolean lookupInFileSystem, final String...names) {
    ClassLoader cl = resolveClassLoader(classloader);
    Map<String, List<byte[]>> result = new HashMap<>();
    for (String name: names) {
      List<byte[]> resources = getMultipleResourcesAsBytes(name, cl, lookupInFileSystem);
      if (resources != null) result.put(name, resources);
    }
    return result;
  }


  @Override
  public Map<String, List<byte[]>> getMultipleResourcesAsBytes(final Collection<ClassLoader> classLoaders, final boolean lookupInFileSystem, final String...names) {
    Map<String, List<byte[]>> result = new HashMap<>();
    for (String name: names) {
      List<byte[]> resources = getMultipleResourcesAsBytes(name, classLoaders, lookupInFileSystem);
      if (resources != null) result.put(name, resources);
    }
    return result;
  }

  /**
   * Resolve the class loader to use for getting resources from the classpath.
   * @param classloader the initial clss loader.
   * @return the resolved class loader.
   */
  protected abstract ClassLoader resolveClassLoader(ClassLoader classloader);
}
