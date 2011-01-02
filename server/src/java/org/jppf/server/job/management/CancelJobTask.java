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

import org.jppf.management.*;
import org.jppf.server.JPPFDriver;
import org.jppf.server.nio.ChannelWrapper;
import org.jppf.server.nio.nodeserver.AbstractNodeContext;
import org.slf4j.*;

/**
 * Instances of this class are intented to perform job management functions for a specxific node. 
 * @author Laurent Cohen
 */
class CancelJobTask implements Runnable
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(CancelJobTask.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The id of the job to manage.
	 */
	private String jobId = null;
	/**
	 * The node on which to perform this task.
	 */
	private ChannelWrapper channel = null;
	/**
	 * True if the job should be requeued on the server side, false otherwise.
	 */
	private boolean requeue = true;

	/**
	 * Initialize this task.
	 * @param jobId the id of the job to manage.
	 * @param channel the node on which to perform this task.
	 * @param requeue true if the job should be requeued on the server side, false otherwise.
	 */
	public CancelJobTask(String jobId, ChannelWrapper channel, boolean requeue)
	{
		this.jobId = jobId;
		this.channel = channel;
		this.requeue = requeue;
	}

	/**
	 * Execute this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			AbstractNodeContext context = (AbstractNodeContext) channel.getContext();
			context.setJobCanceled(true);
			JPPFManagementInfo nodeInfo = JPPFDriver.getInstance().getNodeInformation(channel);
			if (debugEnabled) log.debug("Request to cancel jobId = '" + jobId + "' on node " + channel + ", requeue = " + requeue);
			if (nodeInfo == null) return;
			JMXNodeConnectionWrapper node = new JMXNodeConnectionWrapper(nodeInfo.getHost(), nodeInfo.getPort());
			node.connect();
			while (!node.isConnected()) Thread.sleep(10);
			node.invoke(JPPFAdminMBean.NODE_MBEAN_NAME, "cancelJob", new Object[] { jobId, requeue }, new String[] { "java.lang.String", "java.lang.Boolean" });
			node.close();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
