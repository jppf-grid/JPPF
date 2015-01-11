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

package org.jppf.client.taskwrapper;

import java.io.Serializable;

/**
 * Wrapper interface for tasks that are not implementations of {@link org.jppf.node.protocol.Task Task<T>}.
 * @author Laurent Cohen
 * @exclude
 */
public interface TaskObjectWrapper extends Serializable
{
  /**
   * Type-safe enumeration for the type of method to execute.
   * @exclude
   */
  public enum MethodType
  {
    /**
     * Instance method type.
     */
    INSTANCE,
    /**
     * Constructor type.
     */
    CONSTRUCTOR,
    /**
     * Static method type.
     */
    STATIC
  }

  /**
   * The type of the method to execute on the object.
   * @return a <code>MethodType</code> enum value.
   */
  MethodType getMethodType();

  /**
   * Execute the task depending on its type.
   * @return the result of the execution.
   * @throws Exception if an error occurs during the execution.
   */
  Object execute() throws Exception;

  /**
   * Return the object on which a method or constructor is called.
   * @return an object or null if the invoked method is static.
   */
  Object getTaskObject();
}
