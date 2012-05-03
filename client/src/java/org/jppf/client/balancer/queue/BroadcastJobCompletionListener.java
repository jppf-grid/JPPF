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

package org.jppf.client.balancer.queue;

import org.jppf.client.balancer.ChannelWrapper;
import org.jppf.client.balancer.ClientCompletionListener;
import org.jppf.client.balancer.ClientJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Laurent Cohen
 */
public class BroadcastJobCompletionListener implements ClientCompletionListener
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
   * The broadcast job to dispatch ot each node.
   */
  private ClientJob bundleWrapper;
  /**
   * This map keeps the number of not executed tasks for each node uuid.
   */
  private Map<String, Integer> completionMap = new HashMap<String, Integer>();

  /**
   * Initialize this completion listener with the specified broadcast job and set of node uuids.
   * @param bundleWrapper the broadcast job to dispatch ot each node.
   * @param connections   list of all connections on which broadcast job is executed.
   */
  public BroadcastJobCompletionListener(final ClientJob bundleWrapper, final List<ChannelWrapper> connections)
  {
    this.bundleWrapper = bundleWrapper;
    int taskCount = bundleWrapper.getTaskCount();
    for (ChannelWrapper connection : connections)
    {
      ChannelWrapper xConnection = (ChannelWrapper) connection;
      completionMap.put(xConnection.getUuid(), taskCount);
    }
    if (debugEnabled) log.debug("task count=" + taskCount + ", completionMap=" + completionMap);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void taskCompleted(final ClientJob result)
  {
    String uuid = result.getBroadcastUUID();
    int n = result.getTaskCount();
    if (debugEnabled) log.debug("received " + n + " tasks for node uuid=" + uuid);
    int pending = completionMap.get(uuid);
    pending -= n;
    if (pending <= 0)
    {
      completionMap.remove(uuid);
    }
    else completionMap.put(uuid, pending);
    if (completionMap.isEmpty())
    {
      bundleWrapper.fireTaskCompleted();
    }
  }
}
