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
package org.jppf.jca.work;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.jppf.client.*;
import org.jppf.client.event.ClientConnectionStatusEvent;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.jca.work.submission.JcaSubmissionManager;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration commands,
 * and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether classes from
 * the submitting application should be dynamically reloaded or not, depending on whether
 * the uuid has changed or not.
 * @author Laurent Cohen
 */
public class JPPFJcaClient extends AbstractGenericClient
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFJcaClient.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Manages asynchronous work submission to the JPPF driver.
	 */
	private JcaSubmissionManager submissionManager = null;

	/**
	 * Initialize this client with a specified application UUID.
	 * @param uuid the unique identifier for this local client.
	 * @param configuration the object holding the JPPF configuration.
	 */
	public JPPFJcaClient(final String uuid, final String configuration)
	{
		super(uuid, configuration);
	}

	/**
	 * Submit a JPPFJob for execution.
	 * @param job the job to execute.
	 * @return the results of the tasks' execution, as a list of <code>JPPFTask</code> instances for a blocking job, or null if the job is non-blocking.
	 * @throws Exception if an error occurs while sending the job for execution.
	 * @see org.jppf.client.AbstractJPPFClient#submit(org.jppf.client.JPPFJob)
	 */
	@Override
	public List<JPPFTask> submit(final JPPFJob job) throws Exception
	{
		return null;
	}

	/**
	 * Close this client and release all the resources it is using.
	 */
	@Override
	public void close()
	{
		super.close();
	}

	/**
	 * Invoked when the status of a client connection has changed.
	 * @param event the event to notify of.
	 * @see org.jppf.client.event.ClientConnectionStatusListener#statusChanged(org.jppf.client.event.ClientConnectionStatusEvent)
	 */
	@Override
	public void statusChanged(final ClientConnectionStatusEvent event)
	{
		super.statusChanged(event);
		if (submissionManager != null) submissionManager.wakeUp();
	}

	/**
	 * Get the submission manager for thsi JPPF client.
	 * @return a <code>JPPFSubmissionManager</code> instance.
	 */
	public JcaSubmissionManager getSubmissionManager()
	{
		return submissionManager;
	}

	/**
	 * Set the submission manager for thsi JPPF client.
	 * @param submissionManager a <code>JPPFSubmissionManager</code> instance.
	 */
	public void setSubmissionManager(final JcaSubmissionManager submissionManager)
	{
		this.submissionManager = submissionManager;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected AbstractJPPFClientConnection createConnection(final String uuid, final String name, final JPPFConnectionInformation info)
	{
		return new JPPFJcaClientConnection(uuid, name, info, this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void initConfig(final Object configuration)
	{
		if (log.isDebugEnabled()) log.debug("initializing configuration:\n" + configuration);
		try
		{
			//TypedProperties props = new TypedProperties();
			TypedProperties props = JPPFConfiguration.getProperties();
			ByteArrayInputStream bais = new ByteArrayInputStream(((String) configuration).getBytes());
			props.load(bais);
			bais.close();
			config = props;
		}
		catch(Exception e)
		{
			log.error("Error while initializing the JPPF client configuration", e);
		}
		if (log.isDebugEnabled()) log.debug("config properties: " + config);
	}
}
