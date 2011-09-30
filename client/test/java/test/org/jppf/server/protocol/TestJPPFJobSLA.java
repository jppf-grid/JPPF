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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.JPPFException;
import org.jppf.client.JPPFJob;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.server.protocol.*;
import org.junit.Test;

import test.org.jppf.test.setup.*;

/**
 * Unit tests for {@link JPPFJobSLA}.
 * In this class, we test that the behavior is the expected one, from the client point of view,
 * as specified in the job SLA. 
 * @author Laurent Cohen
 */
public class TestJPPFJobSLA extends Setup1D1N1C
{
	/**
	 * Count of the number of jobs created.
	 */
	private static AtomicInteger jobCount = new AtomicInteger(0);
	/**
	 * A "short" duration for this test.
	 */
	private static final long TIME_SHORT = 1000L;
	/**
	 * A "long" duration for this test.
	 */
	private static final long TIME_LONG = 5000L;
	/**
	 * A "rest" duration for this test.
	 */
	private static final long TIME_REST = 1L;
	/**
	 * A the date format used in the tests.
	 */
	private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

	/**
	 * Simply test that a job does expires at a specified date.
	 * @throws Exception if any error occurs.
	 */
	@Test
	public void testJobExpirationAtDate() throws Exception
	{
		JPPFJob job = createJob("testJobExpirationAtDate", 1, TIME_LONG);
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		Date date = new Date(System.currentTimeMillis() + TIME_SHORT);
		job.getJobSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
		List<JPPFTask> results = client.submit(job);
		assertNotNull(results);
		assertEquals(results.size(), 1);
		JPPFTask task = results.get(0);
		assertNull(task.getResult());
	}

	/**
	 * Test that a job does not expires at a specified date, because it completes before that date.
	 * @throws Exception if any error occurs.
	 */
	@Test
	public void testJobExpirationAtDateTooLate() throws Exception
	{
		JPPFJob job = createJob("testJobExpirationAtDateTooLate", 1, TIME_SHORT);
		SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
		Date date = new Date(System.currentTimeMillis() + TIME_LONG);
		job.getJobSLA().setJobExpirationSchedule(new JPPFSchedule(sdf.format(date), DATE_FORMAT));
		List<JPPFTask> results = client.submit(job);
		assertNotNull(results);
		assertEquals(results.size(), 1);
		JPPFTask task = results.get(0);
		assertNotNull(task.getResult());
		assertEquals(BaseSetup.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
	}

	/**
	 * Simply test that a job does expires after a specified delay.
	 * @throws Exception if any error occurs.
	 */
	@Test
	public void testJobExpirationAfterDelay() throws Exception
	{
		JPPFJob job = createJob("testJobExpirationAfterDelay", 1, TIME_LONG);
		job.getJobSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
		List<JPPFTask> results = client.submit(job);
		assertNotNull(results);
		assertEquals(results.size(), 1);
		JPPFTask task = results.get(0);
		assertNull(task.getResult());
	}

	/**
	 * Test that a job does not expire after a specified delay, because it completes before that.
	 * @throws Exception if any error occurs.
	 */
	@Test
	public void testJobExpirationAfterDelayTooLate() throws Exception
	{
		JPPFJob job = createJob("testJobExpirationAfterDelayTooLate", 1, TIME_SHORT);
		job.getJobSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_LONG));
		List<JPPFTask> results = client.submit(job);
		assertNotNull(results);
		assertEquals(results.size(), 1);
		JPPFTask task = results.get(0);
		assertNotNull(task.getResult());
		assertEquals(BaseSetup.EXECUTION_SUCCESSFUL_MESSAGE, task.getResult());
	}

	/**
	 * Simply test that a job does expires after a specified delay.
	 * @throws Exception if any error occurs.
	 */
	@Test(timeout=5000)
	public void testSuspendedJobExpiration() throws Exception
	{
		JPPFJob job = createJob("testJobExpirationAfterDelay", 1, TIME_LONG);
		job.getJobSLA().setSuspended(true);
		job.getJobSLA().setJobExpirationSchedule(new JPPFSchedule(TIME_SHORT));
		List<JPPFTask> results = client.submit(job);
		assertNotNull(results);
		assertEquals(results.size(), 1);
		JPPFTask task = results.get(0);
		assertNull(task.getResult());
	}

	/**
	 * Create a blocking job with the specified number of tasks, each with the specified duration.
	 * @param id the job id.
	 * @param nbTasks the number of tasks in the job.
	 * @param duration the duration of each task.
	 * @return a {@link JPPFJob} instance.
	 * @throws JPPFException if an error occurs while creating the job.
	 */
	protected synchronized JPPFJob createJob(String id, int nbTasks, long duration) throws JPPFException
	{
		JPPFJob job = new JPPFJob();
		job.setId(id + '(' + jobCount.incrementAndGet() + ')');
		for (int i=0; i<nbTasks; i++)
		{
			JPPFTask task = new SimpleTask(duration);
			task.setId(job.getId()  + " - task " + (i+1));
			job.addTask(task);
		}
		return job;
	}
}
