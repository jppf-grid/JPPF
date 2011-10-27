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
package org.jppf.jca.work.submission;

import static org.jppf.client.SubmissionStatus.*;

import org.jppf.client.*;
import org.jppf.node.protocol.Task;

/**
 * Wrapper for submitting a job.
 */
public class JcaJobSubmission extends AbstractJobSubmission
{
	/**
	 * Initialize this job submission.
	 * @param job the submitted job.
	 * @param connection the connection to execute the job on.
	 * @param locallyExecuting determines whether the job will be executed locally, at least partially.
	 */
	JcaJobSubmission(final JPPFJob job, final AbstractJPPFClientConnection connection, final boolean locallyExecuting)
	{
		super(job, connection, locallyExecuting);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run()
	{
		JcaSubmissionResult result = (JcaSubmissionResult) job.getResultListener();
		String requestUuid = job.getJobUuid();
		ClassLoader cl = null;
		ClassLoader oldCl = null;
		if (!job.getTasks().isEmpty())
		{
			Task task = job.getTasks().get(0);
			cl = task.getClass().getClassLoader();
			connection.getClient().addRequestClassLoader(requestUuid, cl);
			if (JcaSubmissionManager.log.isDebugEnabled()) JcaSubmissionManager.log.debug("adding request class loader=" + cl + " for uuid=" + requestUuid);
		}
		try
		{
			if (cl != null)
			{
				oldCl = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(cl);
			}
			connection.getClient().getLoadBalancer().execute(this, connection, locallyExecuting);
			result.waitForResults(0);
			connection.getClient().removeRequestClassLoader(requestUuid);
			result.setStatus(COMPLETE);
		}
		catch (Exception e)
		{
			result.setStatus(FAILED);
			JcaSubmissionManager.log.error(e.getMessage(), e);
		}
		finally
		{
			if (connection != null) connection.setStatus(JPPFClientConnectionStatus.ACTIVE);
			if (cl != null) Thread.currentThread().setContextClassLoader(oldCl);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getId()
	{
		return job.getJobUuid();
	}
}