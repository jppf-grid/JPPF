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
package test.node.nativelib;

import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a staring point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class NativeLibRunner
{
	/**
	 * The JPPF client, handles all communications with the server.
	 * It is recommended to only use one JPPF client per JVM, so it
	 * should generally be created and used as a singleton.
	 */
	private static JPPFClient jppfClient =  null;

	/**
	 * The entry point for this application runner to be run from a Java command line.
	 * @param args by default, we do not use the command line arguments,
	 * however nothing prevents us from using them if need be.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			NativeLibRunner runner = new NativeLibRunner();
			JPPFJob job = runner.createJob();
	    job.setId("Native Lib Loading");
      System.out.println("submitting job #" + job.getId());
			runner.executeBlockingJob(job);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jppfClient != null) jppfClient.close();
		}
	}

	/**
	 * Create a JPPF job that can be submitted for execution.
	 * @return an instance of the {@link org.jppf.client.JPPFJob JPPFJob} class.
	 * @throws Exception if an error occurs while creating the job or adding tasks.
	 */
	public JPPFJob createJob() throws Exception
	{
		JPPFJob job = new JPPFJob();
		NativeLibTask task = new NativeLibTask();
		job.addTask(task);
		return job;
	}

	/**
	 * Execute a job in blocking mode. The application will be blocked until the job
	 * execution is complete.
	 * @param job the JPPF job to execute.
	 * @throws Exception if an error occurs while executing the job.
	 */
	public void executeBlockingJob(JPPFJob job) throws Exception
	{
		job.setBlocking(true);
		List<JPPFTask> results = jppfClient.submit(job);
		for (JPPFTask task: results)
		{
			if (task.getException() != null)
			{
				System.out.println("Caught exception:");
				task.getException().printStackTrace();
			}
			else
			{
				System.out.println("Result:" + task.getResult());
			}
		}
	}
}
