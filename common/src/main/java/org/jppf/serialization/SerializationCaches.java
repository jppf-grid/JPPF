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

package org.jppf.serialization;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.*;

/**
 * Instances of this class handle the caching and lookup of class descriptors and objects during serialization.
 * @author Laurent Cohen
 * @exclude
 */
class SerializationCaches {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(SerializationCaches.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * List of all primitive types.
   */
  static final Class<?>[] PRIMITIVE_TYPES = { Byte.TYPE, Short.TYPE, Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Character.TYPE, Boolean.TYPE/*, Void.TYPE*/ };
  /**
   * List of all wrapper types.
   */
  static final Class<?>[] WRAPPER_TYPES = { Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Character.class, Boolean.class/*, Void.class*/ };
  /**
   * Mapping of primitive types to their descriptor.
   */
  final static Map<Class<?>, ClassDescriptor> globalTypesMap = initGlobalTypes();
  /**
   * Mapping of classes to their descriptor.
   */
  final Map<Class<?>, ClassDescriptor> classToDescMap = new HashMap<>();
  /**
   * Mapping of objects to their handle.
   */
  final Map<Object, Integer> objectHandleMap = new IdentityHashMap<>(256);
  /**
   * Counter for the class handles.
   */
  private final AtomicInteger classHandleCount = new AtomicInteger(0);
  /**
   * Counter for the object handles.
   */
  private final AtomicInteger objectHandleCount = new AtomicInteger(0);

  /**
   * Initialize the descriptors for all primitive types.
   * @return a mapping of primitive types to their class descriptor.
   */
  private static Map<Class<?>, ClassDescriptor> initGlobalTypes() {
    final Map<Class<?>, ClassDescriptor> map = createClassKeyMap();
    final AtomicInteger counter = new AtomicInteger(0);
    try {
      for (final Class<?> c: PRIMITIVE_TYPES) getClassDescriptorGeneric(c, counter, map, null);
      //for (Class<?> c: WRAPPER_TYPES) getClassDescriptorGeneric(c, counter, map, null);
      getClassDescriptorGeneric(Object.class, counter, map, null);
      getClassDescriptorGeneric(String.class, counter, map, null);
    } catch (final Exception e) {
      log.error("error initializing global types", e);
    }
    return map;
  }

  /**
   * Create a Map where the keys are {@link Class} objects.
   * @param <V> the type of the map's values.
   * @return a new {@link Map}.
   */
  static <V> Map<Class<?>, V> createClassKeyMap() {
    return new HashMap<>();
  }

  /**
   * Default constructor.
   */
  SerializationCaches() {
    classToDescMap.putAll(globalTypesMap);
    classHandleCount.set(classToDescMap.size());
  }

  /**
   * Get the descriptor for the specified class, and created it if needed.
   * @param clazz the class for which to get a descriptor.
   * @param map a temporary association map.
   * @return a {@link ClassDescriptor} object.
   * @throws Exception if nay error occurs.
   */
  ClassDescriptor getClassDescriptor(final Class<?> clazz, final Map<Class<?>, ClassDescriptor> map) throws Exception {
    return getClassDescriptorGeneric(clazz, classHandleCount, classToDescMap, map);
  }

  /**
   * Create a new handle for the specified object, and the new entry in the map.
   * @param o the object for which to get a handle.
   * @return the handle as an int value.
   */
  int newObjectHandle(final Object o) {
    final int handle = objectHandleCount.incrementAndGet();
    objectHandleMap.put(o, handle);
    if (traceEnabled) try { log.trace("created handle " + handle  + " for o=" + o); } catch(@SuppressWarnings("unused") final Exception e) {}
    return handle;
  }

  /**
   * Get the descriptor for the specified class, and create it if needed.
   * @param clazz the class for which to get a descriptor.
   * @param counter the handle as an auto-incrementing counter.
   * @param map the map that contains the handle to class descriptor associations.
   * @param map2 a temporary association map.
   * @return a {@link ClassDescriptor} object.
   * @throws Exception if nay error occurs.
   */
  static ClassDescriptor getClassDescriptorGeneric(final Class<?> clazz, final AtomicInteger counter,
      final Map<Class<?>, ClassDescriptor> map, final Map<Class<?>, ClassDescriptor> map2) throws Exception {
    ClassDescriptor cd = map.get(clazz);
    if (cd == null) cd = addClassGeneric(clazz, counter, map, map2);
    return cd;
  }

  /**
   * Add a class mapping.
   * @param clazz the class to map to a descriptor.
   * @param counter the handle as an auto-incrementing counter.
   * @param map the map that contains the handle to class descriptor associations.
   * @param map2 a temporary association map.
   * @return the {@link ClassDescriptor} object that was created.
   * @throws Exception if any error occurs.
   */
  static ClassDescriptor addClassGeneric(final Class<?> clazz, final AtomicInteger counter,
      final Map<Class<?>, ClassDescriptor> map, final Map<Class<?>, ClassDescriptor> map2) throws Exception {
    final ClassDescriptor cd = new ClassDescriptor(clazz);
    //if (traceEnabled) try { log.trace("created " + cd); } catch(Exception e) {}
    map.put(clazz, cd);
    if (map2 != null) map2.put(clazz, cd);
    for (final FieldDescriptor fd: cd.fields) fd.type = getClassDescriptorGeneric(fd.field.getType(), counter, map, map2);
    final Class<?> tmpClazz = clazz.getSuperclass();
    if ((tmpClazz != null) && (tmpClazz != Object.class)) cd.superClass = getClassDescriptorGeneric(tmpClazz, counter, map, map2);
    if (clazz.isArray()) cd.componentType = getClassDescriptorGeneric(clazz.getComponentType(), counter, map, map2);
    return cd;
  }
}
