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

import java.util.*;

import org.slf4j.*;

/**
 * Instances of this class handle the caching and lookup of class descriptors and objects during deserialization.
 * @author Laurent Cohen
 * @exclude
 */
class DeserializationCaches implements Cleanable {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DeserializationCaches.class);
  /**
   * Mapping of handles to corresponding class descriptors.
   */
  Map<Integer, ClassDescriptor> handleToDescriptorMap = new HashMap<>();
  /**
   * Mapping of classes to their descriptor.
   */
  Map<Class<?>, ClassDescriptor> classToDescMap = new HashMap();
  /**
   * Mapping of handles to corresponding objects.
   */
  Map<Integer, Object> handleToObjectMap = new HashMap<>();

  /**
   * Default constructor.
   */
  DeserializationCaches() {
    for (Map.Entry<Class<?>, ClassDescriptor> entry: SerializationCaches.globalTypesMap.entrySet()) {
      ClassDescriptor cd = entry.getValue();
      handleToDescriptorMap.put(cd.handle, cd);
      classToDescMap.put(cd.clazz, cd);
    }
  }

  /**
   * Get the class descriptor associated with the specified handle.
   * @param handle the handle to lookup.
   * @return a {@link ClassDescriptor} instance.
   */
  ClassDescriptor getDescriptor(final int handle) {
    return handleToDescriptorMap.get(handle);
  }

  /**
   * 
   * @param cdMap .
   * @param cl .
   * @throws Exception if any error occurs.
   */
  void setupHandles(final Map<String, ClassDescriptor> cdMap, final ClassLoader cl) throws Exception {
    for (Map.Entry<String, ClassDescriptor> entry: cdMap.entrySet()) {
      ClassDescriptor cd = entry.getValue();
      handleToDescriptorMap.put(cd.handle, cd);
    }
    for (Map.Entry<String, ClassDescriptor> entry: cdMap.entrySet()) setupHandles(entry.getValue(), cdMap, cl);
  }

  /**
   * 
   * @param initCD .
   * @param map .
   * @param cl .
   * @throws Exception if any error occurs.
   */
  private void setupHandles(final ClassDescriptor initCD, final Map<String, ClassDescriptor> map, final ClassLoader cl) throws Exception {
    //if (cd.handle <= 0) log.info("no handle for cd={}", cd);
    ClassDescriptor cd = initCD;
    if (cd.clazz == null) {
      cd.fillIn(SerializationReflectionHelper.getTypeFromSignature(cd.signature, cl), false);
    }
    if (!classToDescMap.containsKey(cd.clazz)) {
      classToDescMap.put(cd.clazz, cd);
      if (cd.array) {
        Class<?> clazz = cd.clazz.getComponentType();
        if (cd.componentType == null) cd.componentType = descriptorFromClass(clazz, map);
        if ((cd.componentType != null) && (cd.componentType.clazz == null)) setupHandles(cd.componentType, map, cl);
      }
      if (cd.superClass == null) {
        Class<?> clazz = cd.clazz.getSuperclass();
        if ((clazz != null) && (clazz != Object.class)) cd.superClass = descriptorFromClass(clazz, map);
      }
      if ((cd.superClass != null) && (cd.superClass.clazz == null)) setupHandles(cd.superClass, map, cl);
      if (cd.fields != null) {
        for (FieldDescriptor fd: cd.fields) {
          if (fd.type == null) fd.type = descriptorFromClass(fd.field.getType(), map);
          if (fd.type.clazz == null) setupHandles(fd.type, map, cl);
        }
      }
    }
  }

  /**
   * 
   * @param clazz .
   * @param map .
   * @return .
   * @throws Exception if any error occurs.
   */
  private ClassDescriptor descriptorFromClass(final Class<?> clazz, final Map<String, ClassDescriptor> map) throws Exception {
    ClassDescriptor cd = classToDescMap.get(clazz);
    if (cd == null) cd = map.get(SerializationReflectionHelper.getSignatureFromType(clazz));
    return cd;
  }

  /**
   * 
   */
  private class Remapping {
    /**
     * 
     */
    public ClassDescriptor cd, cd2;

    /**
     * 
     * @param cd .
     * @param cd2 .
     */
    public Remapping(final ClassDescriptor cd, final ClassDescriptor cd2) {
      this.cd = cd;
      this.cd2 = cd2;
    }
  }

  @Override
  public void setup() {
  }

  @Override
  public void cleanup() {
  }
}
