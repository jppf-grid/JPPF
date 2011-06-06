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

package sample.helloworld;

import java.io.*;
import java.util.List;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;

/**
 * Runner for the hello world application.
 * @author Laurent Cohen
 */
public class HelloWorldRunner
{
	/**
	 * Entry point.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			JPPFClient client = new JPPFClient();
			JPPFJob job = new JPPFJob();
			job.addTask(new HelloWorld());
			job.addTask(new HelloWorldAnnotated(), "hello message", 1);
			job.addTask(HelloWorldAnnotatedStatic.class, "hello message", 2);
			job.addTask(HelloWorldAnnotatedConstructor.class, "hello message", 3);
			job.addTask("helloPojoMethod", new HelloWorldPojo(), "hello message", 4);
			job.addTask("helloPojoStaticMethod", HelloWorldPojoStatic.class, "hello message", 5);
			job.addTask("HelloWorldPojoConstructor", HelloWorldPojoConstructor.class, "hello message", 6);
			job.addTask(new HelloWorldRunnable());
			job.addTask(new HelloWorldCallable());
			List<JPPFTask> results = client.submit(job);
			System.out.println("********** Results: **********");
			for (JPPFTask task: results)
			{
				if (task.getException() != null)
				{
					StringWriter sw = new StringWriter();
					task.getException().printStackTrace(new PrintWriter(sw));
					System.out.println(sw.toString());
				}
				else System.out.println("" + task.getResult());
			}
			client.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}
}
