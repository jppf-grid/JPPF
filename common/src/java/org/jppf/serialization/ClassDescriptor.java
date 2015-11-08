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
import java.lang.reflect.*;
import java.util.*;

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
   * Indicates the class is a primitive type.
   */
  static final byte PRIMITIVE = 1;
  /**
   * Indicates the class is an array type.
   */
  static final byte ARRAY = 2;
  /**
   * Indicates the class is an enum type.
   */
  static final byte ENUM_TYPE = 4;
  /**
   * Indicates the class has the readObject() and writeObject() methods.
   */
  static final byte HAS_WRITE_OBJECT = 8;
  /**
   * Indicates the class implements the Externalizable interface.
   */
  static final byte EXTERNALIZABLE = 16;
  /**
   * The fields of the described class.
   */
  FieldDescriptor[] fields = NO_FIELDS;
  /**
   * Bitwise flags associated with the described class.
   */
  byte flags = 0;
  /**
   * Is the described class externalizable.
   */
  boolean hasReadWriteObject = false;
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
  boolean externalizable = false;
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
   * Handle for this class descriptor.
   */
  int handle = 0;
  /**
   * Descriptor for this the super class.
   */
  ClassDescriptor superClass;
  /**
   * Component type if this class is an array.
   */
  ClassDescriptor componentType;

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
    this.clazz = clazz;
    primitive = clazz.isPrimitive();
    enumType = clazz.isEnum();
    if (!primitive && !enumType) {
      externalizable = Externalizable.class.isAssignableFrom(clazz);
      Method m = serializing ? SerializationReflectionHelper.getWriteObjectMethod(clazz) : SerializationReflectionHelper.getReadObjectMethod(clazz);
      hasReadWriteObject = m != null;
      if (hasReadWriteObject) {
        m.setAccessible(true);
        if (serializing) writeObjectMethod = m;
        else readObjectMethod = m;
      }
      array = clazz.isArray();
      if (!array) {
        Field[] refFields = SerializationReflectionHelper.getNonTransientDeclaredFields(clazz);
        if (refFields.length > 0) {
          Arrays.sort(refFields, new Comparator<Field>() {
            @Override
            public int compare(final Field o1, final Field o2) {
              return o1.getName().compareTo(o2.getName());
            }
          });
          fields = new FieldDescriptor[refFields.length];
          for (int i=0; i<refFields.length; i++) {
            refFields[i].setAccessible(true);
            fields[i] = new FieldDescriptor(refFields[i]);
          }
        }
      }
    }
    if (signature == null) signature = SerializationReflectionHelper.getSignatureFromType(clazz).intern();
  }

  /**
   * Write this class descriptor to an object output stream.
   * @param serializer the stream to write to.
   * @throws Exception if any error occurs.
   */
  void write(final Serializer serializer) throws Exception {
    serializer.writeClassHandle(handle);
    serializer.writeString(signature);
  }

  /**
   * Read this class descriptor from an input stream.
   * @param deserializer the stream to read from.
   * @return this class descriptor.
   * @throws Exception if any error occurs.
   */
  ClassDescriptor read(final Deserializer deserializer) throws Exception {
    handle = deserializer.readClassHandle();
    signature = deserializer.readString();
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("handle=").append(handle).append(", ");
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
