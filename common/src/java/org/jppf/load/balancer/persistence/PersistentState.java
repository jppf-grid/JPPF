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

package org.jppf.load.balancer.persistence;

import java.util.concurrent.locks.Lock;

/**
 * This interface is implemented by load-balancers that wish to persist their state.
 * @author Laurent Cohen
 * @since 6.0
 */
public interface PersistentState {
  /**
   * Get this bundler's state.
   * @return an Object representing the state of the load-balancer.
   */
  Object getState();

  /**
   * Set this bundler's state.
   * @param state an Object representing the state of the load-balancer.
   */
  void setState(Object state);

  /**
   * Get a lock that can be used to synchronize access to the load-balancer state.
   * The main usage is to avoid race conditions when the state is serialized by the persistence thread,
   * while it is being updated, in particular via its {@code feedback()} method, in another thread.
   * @return a {@link Lock} object.
   */
  Lock getStateLock();
}
