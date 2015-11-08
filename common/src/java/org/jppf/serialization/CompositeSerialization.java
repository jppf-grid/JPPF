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
 * Abstract super class for serialization schemes that delegate to another serialization scheme.
 * @author Laurent Cohen
 */
abstract class CompositeSerialization implements JPPFSerialization {
  /**
   * Serialization scheme to delegate to.
   */
  private JPPFSerialization delegate;

  /**
   * Set the concrete serialization to delegate to.
   * @param delegate the serialization scheme to compress/decompress.
   * @return this serialization scheme.
   */
  CompositeSerialization delegateTo(final JPPFSerialization delegate) {
    this.delegate = delegate;
    return this;
  }

  /**
   * Get the concrete serialization to delegate to.
   * @return the serialization scheme to compress/decompress.
   */
  JPPFSerialization getDelegate() {
    return delegate;
  }
}
