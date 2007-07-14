/*
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
package org.jppf.process;

import java.util.Properties;

/**
 * Utility class used to create sets of configuration properties for JPPF clients,
 * drivers and nodes, as well as log4j configuration properties.
 * The created properties objects are intended to be stored into temporary files,
 * to which corresponding processes will then refer.
 * @author Laurent Cohen
 */
public class ProcessConfig
{
	/**
	 * The set of configuration properties names for a node.
	 */
	private static final String[] NODE_PROPERTIES =
	{ 
		"jppf.server.host", "class.server.port", "node.server.port", "jppf.policy.file",
		"reconnect.initial.delay", "reconnect.max.time", "reconnect.interval", "processing.threads"
	}; 

	/**
	 * The set of default values for the configuration properties of a node.
	 */
	private static final String[] NODE_DEFAULTS =
	{ 
		"localhost", "11111", "11113", "jppf.policy", "1", "60", "1", "1"
	}; 

	/**
	 * The set of configuration properties names for a driver.
	 */
	private static final String[] DRIVER_PROPERTIES =
	{ 
		"jppf.server.host", "class.server.port", "app.server.port", "node.server.port",
		"max.memory.option", "task.bundle.size", "task.bundle.strategy"
	}; 

	/**
	 * The set of default values for the configuration properties of a driver.
	 */
	private static final String[] DRIVER_DEFAULTS =
	{ 
		"localhost", "11111", "11112", "11113", "128", "5", "autotuned"
	}; 

	/**
	 * The set of configuration properties names for a client.
	 */
	private static final String[] CLIENT_PROPERTIES =
	{
		"jppf.server.host", "class.server.port", "app.server.port",
		"reconnect.initial.delay", "reconnect.max.time", "reconnect.interval"
	}; 

	/**
	 * The set of configuration properties names for a client.
	 */
	private static final String[] CLIENT_DEFAULTS =
	{
		"localhost", "11111", "11112", "1", "60", "1"
	}; 

	/**
	 * Build the set of configuration properties for a node.
	 * @param values the values of the configuration properties to set.
	 * @return the configuration properties as a <code>Properties</code> instance.
	 */
	public static Properties buildNodeConfig(String...values)
	{
		return buildConfig(NODE_PROPERTIES, NODE_DEFAULTS, values);
	}

	/**
	 * Build the set of configuration properties for a client.
	 * @param values the values of the configuration properties to set.
	 * @return the configuration properties as a <code>Properties</code> instance.
	 */
	public static Properties buildClientConfig(String...values)
	{
		return buildConfig(CLIENT_PROPERTIES, CLIENT_DEFAULTS, values);
	}

	/**
	 * Build the set of configuration properties for a driver.
	 * @param values the values of the configuration properties to set.
	 * @return the configuration properties as a <code>Properties</code> instance.
	 */
	public static Properties buildDriverConfig(String...values)
	{
		Properties props = buildConfig(DRIVER_PROPERTIES, DRIVER_DEFAULTS, values);
		props.setProperty("task.bundle.autotuned.strategy", "smooth");
		props.setProperty("strategy.smooth.minSamplesToAnalyse", "500");
		props.setProperty("strategy.smooth.minSamplesToCheckConvergence", "300");
		props.setProperty("strategy.smooth.maxDeviation", "0.2");
		props.setProperty("strategy.smooth.maxGuessToStable", "10");
		props.setProperty("strategy.smooth.sizeRatioDeviation", "1.5");
		props.setProperty("strategy.smooth.decreaseRatio", "0.2");
		props.setProperty("remote.debug.port", "8000");
		props.setProperty("remote.debug.suspend", "false");
		return props;
	}

	/**
	 * Build the set of configuration properties for a process.
	 * @param names the names of the configuration properties to set.
	 * @param defaults the default values to use if the caller does not supply all values.
	 * @param values the values of the configuration properties to set.
	 * @return the configuration properties as a <code>Properties</code> instance.
	 */
	public static Properties buildConfig(String[] names, String[] defaults, String...values)
	{
		Properties props = new Properties();
		if (values == null) values = new String[0];
		int i = 0;
		for (; i<values.length; i++) props.setProperty(names[i], values[i]);
		for (; i<defaults.length; i++) props.setProperty(names[i], defaults[i]);
		return props;
	}

	/**
	 * Build the set of log4j configuration properties for a process.
	 * @param logFile the name of the log file.
	 * @param append determines whether the log file should be re-created or appended to.
	 * @param logLevel the logging level to use.
	 * @return the configuration properties as a <code>Properties</code> instance.
	 */
	public static Properties buildLog4jConfig(String logFile, boolean append, String logLevel)
	{
		Properties props = new Properties();
		props.setProperty("log4j.appender.JPPF", "org.apache.log4j.FileAppender");
		props.setProperty("log4j.appender.JPPF.File", logFile);
		props.setProperty("log4j.appender.JPPF.Append", ""+append);
		props.setProperty("log4j.appender.JPPF.layout", "org.apache.log4j.PatternLayout");
		props.setProperty("log4j.appender.JPPF.layout.ConversionPattern", "%d [%-5p][%c.%M(%L)]: %m\n");
		props.setProperty("log4j.rootLogger", logLevel+", JPPF");
		return props;
	}
}
