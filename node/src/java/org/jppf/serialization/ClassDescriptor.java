/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

/**
 * Instances of this class describe a Java class with all its non-transient fields
 * and characteristics relating to serialization.
 * @author Laurent Cohen
 * @exclude
 */
class ClassDescriptor
{
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
   * Is the described class externalizable.
   */
  boolean hasWriteObject = false;
  /**
   * Bitwise flags associated with the described class.
   */
  byte flags = 0;
  /**
   * Is the described class externalizable.
   */
  Method writeObjectMethod;
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
   * Handle for the superclass, used for deserialization only.
   */
  int superClassHandle;
  /**
   * Component type if this class is an array.
   */
  ClassDescriptor componentType;
  /**
   * Handle for the component type, used for deserialization only.
   */
  int componentTypeHandle;

  /**
   * Initialize an empty class descriptor.
   */
  ClassDescriptor()
  {
  }

  /**
   * Initialize a class descriptor from the specified class.
   * @param clazz the class from which to initialize.
   * @throws Exception if any error occurs.
   */
  ClassDescriptor(final Class<?> clazz) throws Exception
  {
    this.clazz = clazz;
    primitive = clazz.isPrimitive();
    enumType = clazz.isEnum();
    if (!primitive && !enumType)
    {
      externalizable = Externalizable.class.isAssignableFrom(clazz);
      writeObjectMethod = SerializationReflectionHelper.getWriteObjectMethod(clazz);
      hasWriteObject = writeObjectMethod != null;
      array = clazz.isArray();
      if (!array)
      {
        Field[] refFields = SerializationReflectionHelper.getNonTransientFields(clazz);
        if (refFields.length > 0)
        {
          fields = new FieldDescriptor[refFields.length];
          for (int i=0; i<refFields.length; i++) fields[i] = new FieldDescriptor(refFields[i]);
        }
      }
    }
    signature = SerializationReflectionHelper.getSignatureFromType(clazz).intern();
  }

  /**
   * Write this class descriptor to an object output stream.
   * @param out the stream to write to.
   * @throws IOException if any error occurs.
   */
  void write(final ObjectOutputStream out) throws IOException
  {
    out.writeInt(handle);
    out.writeUTF(signature);
    flags = 0;
    if (primitive) flags |= PRIMITIVE;
    if (enumType) flags |= ENUM_TYPE;
    if (hasWriteObject) flags |= HAS_WRITE_OBJECT;
    if (externalizable) flags |= EXTERNALIZABLE;
    if (array) flags |= ARRAY;
    out.writeByte(flags);
    out.writeInt((superClass != null) ? superClass.handle : 0);
    if (array) out.writeInt(componentType.handle);
    if (!primitive)
    {
      out.writeInt(fields.length);
      for (FieldDescriptor field : fields) field.write(out);
    }
  }

  /**
   * Read this class descriptor from an input stream.
   * @param in the stream to read from.
   * @throws IOException if any error occurs.
   */
  void read(final ObjectInputStream in) throws IOException
  {
    handle = in.readInt();
    signature = in.readUTF();
    flags = in.readByte();
    primitive = (flags & PRIMITIVE) != 0;
    enumType = (flags & ENUM_TYPE) != 0;
    hasWriteObject = (flags & HAS_WRITE_OBJECT) != 0;
    externalizable = (flags & EXTERNALIZABLE) != 0;
    array = (flags & ARRAY) != 0;
    superClassHandle = in.readInt();
    if (array) componentTypeHandle = in.readInt();
    if (!primitive)
    {
      int n = in.readInt();
      fields = new FieldDescriptor[n];
      for (int i=0; i<n; i++)
      {
        fields[i] = new FieldDescriptor();
        fields[i].read(in);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("handle=").append(handle).append(", ");
    sb.append("signature=").append(signature).append(", ");
    sb.append("primitive=").append(primitive).append(", ");
    sb.append("hasWriteObject=").append(hasWriteObject).append(", ");
    sb.append("externalizable=").append(externalizable).append(", ");
    sb.append("array=").append(array).append(", ");
    if (superClassHandle > 0) sb.append("superClassHandle=").append(superClassHandle).append(", ");
    if (superClass != null) sb.append("superClass=").append(superClass.signature).append(", ");
    if (componentTypeHandle > 0) sb.append("componentTypeHandle=").append(componentTypeHandle).append(", ");
    if (componentType != null) sb.append("componentType=").append(componentType.signature).append(", ");
    sb.append("fields={");
    for (int i=0; i<fields.length; i++)
    {
      if (i > 0) sb.append(", ");
      sb.append(fields[i]);
    }
    sb.append('}');
    return sb.toString();
  }
}
