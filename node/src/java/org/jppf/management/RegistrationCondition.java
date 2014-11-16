/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package org.jppf.management;


/**
 * Interface implemented by an MBean that wishes to be registered and available only in specified conditions.
 * @author Laurent Cohen
 * @exclude
 */
public interface RegistrationCondition {
  /**
   * Whether the MBean should be registered.
   * @param params parameters passed.
   * @return {@code true} if the MBean should be regstered, {@code false} otherwise.
   */
  boolean mustRegister(Object...params);
}
