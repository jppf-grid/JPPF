/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.server.queue;

import java.util.*;

import org.jppf.server.*;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class BroadcastJobCompletionListener implements TaskCompletionListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BroadcastJobCompletionListener.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Handles the mappings of nodes and communication channels.
   */
  private static NodeInformationHandler nodeHandler = JPPFDriver.getInstance().getNodeHandler();
  /**
   * The broadcast job to dispatch ot each node.
   */
  private ServerJob bundleWrapper;
  /**
   * The uuids of the nodes to which the job will be dispatched.
   */
  private Set<String> nodeUuids;
  /**
   * Flag indicating whether the broadcast job has completed.
   */
  private boolean done = false;
  /**
   * This map keeps the number of not executed tasks for each node uuid.
   */
  private Map<String, Integer> completionMap = new HashMap<String, Integer>();

  /**
   * Initialize this completion listener with the specified broadcast job and set of node uuids.
   * @param bundleWrapper the broadcast job to dispatch ot each node.
   * @param nodeUuids the uuids of the nodes to which the job will be dispatched.
   */
  public BroadcastJobCompletionListener(final ServerJob bundleWrapper, final Set<String> nodeUuids)
  {
    this.bundleWrapper = bundleWrapper;
    this.nodeUuids = nodeUuids;
    int taskCount = ((JPPFTaskBundle) bundleWrapper.getJob()).getTaskCount();
    for (String uuid: nodeUuids) completionMap.put(uuid, taskCount);
    if (debugEnabled) log.debug("task count=" + taskCount + ", completionMap=" + completionMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void taskCompleted(final ServerJob result)
  {
    JPPFTaskBundle bundle = (JPPFTaskBundle) result.getJob();
    String uuid = (String) bundle.getParameter(BundleParameter.NODE_BROADCAST_UUID);
    int n = bundle.getTaskCount();
    if (debugEnabled) log.debug("received " + n + " tasks for node uuid=" + uuid);
    int pending = completionMap.get(uuid);
    pending -= n;
    if (pending <= 0)
    {
      completionMap.remove(uuid);
      JPPFDriver.getInstance().getJobManager().jobEnded(result);
    }
    else completionMap.put(uuid, pending);
    if (completionMap.isEmpty())
    {
      ((JPPFTaskBundle) bundleWrapper.getJob()).getCompletionListener().taskCompleted(bundleWrapper);
    }
  }
}
