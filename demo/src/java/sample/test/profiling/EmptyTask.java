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
package sample.test.profiling;

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class do nothing and are intented for node profiling purposes,
 * to analyse the JPPF overhead for task execution. 
 * @author Laurent Cohen
 */
public class EmptyTask extends JPPFTask
{
	/**
	 * The data size in KB.
	 */
	private int dataSizeKB = 0;
	/**
	 * The data in this task.
	 */
	private byte[] data = null;
	/**
	 * Initialize with the specified data size.
	 * @param dataSizeKB the data size in KB.
	 */
	public EmptyTask(int dataSizeKB)
	{
		this.dataSizeKB = dataSizeKB;
		data = new byte[1024*dataSizeKB];
	}

	/**
	 * Perform the excution of this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
	}
}
