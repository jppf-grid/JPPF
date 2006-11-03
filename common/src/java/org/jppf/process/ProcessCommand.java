/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.process;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.utils.JPPFConfiguration;

/**
 * 
 * @author Laurent Cohen
 */
public class ProcessCommand
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(ProcessCommand.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * 
	 * @param mainClass the fully qualified name of the entry point class for the process to start.
	 * @param jppfConfig the set of JPPF configuration properties.
	 * @param log4jConfig the set of log4j configuration properties.
	 * @param maxMem the maximum heap size for the process, in megabytes.
	 * @return a <code>ProcessWrapper</code> instance encapsualting the started process.
	 * @throws Exception if the process failed to start.
	 */
	public static ProcessWrapper buildProcess(String mainClass, Properties jppfConfig,
		Properties log4jConfig, int maxMem) throws Exception
	{
		List<String> command = new ArrayList<String>();
		command.add(System.getProperty("java.home")+"/bin/java");
		command.add("-cp");
		// add "." to the classpath so the process can access the log4j configuration file
		command.add("."+System.getProperty("path.separator")+System.getProperty("java.class.path"));
		command.add("-D"+JPPFConfiguration.CONFIG_PROPERTY+"="+createTempFile(jppfConfig));
		command.add("-Dlog4j.configuration="+createTempFile(log4jConfig));
		command.add("-server");
		command.add("-Xmx" + maxMem + "m");
		command.add(mainClass);
		if (debugEnabled)
		{
			StringBuilder sb = new StringBuilder("process command:\n");
			for (String s: command) sb.append(s).append("\n");
			log.debug(sb.toString());
		}
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.directory(new File(System.getProperty("user.dir")));
		Process p = builder.start();
		return new ProcessWrapper(p);
	}

	/**
	 * Create a temporary file holding the properties in a <code>Properties</code> object.
	 * @param props the properties to write into the file.
	 * @return the path to the temporary file created.
	 * @throws Exception if an error occurs while creating the temporary file.
	 */
	public static String createTempFile(Properties props) throws Exception
	{
		File tempDir = new File("./temp");
		tempDir.mkdir();
		File file = File.createTempFile("jppf-", ".properties", tempDir);
		file.deleteOnExit();
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		props.store(os, "JPPF generated temp file");
		os.close();
		return file.getPath().replaceAll("\\\\", "/");
	}
}
