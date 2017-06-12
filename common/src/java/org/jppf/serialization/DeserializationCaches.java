/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

/**
 * Instances of this class handle the caching and lookup of class descriptors and objects during deserialization.
 * @author Laurent Cohen
 * @exclude
 */
class DeserializationCaches {
  /**
   * Mapping of handles to corresponding classes.
   */
  Map<String, Class<?>> handleToClassMap = new HashMap<>();
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
      classToDescMap.put(cd.clazz, cd);
    }
  }

  /**
   * Get the class descriptor associated with the specified handle.
   * @param handle the handle to lookup.
   * @param cl .
   * @return a {@link ClassDescriptor} instance.
   * @throws Exception if any error occurs.
   */
  ClassDescriptor getDescriptor(final String handle, final ClassLoader cl) throws Exception {
    Class<?> c = getClassFromHandle(handle, cl);
    ClassDescriptor cd = classToDescMap.get(c);
    if (cd == null) {
      cd = createDescriptor(c, handle);
    }
    return cd;
  }

  /**
   * Create a class descriptor from the specified class.
   * @param clazz the class from which to create the descriptor.
   * @param signature the class signature if already computed, or {@code null} if not.
   * @return the newly created class descriptor.
   * @throws Exception if any error occurs.
   */
  ClassDescriptor createDescriptor(final Class<?> clazz, final String signature) throws Exception {
    ClassDescriptor cd = new ClassDescriptor();
    cd.signature = (signature == null) ? SerializationReflectionHelper.getSignatureFromType(clazz) : signature;
    cd.fillIn(clazz, false);
    setupClassDescriptors(cd, clazz.getClassLoader());
    return cd;
  }

  /**
   * Get the class associated with the specified handle.
   * @param handle the handle to lookup.
   * @param cl the class loader with which to load the class.
   * @return a {@link ClassDescriptor} instance.
   * @throws Exception if any error occurs.
   */
  Class<?> getClassFromHandle(final String handle, final ClassLoader cl) throws Exception {
    Class<?> c = handleToClassMap.get(handle);
    if (c == null) {
      c = SerializationReflectionHelper.getTypeFromSignature(handle, cl);
      handleToClassMap.put(handle, c);
    }
    return c;
  }

  /**
   * Explore the class represented by the specified class descritpor and recursively initialize the class descriptors
   * for the super classes, array compoent types and fields.
   * @param cd the class descritpor to explore.
   * @param cl the class loader with which to load the classes.
   * @throws Exception if any error occurs.
   */
  private void setupClassDescriptors(final ClassDescriptor cd, final ClassLoader cl) throws Exception {
    if (!cd.populated) cd.fillIn(getClassFromHandle(cd.signature, cl), false);
    if (!classToDescMap.containsKey(cd.clazz)) {
      classToDescMap.put(cd.clazz, cd);
      if (cd.array) {
        Class<?> clazz = cd.clazz.getComponentType();
        if (cd.componentType == null) cd.componentType = descriptorFromClass(clazz);
        if ((cd.componentType != null) && (cd.componentType.clazz == null)) setupClassDescriptors(cd.componentType, cl);
      }
      if (cd.superClass == null) {
        Class<?> clazz = cd.clazz.getSuperclass();
        if ((clazz != null) && (clazz != Object.class)) cd.superClass = descriptorFromClass(clazz);
      }
      if ((cd.superClass != null) && (cd.superClass.clazz == null)) setupClassDescriptors(cd.superClass, cl);
      if (cd.fields != null) {
        for (FieldDescriptor fd: cd.fields) {
          if (fd.type == null) fd.type = descriptorFromClass(fd.field.getType());
          if (fd.type.clazz == null) setupClassDescriptors(fd.type, cl);
        }
      }
    }
  }

  /**
   * Retrieve or create a class descriptor for the psecified class.
   * @param clazz the class from which to obtain a class descriptor.
   * @return a {@link ClassDescriptor} instance.
   * @throws Exception if any error occurs.
   */
  private ClassDescriptor descriptorFromClass(final Class<?> clazz) throws Exception {
    ClassDescriptor cd = classToDescMap.get(clazz);
    String sig = SerializationReflectionHelper.getSignatureFromType(clazz);
    if (cd == null) cd = createDescriptor(clazz, sig);
    return cd;
  }
}
