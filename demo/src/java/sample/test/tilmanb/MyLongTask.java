/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
			s = s.replace("8", "$");
		}
		setResult("task '"+name+"' executed in "+duration+" ms");
	}
}
