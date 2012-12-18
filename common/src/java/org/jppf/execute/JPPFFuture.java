/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.execute;

import java.util.concurrent.Future;

/**
 * Interface for notifying Future.
 * @param <V> The result type returned by <code>get</code> method.
 * @author Martin JANDA
 */
public interface JPPFFuture<V> extends Future<V>
{
  /**
   * Add a future done listener to this future's list of listeners.
   * @param listener the listener to add to the list.
   * @throws IllegalArgumentException when listener is null.
   */
  void addListener(final Listener listener);

  /**
   * Instances of this interface listen transition of Future to done state.
   */
  public interface Listener
  {
    /**
     * Invoked to notify that future is done (normally or cancelled).
     * @param future the future that has been done.
     */
    void onDone(final JPPFFuture<?> future);
  }
}
