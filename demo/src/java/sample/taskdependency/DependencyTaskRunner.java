/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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
package sample.taskdependency;

import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.server.protocol.*;
import org.jppf.server.protocol.commandline.*;
import org.jppf.utils.StringUtils;

/**
 * Runner class used for testing the framework.
 * @author Laurent Cohen
 */
public class DependencyTaskRunner
{
	/**
	 * Log4j logger for this class.
	 */
	static Log log = LogFactory.getLog(DependencyTaskRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Separator for each test.
	 */
	private static String banner = "\n"+StringUtils.padLeft("", '-', 80)+"\n";

	/**
	 * Entry point for this class, performs a matrix multiplication a number of times.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			jppfClient = new JPPFClient();
			performCommand();
			jppfClient.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * .
	 * @throws Exception .
	 */
	private static void performDependency() throws Exception
	{
		List<JPPFTask> list = new ArrayList<JPPFTask>();
		list.add(new DependencyTask("Hello 1"));
		List<JPPFTask> results = jppfClient.submit(list, null);
		List<JPPFTask> list2 = new ArrayList<JPPFTask>();
		for (JPPFTask task: results)
		{
			DependencyTask dt = (DependencyTask) task;
			if (dt.getNextTask() != null) list2.add(dt);
		}
		jppfClient.submit(list2, null);
	}

	/**
	 * .
	 * @throws Exception .
	 */
	private static void performCommand() throws Exception
	{
		List<JPPFTask> list = new ArrayList<JPPFTask>();
		String dir = System.getProperty("user.dir");
		String remoteDir = "D:/temp";
		CommandLineTask cmdTask = new CommandLineTask(null, remoteDir,
			new String[] {remoteDir + "/" + "testtask_cmd.cmd"});

		String inName = "testtask_input.txt";
		String outName = "testtask_output.txt";
		ExternalArtifact input = new ExternalArtifact(new Location(Location.FILE, dir + "/" + inName),
				new Location(Location.FILE, remoteDir + "/" + inName));
		ExternalArtifact output = new ExternalArtifact(new Location(Location.FILE, dir + "/" + outName),
				new Location(Location.FILE, remoteDir + "/" + outName));
		cmdTask.addInputArtifact(input);
		cmdTask.addOutputArtifact(output);

		JPPFCallback preCallback = new JPPFCallback()
		{
			public void run()
			{
				System.out.println("executing the PRE-processing callback");
			}
		};
		JPPFCallback postCallback = new JPPFCallback()
		{
			public void run()
			{
				System.out.println("executing the POST-processing callback");
			}
		};
		cmdTask.addPreProcessingCallback(preCallback);
		cmdTask.addPostProcessingCallback(postCallback);

		cmdTask.setCaptureOutput(true);
		cmdTask.fetchInputContent();
		list.add(cmdTask);
		List<JPPFTask> results = jppfClient.submit(list, null);
		for (JPPFTask task: results)
		{
			System.out.println("std output: " + ((CommandLineTask) task).getStandardOutput());
			System.out.println("err output: " + ((CommandLineTask) task).getErrorOutput());
			if (task.getException() != null) throw task.getException();
			System.out.println("task executed successfully");
			((CommandLineTask) task).writeOutputContent();
		}
	}
}
