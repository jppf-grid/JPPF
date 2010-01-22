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

package sample.test;

import org.jppf.server.protocol.JPPFTask;

/**
 * This task is for testing the scenario where an attribute is not serializable, initially null at task construction time,
 * and non-null after the task is executed.
 * @author Laurent Cohen
 */
public class NonSerializableAttributeTask extends JPPFTask
{
	/**
	 * Non-serializable attribute, must be null before th etask execution..
	 */
	private NonSerializable ns = null;

	/**
	 * Execute this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		ns = new NonSerializable();
		setResult("execution successfull");
	}

	/**
	 * A dummy non-serializable class.
	 */
	public static class NonSerializable
	{
		/**
		 * A dummy attribute.
		 */
		public int field = 0;
	}
}
