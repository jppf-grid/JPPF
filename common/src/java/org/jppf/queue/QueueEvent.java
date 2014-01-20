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

package org.jppf.queue;

import java.util.EventObject;

/**
 * Instances of this class represent <code>JPPFQueue</code> events.
 * @param <T> the type of jobs that are queued.
 * @param <U> the type of bundles the jobs are split into.
 * @param <V> the type of resulting bundles the jobs are split into.
 * @author Laurent Cohen
 */
public class QueueEvent<T, U, V> extends EventObject
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Represents part or the totality of a job that was submitted.
   */
  private transient T bundleWrapper = null;
  /**
   * Determines if the event is a requeued bundle, following a node failure for instance.
   */
  private boolean requeued = false;

  /**
   * Initialize this event with the specified queue and bundle.
   * @param queue         - the queue this event originates from.
   * @param bundleWrapper - represents part or the totality of a job that was submitted.
   * @param requeue       - determines if the event is a requeued bundle, following a node failure for instance.
   */
  public QueueEvent(final JPPFQueue<T, U, V> queue, final T bundleWrapper, final boolean requeue)
  {
    super(queue);
    this.bundleWrapper = bundleWrapper;
    this.requeued = requeue;
  }

  /**
   * Get the queue this event originates from.
   * @return an instance of <code>JPPFQueue</code>.
   */
  @SuppressWarnings("unchecked")
  public JPPFQueue<T, U, V> getQueue()
  {
    return (AbstractJPPFQueue<T, U, V>) getSource();
  }

  /**
   * Get the task bundle that is the cause of the event.
   * @return an instance of <code>ClientJob</code>.
   */
  public T getBundleWrapper()
  {
    return bundleWrapper;
  }

  /**
   * Determine if this event is a requeued bundle, following a node failure for instance.
   * @return true if a bundle was requeued, false otherwise.
   */
  public boolean isRequeued()
  {
    return requeued;
  }
}
