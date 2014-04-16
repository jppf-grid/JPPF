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

package test.client.schedule;

import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;

/**
 * This subclass of {@link JPPFClient} overrides the {@code submitJob()} method
 * to add a {@link MyJobListener} to each job.
 * @author Laurent Cohen
 */
public class MyJPPFClient extends JPPFClient {
  /**
   * Handles the job expiration.
   */
  private final JobListener jobListener;

  /**
   * Initialize this client with an automatically generated UUID.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public MyJPPFClient(final ClientListener... listeners) {
    super(listeners);
    jobListener = new MyJobListener(this);
  }

  /**
   * Initialize this client with the specified UUID and new connection listeners.
   * @param uuid the unique identifier for this local client.
   * @param listeners the listeners to add to this JPPF client to receive notifications of new connections.
   */
  public MyJPPFClient(final String uuid, final ClientListener... listeners) {
    super(uuid, listeners);
    jobListener = new MyJobListener(this);
  }

  @Override
  public List<Task<?>> submitJob(final JPPFJob job) throws Exception {
    job.addJobListener(jobListener);
    return super.submitJob(job);
  }

  @Override
  public void close() {
    super.close();
    if (jobListener != null) ((MyJobListener) jobListener).close();
  }
}
