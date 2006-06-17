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
package org.jppf.server;

import java.io.IOException;
import java.net.*;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.utils.JPPFConfiguration;

/**
 * <p>This class is intended as a controller for the JPPF driver, to enable stopping and restarting it when requested.
 * <p>It performs the following operations:
 * <ul>
 * <li>open a server socket the driver will listen to (port number is dynamically attributed)</li>
 * <li>Start the JPPF driver as a subprocess, sending the the server socket port number as an argument</li>
 * <li>Wait for the subprocess to exit</li>
 * <li>If the subprocess exit code is equal to 2, the subprocess is restarted</li>
 * </ul>
 * @author Laurent Cohen
 */
public class DriverLauncher
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(DriverLauncher.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * A reference to the JPPF driver subprocess, used to kill it when the driver launcher exits.
	 */
	private static Process process = null;
	/**
	 * The server socket the driver listens to.
	 */
	private static ServerSocket driverServer = null;
	/**
	 * The prot number the erver socket listens to.
	 */ 
	private static int driverPort = 0;

	/**
	 * Start this application, then the JPPF driver a subprocess.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		boolean end = false;
		try
		{
			Runnable hook = new Runnable()
			{
				public void run()
				{
					if (process != null) process.destroy();
				}
			};
			Runtime.getRuntime().addShutdownHook(new Thread(hook));
			startDriverSocket();
			while (!end)
			{
				process = buildProcess();
				if (debugEnabled) log.debug("started driver process [" + process + "]");
				int n = process.waitFor();
				process.destroy();
				if (n != 2) end = true;
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
	private static Process buildProcess() throws Exception
	{
		List<String> command = new ArrayList<String>();
		command.add(System.getProperty("java.home")+"/bin/java");
		command.add("-cp");
		command.add(System.getProperty("java.class.path"));
		String s = System.getProperty("user.language");
		if (s == null) s = Locale.getDefault().getLanguage();
		command.add("-Duser.language="+s);
		command.add("-D"+JPPFConfiguration.CONFIG_PROPERTY+"="+System.getProperty(JPPFConfiguration.CONFIG_PROPERTY));
		command.add("-Dlog4j.configuration="+System.getProperty("log4j.configuration"));
		command.add("-server");
		int n = JPPFConfiguration.getProperties().getInt("max.memory.option", 128);
		command.add("-Xmx" + n + "m");

		command.add("-Xdebug");
		command.add("-Xrunjdwp:transport=dt_socket,address=localhost:8000,server=y,suspend=n");		

		command.add("org.jppf.server.JPPFDriver");
		command.add("" + driverPort);
		if (debugEnabled) log.debug("process command:\n" + command);
		ProcessBuilder builder = new ProcessBuilder(command);
		return builder.start();
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
	private static int startDriverSocket()
	{
		try
		{
			driverServer = new ServerSocket(0);
			driverPort = driverServer.getLocalPort();
			Runnable r = new Runnable()
			{
				public void run()
				{
					while (true)
					{
						try
						{
							Socket s = driverServer.accept();
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
				driverServer.close();
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
				if (debugEnabled) log.debug(ioe.getMessage(), ioe);
				System.exit(1);
			}
		}
		return driverPort;
	}
}
