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


public class ClassLoaderTest
{
	private static JPPFClient jppfClient = null;

	public static void main(final String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			JPPFJob job = new JPPFJob();
			job.setName("broadcast");
			job.getSLA().setBroadcastJob(true);
			Map<ByteKey, URL> map = processJars("ClassLoaderTest.jar", "../JPPF/lib/Hazelcast/hazelcast.jar");
			job.addTask(new JPPFTaskPreInit(map));
			job.setDataProvider(new ClientDataProvider());
			displayResults("Results for job '" + job.getName() + '\'', jppfClient.submit(job));
			JPPFJob job2 = new JPPFJob();
			job2.setName("test class loader");
			job2.addTask(new ClassLoadingTask());
			displayResults("Results for job '" + job2.getName() + '\'', jppfClient.submit(job2));
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

	private static void displayResults(final String title, final List<JPPFTask> results)
	{
		System.out.println("***** " + title + " *****");
		for (JPPFTask t: results)
		{
			if (t.getException() != null) System.out.println("task error: " +  t.getException().getMessage());
			else System.out.println("task result: " + t.getResult());
		}
	}

	private static Map<ByteKey, URL> processJars(final String...paths) throws Exception
	{
		Map<ByteKey, URL> map = new HashMap<ByteKey, URL>();
		for (String path: paths)
		{
			File file = new File(path);
			URL url = file.toURI().toURL();
			ByteKey key = new ByteKey(JPPFUtils.makeKey(url));
			map.put(key, url);
		}
		return map;
	}
}
