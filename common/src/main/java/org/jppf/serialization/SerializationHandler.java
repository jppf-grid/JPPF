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

/**
 *
 * @author Laurent Cohen
 */
public interface SerializationHandler {
  /**
   * Serialize an object of the type processed by this serialization handler.
   * @param obj the object for which to write the fields.
   * @param serializer the serializer to use.
   * @param cd the class descriptor.
   * @throws Exception if any error occurs.
   */
  void writeObject(final Object obj, final Serializer serializer, final ClassDescriptor cd) throws Exception;

  /**
   * Deserialize an object of the type processed by this serialization handler.
   * @param deserializer the deserializer to use.
   * @param cd the class descriptor.
   * @return the deserialized object.
   * @throws Exception if any error occurs.
   */
  Object readDObject(final Deserializer deserializer, final ClassDescriptor cd) throws Exception;

  /**
   * Create a new instance of the class described by the specified descriptor.
   * @param cd the class descriptor to use.
   * @return a new instance of the class.
   * @throws Exception if any error occurs.
   */
  default Object newInstance(final ClassDescriptor cd) throws Exception {
    return SerializationReflectionHelper.create(cd.clazz);
  }
}
