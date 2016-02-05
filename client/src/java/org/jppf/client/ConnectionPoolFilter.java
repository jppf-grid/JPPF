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

package org.jppf.client;

/**
 * Interface for filtering {@link ConnectionPool connection pools}.
 * @param <E> the type of connection pools handled by this filter.
 * @author Laurent Cohen
 * @since 4.2
 */
public interface ConnectionPoolFilter<E extends ConnectionPool> {
  /**
   * Determine whether this filter accepts the specified connection pool.
   * @param pool the connection pool to check.
   * @return {@code true} if the connection pool is accepted, {@code false} otherwise.
   */
  boolean accepts(E pool);
}
