/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.server.job.management;

import java.io.Serializable;

import org.jppf.job.JobInformation;
import org.jppf.management.JPPFManagementInfo;

/**
 * Instances of this cass hold temporary information about a sub-job and the node it was dispatched to
 * @author Laurent Cohen
 */
public class NodeJobInformation implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Empty array
   */
  public static final NodeJobInformation[]    EMPTY_ARRAY = new NodeJobInformation[0];
  /**
   * The information about the node.
   */
  public final JPPFManagementInfo nodeInfo;
  /**
   * The information about the sub-job.
   */
  public final JobInformation jobInfo;

  /**
   * 
   * @param nodeInfo - the information about the node.
   * @param jobInfo - the information about the sub-job.
   */
  public NodeJobInformation(final JPPFManagementInfo nodeInfo, final JobInformation jobInfo)
  {
    this.nodeInfo = nodeInfo;
    this.jobInfo = jobInfo;
  }
}
