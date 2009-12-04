/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.process;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.commons.logging.*;
import org.jppf.process.event.*;
import org.jppf.utils.*;

/**
 * <p>This class is intended as a controller for a subprocess, to enable stopping and restarting it when requested.
 * <p>It performs the following operations:
 * <ul>
 * <li>open a server socket the driver will listen to (port number is dynamically attributed)</li>
 * <li>Start the subprocess, sending the server socket port number as an argument</li>
 * <li>Wait for the subprocess to exit</li>
 * <li>If the subprocess exit code is equal to 2, the subprocess is restarted, otherwise this process exits as well</li>
 * </ul>
 * @author Laurent Cohen
 */
public class ProcessLauncher
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(ProcessLauncher.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * A reference to the JPPF driver subprocess, used to kill it when the driver launcher exits.
	 */
	private Process process = null;
	/**
	 * The server socket the driver listens to.
	 */
	private ServerSocket processServer = null;
	/**
	 * The prot number the erver socket listens to.
	 */ 
	private int processPort = 0;
	/**
	 * The fully qualified name of the main class of the subprocess to launch.
	 */
	private String mainClass = null;

	/**
	 * Initialize this process launcher.
	 * @param mainClass the fully qualified name of the main class of the sub process to launch. 
	 */
	public ProcessLauncher(String mainClass)
	{
		this.mainClass = mainClass;
	}

	/**
	 * Start the socket listener and the subprocess.
	 */
	public void start()
	{
		boolean end = false;
		try
		{
			createShutdownHook();
			startDriverSocket();
			while (!end)
			{
				process = buildProcess();
				ProcessWrapper wrapper = createProcessWrapper(process);
				if (debugEnabled) log.debug("started driver process [" + process + "]");
				int n = process.waitFor();
				end = onProcessExit(n);
				process.destroy();
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Start the JPPF driver subprocess.
	 * @return A reference to the Process object representing the JPPF driver suprocess.
	 * @throws Exception if the process failed to start.
	 */
	public Process buildProcess() throws Exception
	{
		List<String> command = new ArrayList<String>();
		command.add(System.getProperty("java.home")+"/bin/java");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		command.add("-D"+JPPFConfiguration.CONFIG_PROPERTY+"="+System.getProperty(JPPFConfiguration.CONFIG_PROPERTY));
		command.add("-Dlog4j.configuration="+System.getProperty("log4j.configuration"));
		//command.add("-server");
		int n = JPPFConfiguration.getProperties().getInt("max.memory.option", 128);
		command.add("-Xmx" + n + "m");
		String s = JPPFConfiguration.getProperties().getString("other.jvm.options");
		if (s != null)
		{
			String[] options = s.split("\\s");
			for (String opt: options) command.add(opt);
		}

		TypedProperties props = JPPFConfiguration.getProperties();
		if (props.getBoolean("remote.debug.enabled", false))
		{
			int debugPort = props.getInt("remote.debug.port", 8000);
			boolean b = props.getBoolean("remote.debug.suspend", false);
			command.add("-Xdebug");
			command.add("-Xrunjdwp:transport=dt_socket,address=localhost:"+debugPort+",server=y,suspend="+(b?"y":"n"));
		}

		command.add(mainClass);
		command.add("" + processPort);
		if (debugEnabled) log.debug("process command:\n" + command);
		ProcessBuilder builder = new ProcessBuilder(command);
		return builder.start();
	}

	/**
	 * Create a process wrapper around the specified process, to capture its console output
	 * and prevent it from blocking.
	 * @param p the process whose output is to be captured.
	 * @return a <code>ProcessWrapper</code> instance.
	 */
	public ProcessWrapper createProcessWrapper(Process p)
	{
		ProcessWrapper wrapper = new ProcessWrapper(process);
		wrapper.addProcessWrapperEventListener(new ProcessWrapperEventListener()
		{
			public void errorStreamAltered(ProcessWrapperEvent event)
			{
				System.err.println(event.getContent());
			}
			public void outputStreamAltered(ProcessWrapperEvent event)
			{
				System.out.println(event.getContent());
			}
		});
		return wrapper;
	}

	/**
	 * Called when the subprocess has exited with exit value n.
	 * This allows for printing the residual output (both standard and error) to this pJVM's console and log file,
	 * in order to get additional information if a problem occurred.
	 * @param n the exit value of the subprocess.
	 * @return true if this launcher is to be terminated, false if it should re-launch the subprocess.
	 */
	public boolean onProcessExit(int n)
	{
		String s = getOutput(process, "std").trim();
		if (s.length() > 0)
		{
			System.out.println("\nstandard output:\n" + s);
			log.info("standard output:\n" + s);
		}
		s = getOutput(process, "err").trim();
		if (s.length() > 0)
		{
			System.out.println("\nerror output:\n" + s);
			log.info("error output:\n" + s);
		}
		return n != 2;
	}

	/**
	 * Create a shutdown hook that is run when this JVM terminates.<br>
	 * This is normally used to ensure the subprocess is terminated as well.
	 */
	protected void createShutdownHook()
	{
		Runnable hook = new Runnable()
		{
			public void run()
			{
				if (process != null) process.destroy();
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(hook));
	}

	/**
	 * Start a server socket that will accept one connection at a time with the JPPF driver, so the server can shtutdown properly,
	 * when this driver is killed, by a way other than the API (ie CTRL-C or killing the process through the OS shell).<br>
	 * The port the server socket listens to is dynamically attributed, which is obtained by using the constructor
	 * <code>new ServerSocket(0)</code>.<br>
	 * The driver will connect and listen to this port, and exit when the connection is broken.<br>
	 * The single connection at a time is obtained by doing the <code>ServerSocket.accept()</code> and the
	 * <code>Socket.getInputStream().read()</code> in the same thread.
	 * @return the port number on which the server socket is listening.
	 */
	protected int startDriverSocket()
	{
		try
		{
			processServer = new ServerSocket(0);
			processPort = processServer.getLocalPort();
			Runnable r = new Runnable()
			{
				public void run()
				{
					while (true)
					{
						try
						{
							Socket s = processServer.accept();
							s.getInputStream().read();
						}
						catch(IOException ioe)
						{
							if (debugEnabled) log.debug(ioe.getMessage(), ioe);
						}
					}
				}
			};
			new Thread(r).start();
		}
		catch(Exception e)
		{
			try
			{
				processServer.close();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
				if (debugEnabled) log.debug(ioe.getMessage(), ioe);
				System.exit(1);
			}
		}
		return processPort;
	}
	
	/**
	 * Get the output of the driver process.
	 * @param process the process to get the standard or error output from.
	 * @param streamType detrmines whether to obtain the standard or error output.
	 * @return the output as a string.
	 */
	public String getOutput(Process process, String streamType)
	{
		StringBuilder sb = new StringBuilder();
		try
		{
			InputStream is = "std".equals(streamType) ? process.getInputStream() : process.getErrorStream();
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(is));
			try
			{
				String s = "";
				while (s != null)
				{
					s = reader.readLine();
					if (s != null) sb.append(s).append("\n");
				}
			}
			finally
			{
				reader.close();
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return sb.toString();
	}
}
