/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package sample.test.tilmanb;

import java.util.*;

import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class ExitTest
{
	/**
	 * 
	 * @param args .
	 */
	public static void main(String[] args)
	{
		try
		{
			new ExitTest().run();
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
	}

	/**
	 * 
	 * @throws Exception .
	 */
	private void run() throws Exception
	{
		JPPFClient client = new JPPFClient();

		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		taskList.add(new MyLongTask("task1", 100));
		taskList.add(new MyLongTask("task2", 200));
		taskList.add(new MyLongTask("task3", 200));
		taskList.add(new MyLongTask("task4", 140));

		System.out.println("Submitting...");
		List<JPPFTask> result = client.submit(taskList, null);
		System.out.println("processing results");
		for (JPPFTask t : result)
		{
			System.out.println(t.getResult());
		}
		System.out.println("closing");
		client.close();
	}
}
