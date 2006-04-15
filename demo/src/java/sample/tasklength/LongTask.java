/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package sample.tasklength;

import java.util.Random;
import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation. 
 * @author Laurent Cohen
 */
public class LongTask extends JPPFTask
{
	/**
	 * Determines how long this task will run.
	 */
	private long taskLength = 0L;
	/**
	 * Timestamp marking the time when the task execution starts.
	 */
	private long taskStart = 0L;

	/**
	 * Initialize this task with a predefined length of time, in milliseconds, during which it will run.
	 * @param taskLength determines how long this task will run.
	 */
	public LongTask(long taskLength)
	{
		this.taskLength = taskLength;
	}

	/**
	 * Perform the excution of this task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		taskStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - taskStart < taskLength)
		{
			Random rand = new Random(System.currentTimeMillis());
			String s = "";
			for (int i=0; i<100; i++) s += "A"+rand.nextInt(10);
			s.replace("8", "$");
		}
	}
}
