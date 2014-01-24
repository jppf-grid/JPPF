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

package org.jppf.serialization.kryo;

import java.io.*;
import java.lang.reflect.InvocationHandler;
import java.util.*;

import org.jppf.serialization.*;
import org.jppf.utils.pooling.*;
import org.objenesis.instantiator.ObjectInstantiator;
import org.objenesis.strategy.InstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.*;

import de.javakaffee.kryoserializers.*;

/**
 * This implementation uses the <a href="http://code.google.com/p/kryo/">Kryo</a> serialization.
 */
public class KryoSerialization implements JPPFSerialization {
  /**
   * Strategy used to instantiate objects during desrialization.
   */
  private static InstantiatorStrategy str = new InstantiatorStrategy() {
    @Override
    public ObjectInstantiator newInstantiatorOf(final Class c) {
      return new JPPFInstantiator(c);
    }
  };
  /**
   * A fast dynamic pool of {@link Kryo} instances.
   * Using this provides a huge performance improvement vs creating a new Kryo instance each time we serialize or deserialize an object.
   * @see org.jppf.utils.pooling.AbstractObjectPoolQueue
   */
  private static ObjectPool<Kryo> pool = new AbstractObjectPoolQueue<Kryo>() {
    @Override
    protected Kryo create() {
      Kryo kryo = createKryo();
      return kryo;
    }
  };

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    Kryo kryo = pool.get();
    try {
      Output out = new Output(os);
      kryo.writeClassAndObject(out, o);
      out.flush();
    } finally {
      pool.put(kryo);
    }
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    Kryo kryo = pool.get();
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl != null)  kryo.setClassLoader(cl);
    try {
      Input in = new Input(is);
      return kryo.readClassAndObject(in);
    } finally {
      pool.put(kryo);
    }
  }

  /**
   * Creates objects without invoking any constructor.
   */
  private static class JPPFInstantiator implements ObjectInstantiator {
    /**
     * The class to instantiate.
     */
    private final Class c;

    /**
     * Initialize this instantiator with the specified class.
     * @param c the class to instantiate.
     */
    public JPPFInstantiator(final Class c) {
      this.c = c;
    }

    @Override
    public Object newInstance() {
      try {
        return SerializationReflectionHelper.create(c);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
 
  /**
   * Create a Kryo instance, with its instantiator strategy and a set of
   * common serializers (from kryo-serializers project) initialized.
   * @return an instance of {@link Kryo}.
   */
  private static Kryo createKryo() {
    Kryo kryo = new Kryo();
    kryo.setInstantiatorStrategy(str);

    kryo.register(Arrays.asList( "" ).getClass(), new ArraysAsListSerializer() );
    kryo.register(Collections.EMPTY_LIST.getClass(), new CollectionsEmptyListSerializer() );
    kryo.register(Collections.EMPTY_MAP.getClass(), new CollectionsEmptyMapSerializer() );
    kryo.register(Collections.EMPTY_SET.getClass(), new CollectionsEmptySetSerializer() );
    kryo.register(Collections.singletonList( "" ).getClass(), new CollectionsSingletonListSerializer() );
    kryo.register(Collections.singleton( "" ).getClass(), new CollectionsSingletonSetSerializer() );
    kryo.register(Collections.singletonMap( "", "" ).getClass(), new CollectionsSingletonMapSerializer() );
    kryo.register(GregorianCalendar.class, new GregorianCalendarSerializer());
    kryo.register(InvocationHandler.class, new JdkProxySerializer());
    UnmodifiableCollectionsSerializer.registerSerializers(kryo);
    SynchronizedCollectionsSerializer.registerSerializers(kryo);
    kryo.register(EnumMap.class, new EnumMapSerializer());
    kryo.register(EnumSet.class, new EnumSetSerializer());
    return kryo;
  }
}
