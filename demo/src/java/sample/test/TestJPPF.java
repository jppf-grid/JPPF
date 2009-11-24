/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package sample.test;

import java.net.*;

import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.ClientDataProvider;
import org.jppf.utils.JPPFCallable;

/**
 * Test task to check that correct results are returned by the framework.
 * 
 * @author Laurent Cohen
 */
public class TestJPPF extends JPPFTask
{
	/**
	 * Test variable.
	 */
	private int test;

	/**
	 * Default constructor.
	 */
	public TestJPPF()
	{
		this.test = 0;
		// serialize person to XML
	}

	/**
	 * Execute the task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		String hName = "";

		// BucketRendererNode ttt = new BucketRendererNode();

		System.out.println("this should be on the node side");
		ClientDataProvider dp = (ClientDataProvider) getDataProvider();
		// compute a value on the client side and store in the data provider
		Object o = dp.computeValue("result", new MyCallable());
		System.out.println("Result of client-side execution:\n" + o);
		// setResult(o);

		try
		{
			hName = InetAddress.getLocalHost().getHostName() + "  Number: "/* + ttt.taskID */;
		}
		catch (UnknownHostException ex)
		{
			System.out.println("Error !!!");
		}
		setResult(hName + o.toString());
	}

	/**
	 * Callback to execute on the client side.
	 */
	public static class MyCallable implements JPPFCallable<String>
	{
		/**
		 * Execute this callback.
		 * @return a string.
		 * @see java.util.concurrent.Callable#call()
		 */
		public String call()
		{
			String s = "this should be on the client side";
			System.out.println(s);
			return s;
		}
	}
}
