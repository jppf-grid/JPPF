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

package test.org.jppf.test.setup.common;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.event.*;
import org.jppf.client.submission.SubmissionStatus;

/**
 * Sets a flag to {@code true} when the job status becomes {@link SubmissionStatus#EXECUTING}.
 * @author Laurent Cohen
 */
public class ExecutingJobStatusListener implements SubmissionStatusListener {
  /**
   * This flag is set to {@code true} when the job status becomes {@link SubmissionStatus#EXECUTING}.
   */
  private final AtomicBoolean executing = new AtomicBoolean(false);

  @Override
  public void submissionStatusChanged(final SubmissionStatusEvent event) {
    if (event.getStatus() == SubmissionStatus.EXECUTING) executing.compareAndSet(false, true);
  }

  /**
   * Wait until the executing flag is set to {@code true}.
   */
  public void await() {
    try {
      while (!executing.get()) Thread.sleep(10L);
    } catch(InterruptedException ignore) {
    }
  }
}
