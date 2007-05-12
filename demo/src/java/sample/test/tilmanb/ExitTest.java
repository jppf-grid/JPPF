/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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