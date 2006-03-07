/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package sample.matrix;

import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.JPPFException;
import org.jppf.server.app.JPPFClient;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.*;
import org.jppf.utils.StringUtils;
import sample.matrix.SecurityTestTask.ExecutionReport;

/**
 * RUnner class for the Mattrix example.
 * @author Laurent Cohen
 */
public class TestTaskRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(TestTaskRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			perform2();
			System.exit(0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	/**
	 * Perform the test.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void perform() throws JPPFException
	{
		try
		{
			int nbIter = 1;
			int nbTasks = 1;
			DataProvider dp = new MemoryMapDataProvider();
			String s = StringUtils.padLeft("", 'x', 1000);
			dp.setValue("dummyValue", s);
			for (int n=0; n<nbIter; n++)
			{
				List<JPPFTask> tasks = new ArrayList<JPPFTask>();
				for (int i=0; i<nbTasks; i++)
				{
					JPPFTask task = new SecurityTestTask();
					tasks.add(task);
				}
				List<JPPFTask> results = jppfClient.submit(tasks, dp);
				JPPFTask task = results.get(0);
				SecurityTestTask t = (SecurityTestTask) task;
				for (ExecutionReport r: t.getReports())
				{
					System.out.println(r.methodName);
					System.out.println(r.description);
					System.out.println(r.stackTrace+"\n");
				}
				if (task.getException() != null) throw task.getException();
			}
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}

	/**
	 * Perform the test.
	 * @throws JPPFException if an error is raised during the execution.
	 */
	static void perform2() throws JPPFException
	{
		try
		{
			//jppfClient.submitAdminRequest(AdminRequestHeader.ADMIN_SHUTDOWN_RESTART, 3000L, 3000L);
			jppfClient.submitAdminRequest(AdminRequestHeader.ADMIN_SHUTDOWN, 3000L, 3000L);
		}
		catch(Exception e)
		{
			throw new JPPFException(e.getMessage(), e);
		}
	}
}
