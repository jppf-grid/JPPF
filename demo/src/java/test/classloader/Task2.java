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

package test.classloader;

import java.io.Serializable;

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class Task2 extends JPPFTask
{
	/**
	 * 
	 */
	//private int test = 0;

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		MyNonStaticClass msc = new MyNonStaticClass();
		msc.run();
		setResult("Task2 successful");
	}

	/**
	 * Non-static inner class.
	 */
	public class MyNonStaticClass implements Serializable
	{
		/**
		 * Run this class.
		 */
		public void run()
		{
			System.out.println("from " + this.getClass()  + " instance");
		}
	}
}
