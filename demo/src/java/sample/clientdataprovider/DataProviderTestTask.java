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
	 * Execute the task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		System.out.println("this should be on the node side");
		ClientDataProvider dp = (ClientDataProvider) getDataProvider();
		Object o = dp.computeValue("result", new MyCallable());
		System.out.println("Result of client-side execution:\n" + o);
		setResult(o);
	}

	/**
	 * A callable that simply prints a message on the client side.
	 */
	public static class MyCallable implements JPPFCallable<String>
	{
		/**
		 * Execute this callable.
		 * @return a string message.
		 * @see java.util.concurrent.Callable#call()
		 */
		public String call()
		{
			String s = "this should be on the client side";
			System.out.println(s);
			return null;
		}
	}
}
