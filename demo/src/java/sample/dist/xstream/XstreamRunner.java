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
package sample.dist.xstream;

import java.util.*;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Runner class for the XStream demo.
 * @author Laurent Cohen
 */
public class XstreamRunner
{
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, submits the tasks to the JPPF grid.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			long start = System.currentTimeMillis();
			JPPFJob job = new JPPFJob();
			Person person = new Person("John", "Smith", new PhoneNumber(123, "456-7890"));
			job.addTask(new XstreamTask(person));
			// submit the tasks for execution
			List<JPPFTask> results = jppfClient.submit(job);
			long elapsed = System.currentTimeMillis() - start;
			System.out.println("Task executed in " + elapsed + " ms");
			JPPFTask result = results.get(0);
			if (result.getException() != null) throw result.getException();
			System.out.println("Task execution result: " + result.getResult());
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
}
