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

package sample.test.tilmanb;

import java.util.Random;

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class MyLongTask extends JPPFTask
{
	/**
	 * 
	 */
	private String name = null;
	/**
	 * 
	 */
	private long duration = 0L;

	/**
	 * Iniitlaize this task with a specified name and duration.
	 * @param name the name of this task.
	 * @param duration the duration of this task.
	 */
	public MyLongTask(String name, long duration)
	{
		this.name = name;
		this.duration = duration;
	}

	/**
	 * 
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		long taskStart = System.currentTimeMillis();
		while (System.currentTimeMillis() - taskStart < duration)
		{
			Random rand = new Random(System.currentTimeMillis());
			String s = "";
			for (int i=0; i<100; i++) s += "A"+rand.nextInt(10);
			s.replace("8", "$");
		}
		setResult("task '"+name+"' executed in "+duration+" ms");
	}
}
