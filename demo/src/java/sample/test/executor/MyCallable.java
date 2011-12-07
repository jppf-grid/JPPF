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

package sample.test.executor;

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * 
 * @author Laurent Cohen
 */
public class MyCallable implements Callable<String>, Serializable
{
	/**
	 * {@inheritDoc}
	 */
	public String call() throws Exception
	{
		try
		{
			Thread.sleep(6000L);
			return "Hello World";
		}
		catch(InterruptedException e)
		{
			// InterruptedException is thrown because the node cancels the task when it times out
			return "this task timed out";
		}
	}

	/**
	 * Called when this task times out.
	 */
	public void taskExpired()
	{
		System.out.println("this task has expired");
	}
}
