/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
			setResult("Hello World " + count);
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
			Iterator it = results.iterator();
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
		System.exit(0);
	}
}
