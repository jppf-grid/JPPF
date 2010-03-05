/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package sample.clientdataprovider;

import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.ClientDataProvider;
import org.jppf.utils.JPPFCallable;

/**
 * 
 * @author Laurent Cohen
 */
public class DataProviderTestTask extends JPPFTask
{
	/**
	 * Used as job identifier.
	 */
	public int i = 0;
	/**
	 * Used as task identifier within a job.
	 */
	public int j = 0;

	/**
	 * Initialize this task with the specified parameters
	 * @param i used as job identifier.
	 * @param j used as task identifier within a job.
	 */
	public DataProviderTestTask(int i, int j)
	{
		this.i = i;
		this.j = j;
	}

	/**
	 * Execute the task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		//System.out.println("this should be on the node side");
		ClientDataProvider dp = (ClientDataProvider) getDataProvider();
		Object o = dp.computeValue("result", new MyCallable());
		byte[] bytes = (byte[]) o;
		System.out.println("Result of client-side execution is a byte[" + bytes.length + "]");
		setResult(o);
	}

	/**
	 * A callable that simply prints a message on the client side.
	 */
	public static class MyCallable implements JPPFCallable<byte[]>
	{
		/**
		 * Execute this callable.
		 * @return a string message.
		 * @see java.util.concurrent.Callable#call()
		 */
		public byte[] call()
		{
			//String s = "this should be on the client side";
			//System.out.println(s);
			return new byte[10*1024*1024];
		}
	}
}
