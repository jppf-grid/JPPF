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
package test.client.deadlock;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;


/**
 * Runner class for testing bug [3348381 - Client deadlock with asynchronous jobs].
 * @author Laurent Cohen
 */
public class DeadlockRunner
{
	/**
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(DeadlockRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * 
	 */
	private static ExecutorService executor = Executors.newFixedThreadPool(4, new JPPFThreadFactory("TestDeadlock"));

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			print("starting client ...");
			TypedProperties config = JPPFConfiguration.getProperties();
			config.setProperty("jppf.pool.size", "5");
			config.setProperty("jppf.discovery.enabled", "true");
			jppfClient = new JPPFClient();
			//jppfClient.setLocalExecutionEnabled(true);
			//perform(2000, 1, 1L, 0);
			perform(2000, 2000, 1L, 0);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jppfClient.close();
			executor.shutdownNow();
		}
	}
	
	/**
	 * Perform the test using <code>JPPFClient.submit(JPPFJob)</code> to submit the tasks.
	 * @param nbJobs the number of non-blocking jobs to submit.
	 * @param nbTasks the number of tasks per job.
	 * @param millis the execution length of each task (millis part).
	 * @param nanos the execution length of each task (nanos part).
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform(int nbJobs, int nbTasks, long millis, int nanos) throws Exception
	{
		print("submitting the jobs");
		List<Future<SubmitJob>> futures = new ArrayList<Future<SubmitJob>>();
		for (int i=0; i<nbJobs; i++)
		{
			SubmitJob sj = new SubmitJob(i+1, nbTasks, millis, nanos);
			futures.add(executor.submit(sj, sj));
		}
		long start = System.nanoTime();
		print("waiting for the results");
		int count = 0;
		for (Future<SubmitJob> f: futures)
		{
			count++;
			SubmitJob sj = f.get();
			JPPFResultCollector collector = (JPPFResultCollector) sj.job.getResultListener();
			collector.waitForResults();
			if (count % 100 == 0) print("got " + count + " job results");
		}
		long elapsed = System.nanoTime() - start;
		print("ran " + nbJobs + " jobs in: " + StringUtils.toStringDuration(elapsed/1000000));
	}

	/**
	 * Print a message tot he log and to the console.
	 * @param msg the message to print.
	 */
	private static void print(String msg)
	{
		log.info(msg);
		System.out.println(msg);
	}

	/**
	 * The tasks to run on the grid.
	 */
	public static class Task extends JPPFTask
	{
		/**
		 * Millis part of the wait.
		 */
		private long millis = 1L;
		/**
		 * Nanos part of the wait.
		 */
		private int nanos = 0;

		/**
		 * Initialize this task.
		 * @param millis millis part of the wait.
		 */
		public Task(long millis)
		{
			this.millis = millis;
		}

		/**
		 * Initialize this task.
		 * @param millis millis part of the wait.
		 * @param nanos nanos part of the wait.
		 */
		public Task(long millis, int nanos)
		{
			this.millis = millis;
			this.nanos = nanos;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
        public void run()
		{
			try
			{
				Thread.sleep(millis, nanos);
				setResult("ok");
			}
			catch (Exception e)
			{
				setException(e);
				setResult("ko");
			}
		}
	}

	/**
	 * This task submits a job.
	 */
	private static class SubmitJob implements Runnable
	{
		/**
		 * The id for the job.
		 */
		private int jobNumber = 0;
		/**
		 * Number of tasks in the job.
		 */
		private int nbTasks = 0;
		/**
		 * The length of each task.
		 */
		private long length = 1L;
		/**
		 * The length of each task.
		 */
		private int nanos = 0;
		/**
		 * The submitted job.
		 */
		public JPPFJob job = null;

		/**
		 * Intiialize this tasks.
		 * @param jobNumber the id for the job.
		 * @param nbTasks number of tasks in the job.
		 * @param millis millis part of the wait.
		 * @param nanos nanos part of the wait.
		 */
		public SubmitJob(int jobNumber, int nbTasks, long millis, int nanos)
		{
			this.jobNumber = jobNumber;
			this.nbTasks = nbTasks;
			this.length = millis;
			this.nanos = nanos;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
        public void run()
		{
			try
			{
				job = new JPPFJob();
				job.setId("job " + jobNumber);
				job.setBlocking(false);
				job.setResultListener(new JPPFResultCollector(nbTasks));
				for (int j=0; j<nbTasks; j++)
				{
					JPPFTask task = new Task(length, nanos);
					task.setId("task " + jobNumber + ':' + j);
					job.addTask(task);
				}
				jppfClient.submit(job);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
