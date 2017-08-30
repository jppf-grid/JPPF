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

package org.jppf.node.policy;

/**
 * An enumeration of the possible types of values to which an execution policy rule may apply.
 * @author Laurent Cohen
 */
public enum ValueType {
  /**
   * The execution policy rule applies to boolean values.
   */
  BOOLEAN,
  /**
   * The execution policy rule applies to numeric values.
   */
  NUMERIC,
  /**
   * The execution policy rule applies to string values.
   */
  STRING,
  /**
   * Used for literal string expressions that represent a property name.
   */
  PROPERTY_NAME
}
