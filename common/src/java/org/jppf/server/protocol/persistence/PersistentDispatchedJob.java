/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.server.protocol.persistence;

import java.io.Serializable;
import java.util.*;

import org.jppf.node.protocol.Location;
import org.jppf.server.protocol.*;

/**
 * 
 * @author Laurent Cohen
 */
public class PersistentDispatchedJob implements Serializable
{
  /**
   * The job to execute.
   */
  private String jobUuid;
  /**
   * The shared data provider for this task bundle.
   */
  private Location dataProvider;
  /**
   * The tasks to be executed by the node.
   */
  private List<PersistentDispatchedTask> taskList;
  /**
   * The job this submission is for
   */
  private JPPFTaskBundle taskBundle;
  /**
   * Channel to which is this bundle dispatched.
   */
  private String nodeUuid;
  /**
   * The number of tasks in this node bundle.
   */
  private int taskCount;

  /**
   * Initialize this persistent job with the specified node bundle sent to a node.
   * @param nodeBundle the node bundle from which to get the data to persist.
   * @exclude
   */
  public PersistentDispatchedJob(final ServerTaskBundleNode nodeBundle)
  {
    this.jobUuid = nodeBundle.getJob().getUuid();
    this.dataProvider = PersistenceHelper.toLocation(nodeBundle.getDataProvider());
    this.taskList = new ArrayList<>(nodeBundle.getTaskList().size());
    for (ServerTask task: nodeBundle.getTaskList()) this.taskList.add(new PersistentDispatchedTask(task));
    this.taskBundle = nodeBundle.getJob();
    this.nodeUuid = nodeBundle.getChannel().getUuid();
    this.taskCount = nodeBundle.getTaskCount();
  }
}
