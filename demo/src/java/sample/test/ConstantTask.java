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

package sample.test;


/**
 * Test task to check that correct results are returned by the framework.
 * @author Laurent Cohen
 */
public class ConstantTask extends JPPFTestTask
{
	/**
	 * Initialize this task with a specified returned result.
	 * @param n the task result as an integer value.
	 */
	public ConstantTask(final int n)
	{
		setResult(Integer.valueOf(n));
	}

	/**
	 * Execute the task
	 * @see java.lang.Runnable#run()
	 */
	public void test()
	{
	}
}
