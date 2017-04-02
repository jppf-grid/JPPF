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

package org.jppf.queue;

/**
 * Implementation of a generic non-blocking queue, to allow asynchronous access from a large number of threads.
 * @param <T> the type of jobs that are queued.
 * @param <U> the type of input bundles the jobs are split into.
 * @param <V> the type of resulting bundles the jobs are split into.
 * @author Laurent Cohen
 */
public interface JPPFQueue<T, U, V> extends Iterable<T>
{
  /**
   * Add an object to the queue, and notify all listeners about it.
   * @param bundleWrapper the object to add to the queue.
   */
  void addBundle(U bundleWrapper);

  /**
   * Get the next object in the queue.
   * @param bundleWrapper the bundle to either remove or extract a sub-bundle from.
   * @param nbTasks       the maximum number of tasks to get out of the bundle.
   * @return the most recent object that was added to the queue.
   */
  V nextBundle(T bundleWrapper, int nbTasks);

  /**
   * Determine whether the queue is empty or not.
   * @return true if the queue is empty, false otherwise.
   */
  boolean isEmpty();

  /**
   * Get the maximum bundle size for the bundles present in the queue.
   * @return the bundle size as an int.
   */
  int getMaxBundleSize();

  /**
   * Remove the specified bundle from the queue.
   * @param bundleWrapper the bundle to remove.
   * @return the bundle that was removed.
   */
  T removeBundle(T bundleWrapper);
}
