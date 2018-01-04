/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.io.Externalizable;
import java.lang.reflect.Method;

/**
 * Instances of this class describe a Java class with all its non-transient fields
 * and characteristics relating to serialization.
 * @author Laurent Cohen
 * @exclude
 */
class ClassDescriptor {
  /**
   * Constant value for classes with no fields.
   */
  static final FieldDescriptor[] NO_FIELDS = new FieldDescriptor[0];
  /**
   * The fields of the described class.
   */
  FieldDescriptor[] fields = NO_FIELDS;
  /**
   * Is the described class externalizable.
   */
  boolean hasReadWriteObject;
  /**
   * Is the described class externalizable.
   */
  Method writeObjectMethod;
  /**
   * Is the described class externalizable.
   */
  Method readObjectMethod;
  /**
   * Is the described class externalizable.
   */
  boolean externalizable;
  /**
   * Is the class an array type.
   */
  boolean array;
  /**
   * Is the class a primitive type.
   */
  boolean primitive;
  /**
   * Is the class an enum.
   */
  boolean enumType;
  /**
   * Unique signature for the class.
   */
  String signature;
  /**
   * The externalizable class.
   */
  Class<?> clazz;
  /**
   * Descriptor for this the super class.
   */
  ClassDescriptor superClass;
  /**
   * Component type if this class is an array.
   */
  ClassDescriptor componentType;
  /**
   * 
   */
  boolean populated = false;

  /**
   * Initialize an empty class descriptor.
   */
  ClassDescriptor() {
  }

  /**
   * Initialize a class descriptor from the specified class.
   * @param clazz the class from which to initialize.
   * @throws Exception if any error occurs.
   */
  ClassDescriptor(final Class<?> clazz) throws Exception {
    fillIn(clazz, true);
  }

  /**
   * Initialize a class descriptor from the specified class.
   * @param clazz the class from which to initialize.
   * @param serializing whether we are serializing or deserializing.
   * @throws Exception if any error occurs.
   */
  void fillIn(final Class<?> clazz, final boolean serializing) throws Exception {
    populated = true;
    this.clazz = clazz;
    primitive = clazz.isPrimitive();
    enumType = clazz.isEnum();
    if (!primitive && !enumType) {
      externalizable = Externalizable.class.isAssignableFrom(clazz);
      //hasReadWriteObject = handleReadOrWriteObjectMethod(true) && handleReadOrWriteObjectMethod(false);
      hasReadWriteObject = handleReadOrWriteObjectMethod(serializing);
      array = clazz.isArray();
      if (!array) fields = SerializationReflectionHelper.getPersistentDeclaredFields(clazz);
    }
    if (signature == null) signature = SerializationReflectionHelper.getSignatureFromType(clazz);
  }

  /**
   * Retrieve the {@code writeObject()} or {@code readObject()} method. 
   * @param serializing if {@code true} then retireve the {@code writeObject()} method, otherwise retrieve the {@code readObject()} method.
   * @return {@code true} if the method exists for this class, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  boolean handleReadOrWriteObjectMethod(final boolean serializing) throws Exception {
    final Method m = SerializationReflectionHelper.getReadOrWriteObjectMethod(clazz, !serializing);
    final boolean found = (m != null);
    if (found) {
      m.setAccessible(true);
      if (serializing) writeObjectMethod = m;
      else readObjectMethod = m;
    }
    return found;
  }

  /**
   * Write this class descriptor to an object output stream.
   * @param serializer the stream to write to.
   * @throws Exception if any error occurs.
   */
  void write(final Serializer serializer) throws Exception {
    serializer.writeString(signature);
  }

  /**
   * Read this class descriptor from an input stream.
   * @param deserializer the stream to read from.
   * @return this class descriptor.
   * @throws Exception if any error occurs.
   */
  ClassDescriptor read(final Deserializer deserializer) throws Exception {
    signature = deserializer.readString();
    return this;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("signature=").append(signature).append(", ");
    sb.append("clazz=").append(clazz == null ? "null" : clazz.getName()).append(", ");
    sb.append("primitive=").append(primitive).append(", ");
    sb.append("hasWriteObject=").append(hasReadWriteObject).append(", ");
    sb.append("externalizable=").append(externalizable).append(", ");
    sb.append("array=").append(array).append(", ");
    if (superClass != null) sb.append("superClass=").append(superClass.signature).append(", ");
    if (componentType != null) sb.append("componentType=").append(componentType.signature).append(", ");
    sb.append("fields={");
    if ((fields != null) && (fields.length > 0)) {
      for (int i=0; i<fields.length; i++) sb.append("\n  ").append(fields[i]);
      sb.append('\n');
    }
    sb.append('}');
    sb.append(']');
    return sb.toString();
  }
}
