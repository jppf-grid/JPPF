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
package test.jobfromtask;

import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.StringUtils;

/**
 * Instances of this class are defined as tasks with a predefined execution length, specified at their creation. 
 * @author Laurent Cohen
 */
public class SourceTask extends JPPFTask
{
	/**
	 * Initialize this task.
	 */
	public SourceTask()
	{
	}

	/**
	 * Perform the execution of this task.
	 * @see sample.BaseDemoTask#doWork()
	 */
	@Override
    public void run()
	{
		System.out.println("Starting source task '" + getId() + '\'');
		print("starting JPPF client");
		JPPFClient client = new JPPFClient();
		try
		{
			long start = System.currentTimeMillis();
			print("creating destination job");
			JPPFJob job = new JPPFJob();
			job.setName("Destination job");
			job.getSLA().setMaxNodes(1);
			DestinationTask task = new DestinationTask();
			task.setId("destination");
			job.addTask(task);
			print("submitting job");
			List<JPPFTask> results = client.submit(job);
			print("got job results");
			for (JPPFTask t: results)
			{
				Exception e = t.getException();
				if (e != null) throw e;
				else print("destination task result: " + t.getResult());
			}
			long elapsed = System.currentTimeMillis() - start;
			print("processing  performed in "+StringUtils.toStringDuration(elapsed));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			print("closing JPPF client");
			client.close();
		}
	}

	/**
	 * Called when this task is cancelled.
	 * @see org.jppf.server.protocol.JPPFTask#onCancel()
	 */
	@Override
    public void onCancel()
	{
		String s = "task '" + getId() + "' has been cancelled";
		setResult(s);
		print(s);
	}

	/**
	 * Print a message to the log and to the console.
	 * @param msg the message to print.
	 */
	private static void print(String msg)
	{
		//log.info(msg);
		System.out.println(msg);
	}
}
