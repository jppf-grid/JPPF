/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
package test.bufferspace;

import org.jppf.server.protocol.JPPFTask;


/**
 * This task is for testing the netwrok transfer of task with various data sizes.
 * @author Laurent Cohen
 */
public class BufferspaceTask extends JPPFTask
{
	/**
	 * NSO.
	 */
	//private transient Object nso = null;

	/**
	 * The data this task owns.
	 */
	private byte[] data = null;
	/**
	 * Determines whether this task has been executed.
	 */
	private boolean executed = false;
	/**
	 * The duration of this task.
	 */
	private long duration = -1L;

	/**
	 * Determine whether this task has been executed.
	 * @return true if the task was executed, false otherwise.
	 */
	public boolean isExecuted()
	{
		return executed;
	}

	/**
	 * Initialize this task with a specified row of values to multiply.
	 * @param datasize - the size in byte of the byte array this task owns.
	 */
	public BufferspaceTask(int datasize)
	{
		data = new byte[datasize];
	}
	
	/**
	 * Initialize this task with a specified row of values to multiply.
	 * @param datasize the size in byte of the byte array this task owns.
	 * @param duration the duration of this task.
	 */
	public BufferspaceTask(int datasize, long duration)
	{
		this(datasize);
		this.duration = duration;
	}
	
	/**
	 * Perform the multiplication of a matrix row by another matrix.
	 * @see sample.BaseDemoTask#doWork()
	 */
	public void run()
	{
		try
		{
			executed = true;
			if (duration > 0) Thread.sleep(duration);
		}
		catch(Exception e) 
		{
			setException(e);
		}
	}

	/**
	 * 
	 * @param out .
	 * @throws IOException .
	 * @throws ClassNotFoundException .
	 */
	/*
	private void writeObject(ObjectOutputStream out) throws IOException, ClassNotFoundException
	{
    nso = new test.bufferspace.NonSerializableObject();
		out.defaultWriteObject();
	}
	*/
}

