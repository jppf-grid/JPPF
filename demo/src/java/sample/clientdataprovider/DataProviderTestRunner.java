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
			for (int i=1; i<=100; i++)
			{
				JPPFJob job = new JPPFJob();
				for (int j=1; j<=2; j++) job.addTask(new DataProviderTestTask(i, j));
				job.setDataProvider(new ClientDataProvider());
				List<JPPFTask> results = jppfClient.submit(job);
				for (JPPFTask task: results)
				{
					DataProviderTestTask t = (DataProviderTestTask) task;
					if (t.getException() != null) throw t.getException();
					else System.out.println("iteration #" + t.i +" task #" + t.j + " : result: " + t.getResult());
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
