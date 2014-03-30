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

package org.jppf.client.event;

import java.util.EventObject;

import org.jppf.client.*;
import org.jppf.client.balancer.queue.JPPFPriorityQueue;

/**
 * Instances of this class represent events emitted by the JPPF client job queue whenever a job is added to or removed from the queue.
 * @author Laurent Cohen
 * @since 4.1
 */
public class ClientQueueEvent extends EventObject {
  /**
   * The job this event is for.
   */
  private final JPPFJob job;

  /**
   * Initialize this event with the specified source JPPF client and Job.
   * @param client the client whose queue emitted the event.
   * @param job the job this event is for.
   * @param queue the job queue which emitted this event.
   */
  public ClientQueueEvent(final JPPFClient client, final JPPFJob job, final JPPFPriorityQueue queue) {
    super(client);
    this.job = job;
  }

  /**
   * Get the JPPF client source of this event.
   * @return a {@link JPPFClient} instance.
   */
  public JPPFClient getClient() {
    return (JPPFClient) getSource();
  }

  /**
   * Get the job this event is for.
   * @return a {@link JPPFJob} instance.
   */
  public JPPFJob getJob() {
    return job;
  }
}
