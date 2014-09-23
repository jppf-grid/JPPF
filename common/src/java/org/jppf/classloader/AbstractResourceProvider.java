/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
  public byte[] getResource(final String resName) {
    return getResource(resName, null);
  }

  @Override
  public byte[] getResource(final String resName, final ClassLoader classloader) {
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
      if ((is == null) && JPPFConfiguration.getProperties().getBoolean("jppf.classloader.lookup.file", true)) {
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

    if (debugEnabled) log.debug("resource [" + resName + "] not found");
    return null;
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
  public List<byte[]> getMultipleResourcesAsBytes(final String name, final ClassLoader classloader) {
    ClassLoader cl = resolveClassLoader(classloader);
    List<byte[]> result = null;
    try {
      Enumeration<URL> urlEnum = cl.getResources(name);
      if ((urlEnum != null) && urlEnum.hasMoreElements()) {
        while (urlEnum.hasMoreElements()) {
          URL url = urlEnum.nextElement();
          if ((urlEnum != null) && urlEnum.hasMoreElements()) {
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
    if (JPPFConfiguration.getProperties().getBoolean("jppf.classloader.lookup.file", true)) {
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
    return result;
  }

  @Override
  public Map<String, List<byte[]>> getMultipleResourcesAsBytes(final ClassLoader classloader, final String...names) {
    ClassLoader cl = resolveClassLoader(classloader);
    Map<String, List<byte[]>> result = new HashMap<>();
    for (String name: names) {
      List<byte[]> resources = getMultipleResourcesAsBytes(name, cl);
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
