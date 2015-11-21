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

/**
 *
 * @author Laurent Cohen
 */
public interface SerializationHandler {
  /**
   * Write the declared fields of the specified class.
   * @param cd the class descriptor.
   * @param serializer the serializer to use.
   * @param obj the object for which to write the fields.
   * @throws Exception if any error occurs.
   */
  void writeDeclaredFields(final Serializer serializer, final ClassDescriptor cd, final Object obj) throws Exception;

  /**
   * Read the declared fields of the specified class.
   * @param cd the class descriptor.
   * @param deserializer the deserializer to use.
   * @param obj the object for which to read the fields.
   * @throws Exception if any error occurs.
   */
  void readDeclaredFields(final Deserializer deserializer, final ClassDescriptor cd, final Object obj) throws Exception;
}
