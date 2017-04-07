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

package test.org.jppf.test.setup.common;

import static org.jppf.client.event.JobEvent.Type.*;

import org.jppf.client.JPPFJob;
import org.jppf.client.event.*;
import org.jppf.utils.ThreadSynchronization;

/**
 *
 * @author Laurent Cohen
 */
public class AwaitJobListener extends ThreadSynchronization implements JobListener {
  /**
   * Whether the expected event has been received.
   */
  private boolean eventReceived;
  /**
   * The type of event to wait for.
   */
  private final JobEvent.Type expectedEventType;

  /**
   * Initialize this listener.
   * @param job the job to which this listener is registered.
   * @param eventType the type of event to wait for.
   */
  public AwaitJobListener(final JPPFJob job, final JobEvent.Type eventType) {
    this.expectedEventType = eventType;
    job.addJobListener(this);
  }

  @Override
  public void jobStarted(final JobEvent event) {
    checkEventType(event.getJob(), JOB_START);
  }

  @Override
  public void jobEnded(final JobEvent event) {
    checkEventType(event.getJob(), JOB_END);
  }

  @Override
  public void jobDispatched(final JobEvent event) {
    checkEventType(event.getJob(), JOB_DISPATCH);
  }

  @Override
  public void jobReturned(final JobEvent event) {
    checkEventType(event.getJob(), JOB_RETURN);
  }

  /**
   * Check whether the specified event is the excpeted one and take appropriate action.
   * @param job the job to which this listener is registered.
   * @param type the type of event to check.
   */
  private synchronized void checkEventType(final JPPFJob job, final JobEvent.Type type) {
    if (expectedEventType == type) {
      eventReceived = true;
      job.removeJobListener(this);
      wakeUp();
    }
  }

  /**
   * Wait for the specified event.
   * @throws Exception if any error occurs.
   */
  public synchronized void await() throws Exception {
    while (!eventReceived) goToSleep(100L);
  }
}
