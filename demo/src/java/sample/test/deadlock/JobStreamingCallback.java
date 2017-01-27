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

package sample.test.deadlock;

import org.jppf.client.JPPFJob;

/**
 * Callback invoked when a job is created by the job streaming pattern.
 * This allows to configure the job, by changing the SLA and other parameters.
 * @author Laurent Cohen
 */
public interface JobStreamingCallback {
  /**
   * Called when a job is created.
   * @param job the created job.
   */
  void jobCreated(JPPFJob job);

  /**
   * Called when a job has completed.
   * @param job the job that completed.
   * @param jobStream the job stream that submitted the job.
   */
  void jobCompleted(JPPFJob job, JobStreamImpl jobStream);

  /**
   *
   */
  public static class Adapter implements JobStreamingCallback {
    @Override
    public void jobCreated(final JPPFJob job) {
    }

    @Override
    public void jobCompleted(final JPPFJob job, final JobStreamImpl jobStream) {
    }
  }
}
