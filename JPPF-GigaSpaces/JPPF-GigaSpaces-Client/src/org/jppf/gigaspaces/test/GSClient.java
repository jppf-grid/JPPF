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
	 * Initialize the Spring context, invoke the appropriate bean method,
	 * and return the results of the JPPF execution.
	 * @param jobName the name given to the JPPF job.
	 * @param nbTasks the number of tasks in the job.
	 * @param taskDuration the duration in milliseconds of each task in the job.
	 * @return the results as a <code>JPPFJob</code> instance.
	 * @throws Exception if any error occurs.
	 */
	public static JPPFJob execute(String jobName, int nbTasks, long taskDuration) throws Exception
	{
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:client.xml");
		context.start();
		GSClient gsc = (GSClient) context.getBean("gsclient");
		return gsc.runJob(jobName, nbTasks, taskDuration);
	}

	/**
	 * Default constructor.
	 */
	public GSClient()
	{
	}

	/**
	 * Called after the Spring bean initialization.
	 * @throws Exception if any error occurs.
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	public void afterPropertiesSet() throws Exception
	{
	}

	/**
	 * Execute a job with the specified parameters, submitting it to the JPPF space. 
	 * @param jobName the name given to the JPPF job.
	 * @param nbTasks the number of tasks in the job.
	 * @param taskDuration the duration in milliseconds of each task in the job.
	 * @return the results as a <code>JPPFJob</code> instance.
	 * @throws Exception if any error occurs.
	 */
	public JPPFJob runJob(String jobName, int nbTasks, long taskDuration) throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.setId(jobName);
		for (int i=1; i<= nbTasks; i++)
		{
			HelloTask task = new HelloTask(taskDuration);
			task.setId("" + i);
			job.addTask(task);
		}
		return jppfService.submitJob(job);
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
}
