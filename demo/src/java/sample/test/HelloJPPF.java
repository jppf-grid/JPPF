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
package sample.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;

/**
 * "Hello World" example of a JPPF client. This client simply submits a task that returns the "Hello World" string as
 * result. The implementation needs an instance of class JPPFClient to access the server and submit tasks. Next an
 * implementation of the JPPFTask is required to implement the calculations that are performed in a remote node. The
 * code requires the jppf-client.jar and jppf-common.jar Java archives.
 */
public class HelloJPPF implements Serializable
{
	/**
	 * JPPF Task used in the HelloJPPF code sample.
	 */
	public class InnerTask extends JPPFTestTask
	{
		/** task count */
		int count = 1;

		/**
		 * Constructor with a sequence number argument.
		 * @param cnt this task's sequence number.
		 */
		InnerTask(int cnt)
		{
			count = cnt;
		}

		/**
		 * Calculate the result of the task and set the result object with setResult().
		 */
		public void test()
		{
			// ModelPackage a = ModelPackage.createPackage("x");
			setResult("Hello World " + count + " from inner class of "+HelloJPPF.this.getClass());
		}
	}

	/**
	 * The main procedure.
	 * @param args not used.
	 */
	public static void main(String[] args)
	{
		HelloJPPF h = new HelloJPPF();
		h.testInnerTask();
	}

	/**
	 * Test with a non-static inner class implementation of the class.
	 */
	void testInnerTask()
	{
		JPPFClient client = new JPPFClient();
		List<JPPFTask> tasks = new ArrayList<JPPFTask>();
		for (int i = 1; i < 4; i++)
		{
			tasks.add(new InnerTask(i));
		}
		try
		{
			// execute tasks
			List<JPPFTask> results = client.submit(tasks, null);
			// show results
			System.out.println("Got " + results.size() + " results: ");
			Iterator<JPPFTask> it = results.iterator();
			while (it.hasNext())
			{
				JPPFTask t = (JPPFTask) it.next();
				System.out.println("Result object: " + t);
				System.out.println("Result: " + t.getResult() + ", Exception: " + t.getException());
				if (null != t.getException())
				{
					t.getException().printStackTrace();
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			client.close();
		}
	}
}
