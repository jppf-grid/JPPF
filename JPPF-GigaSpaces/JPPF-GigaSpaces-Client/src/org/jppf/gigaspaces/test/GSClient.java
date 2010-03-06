/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.gigaspaces.test;

import org.jppf.client.JPPFJob;
import org.jppf.gigaspaces.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Client class used to invoke a JPPF job submission service deployed as a processing unit.
 * @author Laurent Cohen
 */
public class GSClient implements InitializingBean
{
	/**
	 * Reference to the JPPF service.
	 */
	private JPPFService jppfService = null;
	/**
	 * The job that was recently sent.
	 */
	private JPPFJob job = null;

	/**
	 * Entry point for execution of this client as a standalone application.
	 * @param args not used.
	 */
	public static void main(String[] args)
	{
		execute();
	}

	/**
	 * Initialize the Spring context, invoke the appropriate bean method,
	 * and store the results of the JPPF execution.
	 * @return the results as a <code>JPPFJob</code> instance.
	 */
	public static JPPFJob execute()
	{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:client.xml");
		context.start();
		GSClient gsc = (GSClient) context.getBean("gsclient");
		return gsc.getJob();
	}

	/**
	 * Default constructor.
	 */
	public GSClient()
	{
	}

	/**
	 * Called after the Spring bean initialization and submits a JPPF job to the JPPF space.
	 * @throws Exception if any error occurs.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception
	{
		JPPFJob newJob = new JPPFJob();
		newJob.addTask(new HelloTask());
		this.job = jppfService.submitJob(newJob);
	}

	/**
	 * Get a proxy to the service deployed in a GS space.
	 * @return a <code>JPPFService</code> instance.
	 */
	public JPPFService getJppfService()
	{
		return jppfService;
	}

	/**
	 * Set a proxy to the service deployed in a GS space.
	 * @param service a <code>JPPFService</code> instance.
	 */
	public void setJppfService(JPPFService service)
	{
		this.jppfService = service;
	}

	/**
	 * Get the resulting JPPF job instance.
	 * @return a <code>JPPFJob</code> instance
	 */
	public JPPFJob getJob()
	{
		return job;
	}
}
