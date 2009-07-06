/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package test.client.nissalia;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestMatrixParallel2
{

	static final int NB_REPETITIONS = 10;

	static Log log = LogFactory.getLog(TestMatrixParallel2.class);

	private static JPPFClient jppfClient = null;

	public static void main(String... args)
	{
		try
		{
			jppfClient = new JPPFClient();
			perform();
			jppfClient.close();
			System.exit(0);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			output("before exit(1)");
			System.exit(1);
		}
	}

	private static void perform() throws Exception
	{
		long time = -System.currentTimeMillis();
		DataProvider dataProvider = new MemoryMapDataProvider();
		JPPFJob job = new JPPFJob(dataProvider);

		for (int i=0; i<NB_REPETITIONS; i++) job.addTask(new TestMatrixTask2(i));

// submit the tasks for execution
		JPPFResultCollector collector = new JPPFResultCollector(job.getTasks().size());
		job.setResultListener(collector);
		job.setBlocking(false);
		jppfClient.submit(job);
		List<JPPFTask> results = collector.waitForResults();

// List<JPPFTask> results = jppfClient.submit(tasks, dataProvider);
		for (int i = 0; i < results.size(); i++)
		{
			TestMatrixTask2 tmTask = (TestMatrixTask2) results.get(i);
			if (tmTask.getException() != null) throw tmTask.getException();
			int res = (Integer) tmTask.getResult();
			output(res + " done");
		}

		time += System.currentTimeMillis();
		output("temps d'execution : " + time);
	}

	private static void output(String message)
	{
		System.out.println(message);
		log.info(message);
	}
}
