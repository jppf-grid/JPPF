/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationHandler;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.GregorianCalendar;

import javax.management.ObjectName;

import org.jppf.management.OffloadableNotification;
import org.jppf.management.TaskExecutionNotification;
import org.jppf.node.protocol.graph.TaskGraph;
import org.jppf.serialization.JPPFSerialization;
import org.jppf.utils.pooling.AbstractObjectPoolQueue;
import org.jppf.utils.pooling.ObjectPool;
import org.objenesis.strategy.StdInstantiatorStrategy;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Kryo.DefaultInstantiatorStrategy;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.CollectionSerializer;
import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.MapReferenceResolver;

import de.javakaffee.kryoserializers.ArraysAsListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyListSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptyMapSerializer;
import de.javakaffee.kryoserializers.CollectionsEmptySetSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonListSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonMapSerializer;
import de.javakaffee.kryoserializers.CollectionsSingletonSetSerializer;
import de.javakaffee.kryoserializers.EnumMapSerializer;
import de.javakaffee.kryoserializers.EnumSetSerializer;
import de.javakaffee.kryoserializers.GregorianCalendarSerializer;
import de.javakaffee.kryoserializers.JdkProxySerializer;
import de.javakaffee.kryoserializers.SynchronizedCollectionsSerializer;
import de.javakaffee.kryoserializers.UnmodifiableCollectionsSerializer;

/**
 * This implementation uses the <a href="http://code.google.com/p/kryo/">Kryo</a> serialization.
 */
public class KryoSerialization implements JPPFSerialization {
  /**
   * A fast dynamic pool of {@link Kryo} instances.
   * Using this provides a huge performance improvement vs creating a new Kryo instance each time we serialize or deserialize an object.
   */
  private static ObjectPool<Kryo> pool = new AbstractObjectPoolQueue<Kryo>() {
    @Override
    protected Kryo create() {
      final Kryo kryo = createKryo();
      return kryo;
    }
  };

  @Override
  public void serialize(final Object o, final OutputStream os) throws Exception {
    final Kryo kryo = pool.get();
    try {
      final Output out = new Output(os);
      kryo.writeClassAndObject(out, o);
      out.flush();
    } finally {
      pool.put(kryo);
    }
  }

  @Override
  public Object deserialize(final InputStream is) throws Exception {
    final Kryo kryo = pool.get();
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl != null)  kryo.setClassLoader(cl);
    try {
      final Input in = new Input(is);
      return kryo.readClassAndObject(in);
    } finally {
      pool.put(kryo);
    }
  }

  /**
   * Forces the clearing of the class name to class map upon invocation of the {@code reset()} method.
   */
  public static class CustomClassResolver extends DefaultClassResolver {
    @Override
    public void reset() {
      super.reset();
      if (nameToClass != null) nameToClass.clear();
    }
  }

  /**
   * Create a Kryo instance, with its instantiator strategy and a set of
   * common serializers (from kryo-serializers project) initialized.
   * @return an instance of {@link Kryo}.
   */
  private static Kryo createKryo() {
    final Kryo kryo = new Kryo(new CustomClassResolver(), new MapReferenceResolver());
    kryo.setAutoReset(true);
    kryo.setInstantiatorStrategy(new DefaultInstantiatorStrategy(new StdInstantiatorStrategy()));

    kryo.register(ObjectName.class, new ObjectNameSerializer());
    kryo.register(OffloadableNotification.class, new GenericObjectSerializer());
    kryo.register(TaskExecutionNotification.class, new GenericObjectSerializer());
    kryo.register(TaskGraph.class, new JobTaskGraphSerializer());
    kryo.register(Collection.class, new CollectionSerializer());
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
