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
package org.jppf.server;

import java.net.Socket;
import java.util.*;

import org.apache.log4j.Logger;
import org.jppf.*;
import org.jppf.security.*;
import org.jppf.server.app.JPPFApplicationServer;
import org.jppf.server.nio.classloader.ClassNioServer;
import org.jppf.server.nio.nodeserver.NodeNioServer;
import org.jppf.server.peer.JPPFPeerInitializer;
import org.jppf.server.scheduler.bundle.*;
import org.jppf.utils.*;

/**
 * This class serves as an initializer for the entire JPPF server. It follows the singleton pattern and provides access,
 * accross the JVM, to the tasks execution queue.
 * <p>It also holds a server for incoming client connections, a server for incoming node connections, along with a class server
 * to handle requests to and from remote class loaders. 
 * @author Laurent Cohen
 */
public class JPPFDriver
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFDriver.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * Singleton instance of the JPPFDriver.
	 */
	private static JPPFDriver instance = null;
	/**
	 * The queue that handles the tasks to execute. Objects are added to, and removed from, this queue, asynchronously and by
	 * multiple threads.
	 */
	private JPPFQueue taskQueue = null;
	/**
	 * Serves the execution requests coming from client applications.
	 */
	private JPPFApplicationServer applicationServer = null;
	/**
	 * Serves the JPPF nodes.
	 */
	private NodeNioServer nodeNioServer = null;
	/**
	 * Serves class loading requests from the JPPF nodes.
	 */
	private ClassNioServer classServer = null;
	/**
	 * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
	 */
	private boolean shuttingDown = false;
	/**
	 * Security credentials associated with this JPPF driver.
	 */
	private JPPFSecurityContext credentials = null;

	/**
	 * Initialize this JPPFDriver.
	 */
	protected JPPFDriver()
	{
		initCredentials();
	}

	/**
	 * Initialize and start this driver.
	 * @throws Exception if the initialization fails.
	 */
	public void run() throws Exception
	{
		taskQueue = new JPPFQueue();
		TypedProperties props = JPPFConfiguration.getProperties();

		int port = props.getInt("class.server.port", 11111);
		classServer = new ClassNioServer(port);
		classServer.start();

		Bundler bundler = BundlerFactory.createBundler();
		port = props.getInt("app.server.port", 11112);
		applicationServer = new JPPFApplicationServer(port);
		applicationServer.start();

		port = props.getInt("node.server.port", 11113);
		//nodeServer = new JPPFNodeServer(port, bundler);
		//nodeServer.start();
		nodeNioServer = new NodeNioServer(port, bundler);
		nodeNioServer.start();

		initPeers();
	}

	/**
	 * Initialize the security credentials associated with this JPPF driver.
	 */
	private void initCredentials()
	{
		String uuid = new JPPFUuid().toString();
		StringBuilder sb = new StringBuilder("Driver:");
		sb.append(VersionUtils.getLocalIpAddress()).append(":");
		TypedProperties props = JPPFConfiguration.getProperties();
		sb.append(props.getInt("class.server.port", 11111)).append(":");
		sb.append(props.getInt("app.server.port", 11112)).append(":");
		sb.append(props.getInt("node.server.port", 11113));
		credentials = new JPPFSecurityContext(uuid, sb.toString(), new JPPFCredentials());
	}

	/**
	 * Initialize this driver's peers.
	 */
	private void initPeers()
	{
		TypedProperties props = JPPFConfiguration.getProperties();
		String peerNames = props.getString("jppf.peers");
		if ((peerNames == null) || "".equals(peerNames.trim())) return;
		String[] names = peerNames.split(" ");
		for (String peerName: names) new JPPFPeerInitializer(peerName).start();
	}
	
	/**
	 * Get the singleton instance of the JPPFDriver.
	 * @return a <code>JPPFDriver</code> instance.
	 */
	public static JPPFDriver getInstance()
	{
		if (instance == null) instance = new JPPFDriver();
		return instance;
	}

	/**
	 * Get the queue that handles the tasks to execute.
	 * @return a JPPFQueue instance.
	 */
	public static JPPFQueue getQueue()
	{
		return getInstance().taskQueue;
	}
	
	/**
	 * Get the JPPF class server.
	 * @return a <code>ClassNioServer</code> instance.
	 */
	public ClassNioServer getClassServer()
	{
		return classServer;
	}

	/**
	 * Determines whether this server has initiated a shutdown, in which case it does not accept connections anymore.
	 * @return true if a shutdown is initiated, false otherwise.
	 */
	public boolean isShuttingDown()
	{
		return shuttingDown;
	}

	/**
	 * Get the security credentials associated with this JPPF driver.
	 * @return a <code>JPPFSecurityContext</code> instance.
	 */
	public JPPFSecurityContext getCredentials()
	{
		return credentials;
	}

	/**
	 * Get this driver's unique identifier.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return credentials.getUuid();
	}

	/**
	 * Initialize this task with the specified parameters.<br>
	 * The shutdown is initiated after the specified shutdown delay has expired.<br>
	 * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
	 * @param shutdownDelay delay, in milliseconds, after which the server shutdown is initiated. A value of 0 or less
	 * means an immediate shutdown.
	 * @param restart determines whether the server should restart after shutdown is complete.
	 * If set to false, then the JVM will exit.
	 * @param restartDelay delay, starting from shutdown completion, after which the server is restarted.
	 * A value of 0 or less means the server is restarted immediately after the shutdown is complete. 
	 */
	public void initiateShutdownRestart(long shutdownDelay, boolean restart, long restartDelay)
	{
		log.info("Scheduling server shutdown in " + shutdownDelay + " ms");
		shuttingDown = true;
		if (shutdownDelay <= 0L) shutdownDelay = 0L;
		Timer timer = new Timer();
		ShutdownRestartTask task = new ShutdownRestartTask(timer, restart, restartDelay);
		timer.schedule(task, shutdownDelay);
	}
	
	/**
	 * Shutdown this server and all its components.
	 */
	private void shutdown()
	{
		classServer.end();
		classServer = null;
		nodeNioServer.end();
		nodeNioServer = null;
		applicationServer.end();
		applicationServer = null;
	}
	
	/**
	 * Listen to a socket connection setup in the Driver Launcher, to handle the situation when the Launcher dies
	 * unexpectedly.<br>
	 * In that situation, the connection is broken and this driver knows that it must exit.
	 * @param port the port to listen to.
	 */
	private static void runDriverListener(final int port)
	{
		Runnable r = new Runnable()
		{
			public void run()
			{
				try
				{
					Socket s = new Socket("localhost", port);
					s.getInputStream().read();
				}
				catch(Throwable t)
				{
					System.exit(0);
				}
			}
		};
		new Thread(r).start();
	}

	/**
	 * Start the JPPF server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			if (debugEnabled) log.debug("starting the JPPF driver");
			if ((args == null) || (args.length <= 0))
			{
				throw new JPPFException("The driver should be run with an argument representing a valid TCP port");
			}
			if (!"noLauncher".equals(args[0]))
			{
				int port = Integer.parseInt(args[0]);
				runDriverListener(port);
			}

			JPPFDriver driver = getInstance();
			driver.run();
		}
		catch(Exception e)
		{
			log.fatal(e.getMessage(), e);
			System.exit(1);
		}
	}
	
	/**
	 * Task used by a timer to shutdown, and eventually restart, this server.<br>
	 * Both shutdown and restart operations can be performed with a specified delay.
	 */
	private class ShutdownRestartTask extends TimerTask
	{
		/**
		 * Determines whether the server should restart after shutdown is complete.
		 */
		private boolean restart = true;
		/**
		 * Delay, starting from shutdown completion, after which the server is restarted.
		 */
		private long restartDelay = 0L;
		/**
		 * The timer used to schedule this task, and eventually the restart operation.
		 */
		private Timer timer = null;

		/**
		 * Initialize this task with the specified parameters.<br>
		 * The shutdown is initiated after the specified shutdown delay has expired.<br>
		 * If the restart parameter is set to false then the JVM exits after the shutdown is complete.
		 * @param timer the timer used to schedule this task, and eventually the restart operation.
		 * @param restart determines whether the server should restart after shutdown is complete.
		 * If set to false, then the JVM will exit.
		 * @param restartDelay delay, starting from shutdown completion, after which the server is restarted.
		 * A value of 0 or less means the server is restarted immediately after the shutdown is complete. 
		 */
		public ShutdownRestartTask(Timer timer, boolean restart, long restartDelay)
		{
			this.timer = timer;
			this.restart = restart;
			this.restartDelay = restartDelay;
		}

		/**
		 * Perform the actual shutdown, and eventually restart, as specified in the constructor.
		 * @see java.util.TimerTask#run()
		 */
		public void run()
		{
			log.info("Initiating shutdown");
			shutdown();
			if (!restart)
			{
				log.info("Performing requested exit");
				System.exit(0);
			}
			else
			{
				TimerTask task = new TimerTask()
				{
					public void run()
					{
						try
						{
							log.info("Initiating restart");
							//JPPFDriver.getInstance().run();
							//log.info("Restart complete");
							System.exit(2);
						}
						catch(Exception e)
						{
							log.fatal(e.getMessage(), e);
							throw new JPPFError("Could not restart the JPPFDriver");
						}
					}
				};
				cancel();
				timer.schedule(task, restartDelay);
			}
		}
	}
}
