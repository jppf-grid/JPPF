/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.cascading;

import java.util.List;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.StringUtils;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class CascadingTestRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(CascadingTestRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Separator for each test.
	 */
	private static String banner = "\n"+StringUtils.padLeft("", '-', 80)+"\n";

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			performCommand();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jppfClient.close();
		}
		System.exit(0);
	}

	/**
	 * .
	 * @throws Exception .
	 */
	private static void performCommand() throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.addTask(new Task1());
		job.setExecutionPolicy(new Equal("id", 1));
		List<JPPFTask> results = jppfClient.submit(job);
		for (JPPFTask task: results)
		{
			if (task.getException() != null) task.getException().printStackTrace();
			else System.out.println("result: " + task.getResult());
		}
	}
}
