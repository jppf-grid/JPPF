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
package sample.clientdataprovider;

import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.ClientDataProvider;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class DataProviderTestRunner
{
	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		JPPFClient jppfClient = new JPPFClient();
		try
		{
			int nbJobs = 5;
			int nbTasks = 1;
			JPPFJob[] jobs = new JPPFJob[nbJobs];
			for (int i=0; i<nbJobs; i++)
			{
				jobs[i] = new JPPFJob();
				for (int j=1; j<=nbTasks; j++) jobs[i].addTask(new DataProviderTestTask(i+1, j));
				jobs[i].setDataProvider(new ClientDataProvider());
				jobs[i].setName("job " + (i+1));
				jobs[i].setBlocking(false);
				jobs[i].setResultListener(new JPPFResultCollector(nbTasks));
			}
			for (int i=0; i<nbJobs; i++)
			{
				jppfClient.submit(jobs[i]);
			}
			for (int i=0; i<nbJobs; i++)
			{
				JPPFResultCollector collector = (JPPFResultCollector) jobs[i].getResultListener();
				List<JPPFTask> results = collector.waitForResults();
				for (JPPFTask task: results)
				{
					DataProviderTestTask t = (DataProviderTestTask) task;
					if (t.getException() != null) throw t.getException();
					else System.out.println("job #" + t.i +" task #" + t.j + " : result: " + t.getResult());
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jppfClient.close();
		}
		System.exit(0);
	}
}
