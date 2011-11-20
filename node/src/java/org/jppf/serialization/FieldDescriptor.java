/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import java.lang.reflect.Field;

/**
 * Instances of this class describe a non-transient field of a Java class with in order ot enable serialization and deserialization of this field.
 * @author Laurent Cohen
 */
class FieldDescriptor
{
  /**
   * The name of this field.
   */
  String name;
  /**
   * The corresponding field object.
   */
  Field field;
  /**
   * Descriptor for the type of this field.
   */
  ClassDescriptor type;
  /**
   * Handler for the field type, use at deserialization only.
   */
  int typeHandle;

  /**
   * Initialize an empty field descriptor.
   */
  FieldDescriptor()
  {
  }

  /**
   * Initialize a field descriptor from a field.
   * @param field the field to initialize from.
   * @throws Exception if any error occurs.
   */
  FieldDescriptor(final Field field) throws Exception
  {
    this.field = field;
    name = field.getName();
  }


  /**
   * Write this field descriptor to an object output stream.
   * @param out the stream to write to.
   * @throws IOException if any error occurs.
   */
  void write(final ObjectOutputStream out) throws IOException
  {
    out.writeUTF(name);
    out.writeInt(type.handle);
  }

  /**
   * Read this class descriptor from an input stream.
   * @param in the stream to read from.
   * @throws IOException if any error occurs.
   */
  void read(final ObjectInputStream in) throws IOException
  {
    name = in.readUTF();
    typeHandle = in.readInt();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    return "FieldDescriptor[name=" + name + ", type=" + type + ", typeHandle=" + typeHandle + ']';
  }
}
