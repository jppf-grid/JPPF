/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.nodeserver.async;

import org.jppf.server.protocol.*;

/**
 * 
 * @author Laurent Cohen
 */
class NodeJobEntry {
  /**
   * The task bundle to send or receive.
   */
  final ServerTaskBundleNode nodeBundle;
  /**
   * The job uuid.
   */
  final String jobUuid;

  /**
   * 
   * @param nodeBundle the task bundle to send or receive.
   */
  NodeJobEntry(final ServerTaskBundleNode nodeBundle) {
    this.nodeBundle = nodeBundle;
    this.jobUuid = nodeBundle.getJob().getUuid();
  }

  /**
   * @return the uuid of the current job, if any.
   */
  synchronized String getJobUuid() {
    return jobUuid;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("jobUuid=").append(jobUuid)
      .append(", jobName=").append(nodeBundle.getJob().getName())
      .append(']').toString();
  }
}