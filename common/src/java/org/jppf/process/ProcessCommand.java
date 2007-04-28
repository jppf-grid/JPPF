/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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
import org.jppf.process.NodePropertiesBuilder.NodePermission;
import org.jppf.utils.*;

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
		command.add("-D"+JPPFConfiguration.CONFIG_PROPERTY+"="+createTempFile(jppfConfig, null)+"");
		command.add("-Dlog4j.configuration="+createTempFile(log4jConfig, null)+"");
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
	 * Buidl and start a node process.
	 * @param mainClass the fully qualified name of the entry point class for the process to start.
	 * @param jppfConfig the set of JPPF configuration properties.
	 * @param permissions list of permissions granted to the node.
	 * @param log4jConfig the set of log4j configuration properties.
	 * @param maxMem the maximum heap size for the process, in megabytes.
	 * @return a <code>ProcessWrapper</code> instance encapsulating the started process.
	 * @throws Exception if the process failed to start.
	 */
	public static ProcessWrapper buildNodeProcess(String mainClass, Properties jppfConfig,
			List<NodePermission> permissions, Properties log4jConfig, int maxMem) throws Exception
	{
		if (permissions != null)
		{
			StringBuilder sb = new StringBuilder();
			for (NodePermission p: permissions)
			{
				sb.append(p.toString()).append("\n");
			}
			String policyFile = createTempFile("jppf-", "policy", null, sb.toString());
			jppfConfig.setProperty("jppf.policy.file", policyFile);
		}
		return buildProcess(mainClass, jppfConfig, log4jConfig, maxMem);
	}

	/**
	 * Create a temporary file holding the properties in a <code>Properties</code> object.
	 * @param props the properties to write into the file.
	 * @param dir path of the directory to create the file into.
	 * @return the path to the temporary file created.
	 * @throws Exception if an error occurs while creating the temporary file.
	 */
	public static String createTempFile(Properties props, String dir) throws Exception
	{
		File tempDir = new File("./jppftemp");
		if (!tempDir.exists()) tempDir.mkdirs();
		File file = File.createTempFile("jppf-", ".properties", tempDir);
		file.deleteOnExit();
		OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
		props.store(os, "JPPF generated temp file");
		os.close();
		return file.getPath();
	}

	/**
	 * Create a temporary file the content of a string.
	 * @param prefix prefix to use for the file name.
	 * @param ext the file name extension.
	 * @param dir path of the directory to create the file into.
	 * @param content the content to write into the file.
	 * @return the path to the temporary file created.
	 * @throws Exception if an error occurs while creating the temporary file.
	 */
	public static String createTempFile(String prefix, String ext, String dir, String content) throws Exception
	{
		File tempDir = new File("./jppftemp");
		if (!tempDir.exists()) tempDir.mkdirs();
		File file = File.createTempFile(prefix, "." + ext, tempDir);
		file.deleteOnExit();
		String filename = file.getPath();
		FileUtils.writeTextFile(filename, content);
		return filename;
	}
}
