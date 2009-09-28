/*
 * Java Parallel Processing Framework.
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
package sample.datasize;

import org.jppf.server.protocol.JPPFTask;


/**
 * This task is for testing the netwrok transfer of task with various data sizes.
 * @author Laurent Cohen
 */
public class DataTask extends JPPFTask
{
	/**
	 * The data this task owns.
	 */
	private byte[] data = null;

	/**
	 * Initialize this task with a specified row of values to multiply.
	 * @param datasize - the size in byte of the byte array this task owns.
	 */
	public DataTask(int datasize)
	{
		data = new byte[datasize];
	}
	
	/**
	 * Perform the multiplication of a matrix row by another matrix.
	 * @see sample.BaseDemoTask#doWork()
	 */
	public void run()
	{
		try
		{
		}
		catch(Exception e)
		{
			setException(e);
		}
	}
}

