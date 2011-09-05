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
package test.classloader;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.ClientDataProvider;
import org.jppf.utils.StringUtils;


/**
 * Runner class for the class version demo.
 * @author Laurent Cohen
 */
public class ClassLoaderTest
{
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.,<br>
	 * The number of times is specified as a configuration property named &quot;matrix.iterations&quot;.<br>
	 * The size of the matrices is specified as a configuration property named &quot;matrix.size&quot;.<br>
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			perform2();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jppfClient != null) jppfClient.close();
		}
	}

	/**
	 * Perform the test.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform() throws Exception
	{
		long totalTime = System.currentTimeMillis();
		JPPFJob job = new JPPFJob();
		job.setId("test class loader");
		job.addTask(new Task1());
		job.addTask(new Task2());
		List<JPPFTask> results = jppfClient.submit(job);
		for (JPPFTask t: results)
		{
			if (t.getException() != null) System.out.println("task error: " +  t.getException().getMessage());
			else System.out.println("task result: " + t.getResult());
		}
		totalTime = System.currentTimeMillis() - totalTime;
		System.out.println("Computation time: " + StringUtils.toStringDuration(totalTime));
	}

	/**
	 * Perform the test.
	 * @throws Exception if an error is raised during the execution.
	 */
	private static void perform2() throws Exception
	{
		JPPFJob job = new JPPFJob();
		job.setId("broadcast");
		job.getJobSLA().setBroadcastJob(true);
		File file = new File("C:/Workspaces/JPPF-b2.5/JPPF/lib/Hazelcast/hazelcast.jar");
		URL url = file.toURI().toURL();
		byte[] bytes = JPPFUtils.makeKey(url);
		ByteKey key = new ByteKey(bytes);
		Map<ByteKey, URL> map = new HashMap<ByteKey, URL>();
		map.put(key, url);
		job.addTask(new JPPFTaskPreInit(map));
		job.setDataProvider(new ClientDataProvider());
		List<JPPFTask> results = jppfClient.submit(job);
		for (JPPFTask t: results)
		{
			if (t.getException() != null) System.out.println("task error: " +  t.getException().getMessage());
			else System.out.println("task result: " + t.getResult());
		}
		JPPFJob job2 = new JPPFJob();
		job2.setId("test class loader");
		job2.addTask(new ClassLoadingTask());
		List<JPPFTask> results2 = jppfClient.submit(job2);
		for (JPPFTask t: results2)
		{
			if (t.getException() != null) System.out.println("task error: " +  t.getException().getMessage());
			else System.out.println("task result: " + t.getResult());
		}
	}
}
