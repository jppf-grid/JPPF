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

package org.jppf.serialization;

import java.io.*;
import java.lang.ref.SoftReference;

import org.jppf.utils.pooling.*;

/**
 * This implementation uses the JPPF serialization.
 */
public class DefaultJPPFSerialization implements JPPFSerialization {
  /**
   * 
   */
  private SoftReference<ClassLoader> ref = new SoftReference<>(null);
  /**
   * A fast dynamic pool of {@link Serializer} instances.
   */
  private ObjectPool<Serializer> serializerPool = new SerializerPool();
  /**
   * A fast dynamic pool of {@link Deserializer} instances.
   */
  private ObjectPool<Deserializer> deserializerPool =  new DeserializerPool();

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    new JPPFObjectOutputStream(os).writeObject(o);
    /*
    synchronized(this) {
      ClassLoader refCl = ref.get();
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if ((refCl == null) || (cl != refCl)) {
        ref = new SoftReference<>(cl);
        serializerPool = new SerializerPool();
      }
    }
    Serializer serializer = null;
    try {
      serializer = serializerPool.get();
      new JPPFObjectOutputStream(os, serializer).writeObject(o);
    } finally {
      if (serializer != null) serializerPool.put(serializer);
    }
    */
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    return new JPPFObjectInputStream(is).readObject();
    /*
    synchronized(this) {
      ClassLoader refCl = ref.get();
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if ((refCl == null) || (cl != refCl)) {
        ref = new SoftReference<>(cl);
        deserializerPool = new DeserializerPool();
      }
    }
    Deserializer deserializer = null;
    try {
      deserializer = deserializerPool.get();
      return new JPPFObjectInputStream(is, deserializer).readObject();
    } finally {
      if (deserializer != null) deserializerPool.put(deserializer);
    }
    */
  }

  /**
   * A fast dynamic pool of {@link Serializer} instances.
   */
  private static class SerializerPool extends AbstractObjectPoolQueue<Serializer> {
    @Override
    protected Serializer create() {
      Serializer serializer = new Serializer(null);
      return serializer;
    }

    @Override
    public void put(final Serializer serializer) {
      serializer.caches.objectHandleMap.clear();
      super.put(serializer);
    }
  };

  /**
   * A fast dynamic pool of {@link Deserializer} instances.
   */
  private static class DeserializerPool extends AbstractObjectPoolQueue<Deserializer> {
    @Override
    protected Deserializer create() {
      Deserializer deserializer = new Deserializer(null);
      return deserializer;
    }

    @Override
    public void put(final Deserializer deserializer) {
      deserializer.caches.handleToObjectMap.clear();
      super.put(deserializer);
    }
  };
}
