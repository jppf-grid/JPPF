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
public class Task1 extends JPPFTask
{
	/**
	 * Test field.
	 */
	private MyStaticClass msc = null;

	/**
	 * Default constructor.
	 */
	public Task1()
	{
		msc = new MyStaticClass();
	}

	/**
	 * {@inheritDoc}
	 */
	public void run()
	{
		msc.run();
		setResult("Task1 successful");
	}

	/**
	 * Static inner class.
	 */
	public static class MyStaticClass implements Serializable
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
