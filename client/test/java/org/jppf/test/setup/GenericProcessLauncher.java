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

package org.jppf.test.setup;

import java.io.*;
import java.util.*;

import org.jppf.process.ProcessWrapper;

/**
 * Super class for launching a JPPF driver or node.
 * @author Laurent Cohen
 */
public class GenericProcessLauncher
{
	/**
	 * System path separator.
	 */
	public static final String PATH_SEPARATOR = System.getProperty("path.separator");
	/**
	 * Default directory.
	 */
	public static final String DEFAULT_DIR = System.getProperty("user.dir");
	/**
	 * List of files to have in the classpath.
	 */
	private List<String> classpath = new ArrayList<String>();
	/**
	 * The JVM options to set.
	 */
	private List<String> jvmOptions = new ArrayList<String>();
	/**
	 * Path to the JPPF configuration file.
	 */
	private String jppfConfig = null;
	/**
	 * The program arguments.
	 */
	private List<String> arguments = new ArrayList<String>();
	/**
	 * Path to the commons-logging configuration file.
	 */
	private String comonsLogging = "config/commons-logging.properties";
	/**
	 * Path to the log4j configuration file.
	 */
	private String log4j = null;
	/**
	 * Directory in which the program is started.
	 */
	private String dir = DEFAULT_DIR;
	/**
	 * The process started by this process launcher.
	 */
	private Process process = null;
	/**
	 * Wrapper around the process.
	 */
	private ProcessWrapper wrapper = null;
	/**
	 * Fully qualifie name of the main class.
	 */
	private String mainClass = null;

	/**
	 * Default constructor.
	 */
	public GenericProcessLauncher()
	{
		addClasspathElement("../node/classes");
		String libDir = "../JPPF/lib/";
		addClasspathElement(libDir + "slf4j/jcl-over-slf4j-1.6.1.jar");
		addClasspathElement(libDir + "slf4j/slf4j-api-1.6.1.jar");
		addClasspathElement(libDir + "slf4j/slf4j-log4j12-1.6.1.jar");
		addClasspathElement(libDir + "log4j/log4j-1.2.15.jar");
	}

	/**
	 * Get the path to the JPPF configuration file.
	 * @return the path as a string.
	 */
	public String getJppfConfig()
	{
		return jppfConfig;
	}

	/**
	 * Set the path to the JPPF configuration file.
	 * @param jppfConfig the path as a string.
	 */
	public void setJppfConfig(String jppfConfig)
	{
		this.jppfConfig = jppfConfig;
	}

	/**
	 * Get the path to the commons-logging configuration file.
	 * @return the path as a string.
	 */
	public String getComonsLogging()
	{
		return comonsLogging;
	}

	/**
	 * Set the path to the commons-logging configuration file.
	 * @param comonsLogging the path as a string.
	 */
	public void setComonsLogging(String comonsLogging)
	{
		this.comonsLogging = comonsLogging;
	}

	/**
	 * Get the path to the log4j configuration file.
	 * @return the path as a string.
	 */
	public String getLog4j()
	{
		return log4j;
	}

	/**
	 * Set the path to the log4j configuration file.
	 * @param log4j the path as a string.
	 */
	public void setLog4j(String log4j)
	{
		this.log4j = log4j;
	}

	/**
	 * Get the directory in which the program runs.
	 * @return the directory as a string.
	 */
	public String getDir()
	{
		return dir;
	}

	/**
	 * Set the directory in which the program runs.
	 * @param dir the directory as a string.
	 */
	public void setDir(String dir)
	{
		this.dir = dir;
	}

	/**
	 * Get the main class.
	 * @return the main class as a string.
	 */
	public String getMainClass()
	{
		return mainClass;
	}

	/**
	 * Set the main class.
	 * @param mainClass the main class as a string.
	 */
	public void setMainClass(String mainClass)
	{
		this.mainClass = mainClass;
	}

	/**
	 * Add an element (jar or folder) to the classpath.
	 * @param element the classpath element to add.
	 */
	public void addClasspathElement(String element)
	{
		classpath.add(element);
	}

	/**
	 * Add a JVM option (including system property definitions).
	 * @param option the option to add.
	 */
	public void addJvmOption(String option)
	{
		jvmOptions.add(option);
	}

	/**
	 * Add a program argument.
	 * @param arg the argument to add.
	 */
	public void addArgument(String arg)
	{
		arguments.add(arg);
	}

	/**
	 * Start the process.
	 * @throws IOException if the process fails to start.
	 */
	public void startProcess() throws IOException
	{
		List<String> command = new ArrayList<String>();
		command.add(System.getProperty("java.home")+"/bin/java");
		command.add("-cp");
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<classpath.size(); i++)
		{
			if (i > 0) sb.append(PATH_SEPARATOR);
			sb.append(classpath.get(i));
		}
		command.add(sb.toString());
		command.addAll(jvmOptions);
		command.add("-Djppf.config=" + jppfConfig);
		command.add("-Dlog4j.configuration=" + log4j);
		command.add("-Dorg.apache.commons.logging.Log=org.apache.commons.logging.impl.Log4JLogger");
		command.add(mainClass);
		command.addAll(arguments);
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(command);
		if (dir != null) builder.directory(new File(dir));
		wrapper = new ProcessWrapper();
		wrapper.setProcess(builder.start());
	}

	/**
	 * Stop the process.
	 */
	public void stopProcess()
	{
		if ((wrapper != null) && (wrapper.getProcess() != null)) wrapper.getProcess().destroy();
	}
}
