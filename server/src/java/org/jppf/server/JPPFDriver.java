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
package org.jppf.server;

import java.util.Timer;

import org.jppf.JPPFException;
import org.jppf.classloader.LocalClassLoaderChannel;
import org.jppf.comm.discovery.JPPFConnectionInformation;
import org.jppf.logging.jmx.JmxMessageNotifier;
import org.jppf.process.LauncherListener;
import org.jppf.server.app.JPPFApplicationServer;
import org.jppf.server.job.JPPFJobManager;
import org.jppf.server.nio.classloader.*;
import org.jppf.server.nio.nodeserver.*;
import org.jppf.server.node.local.JPPFLocalNode;
import org.jppf.server.queue.*;
import org.jppf.startup.*;
import org.jppf.utils.*;
import org.slf4j.*;

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
	 * Logger for this class.
	 */
	static Logger log = LoggerFactory.getLogger(JPPFDriver.class);
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
	private JPPFApplicationServer[] applicationServers = null;
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
	 * This listener gathers the statistics published through the management interface.
	 */
	private JPPFDriverStatsUpdater statsUpdater = new JPPFDriverStatsUpdater();
	/**
	 * Generates the statistcs events of which all related listeners are notified.
	 */
	private JPPFDriverStatsManager statsManager = new JPPFDriverStatsManager();
	/**
	 * Manages and monitors the jobs thoughout their processing within this driver.
	 */
	private JPPFJobManager jobManager = null;
	/**
	 * Uuid for this driver.
	 */
	private String uuid = new JPPFUuid(JPPFUuid.HEXADECIMAL, 32).toString().toUpperCase();
	/**
	 * Performs initialization of the driver's components.
	 */
	private DriverInitializer initializer = null;
	/**
	 * Manages information about the nodes.
	 */
	private NodeInformationHandler nodeHandler = null;

	/**
	 * Initialize this JPPFDriver.
	 */
	protected JPPFDriver()
	{
		// initialize the jmx logger
		new JmxMessageNotifier();
		nodeHandler = new NodeInformationHandler();
		statsManager.addListener(statsUpdater);
		initializer = new DriverInitializer(this);
		if (debugEnabled) log.debug("instantiating JPPF driver with uuid=" + uuid);
	}

	/**
	 * Initialize and start this driver.
	 * @throws Exception if the initialization fails.
	 */
	public void run() throws Exception
	{
		jobManager = new JPPFJobManager();
		taskQueue = new JPPFPriorityQueue();
		((JPPFPriorityQueue) taskQueue).addQueueListener(jobManager);
		JPPFConnectionInformation info = initializer.getConnectionInformation();
		TypedProperties config = JPPFConfiguration.getProperties();

		initializer.initRecoveryServer();

		classServer = new ClassNioServer(info.classServerPorts);
		classServer.start();
		initializer.printInitializedMessage(info.classServerPorts, "Class Server");

		applicationServers = new JPPFApplicationServer[info.applicationServerPorts.length];
		for (int i=0; i<info.applicationServerPorts.length; i++)
		{
			applicationServers[i] = new JPPFApplicationServer(info.applicationServerPorts[i]);
			applicationServers[i].start();
		}
		initializer.printInitializedMessage(info.applicationServerPorts, "Client Server");

		nodeNioServer = new NodeNioServer(info.nodeServerPorts);
		nodeNioServer.start();
		initializer.printInitializedMessage(info.nodeServerPorts, "Tasks Server");

		if (config.getBoolean("jppf.local.node.enabled", false))
		{
			LocalClassLoaderChannel localClassChannel = new LocalClassLoaderChannel(new LocalClassContext());
			LocalNodeChannel localNodeChannel = new LocalNodeChannel(new LocalNodeContext());
			JPPFLocalNode node = new JPPFLocalNode(localNodeChannel, localClassChannel);
			classServer.initLocalChannel(localClassChannel);
			nodeNioServer.initLocalChannel(localNodeChannel);
			new Thread(node, "Local node").start();
		}
		
		initializer.initJmxServer();
		new JPPFStartupLoader().load(JPPFDriverStartupSPI.class);

		initializer.initBroadcaster();
		initializer.initPeers();
		System.out.println("JPPF Driver initialization complete");
	}

	/**
	 * Get the singleton instance of the JPPFDriver.
	 * @return a <code>JPPFDriver</code> instance.
	 */
	public static JPPFDriver getInstance()
	{
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
	 * Get the JPPF nodes server.
	 * @return a <code>NodeNioServer</code> instance.
	 */
	public NodeNioServer getNodeNioServer()
	{
		return nodeNioServer;
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
	 * Get this driver's unique identifier.
	 * @return the uuid as a string.
	 */
	public String getUuid()
	{
		return uuid;
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
	public void shutdown()
	{
		log.info("Shutting down");
		initializer.stopBroadcaster();
		initializer.stopPeerDiscoveryThread();
		classServer.end();
		classServer = null;
		if (nodeNioServer != null)
		{
			nodeNioServer.end();
			nodeNioServer = null;
		}
		for (int i=0; i<applicationServers.length; i++)
		{
			applicationServers[i].end();
			applicationServers[i] = null;
		}
		applicationServers = null;
		initializer.stopJmxServer();
		jobManager.close();
		initializer.stopRecoveryServer();
	}

	/**
	 * Get the listener that gathers the statistics published through the management interface.
	 * @return a <code>JPPFStatsUpdater</code> instance.
	 */
	public JPPFDriverStatsUpdater getStatsUpdater()
	{
		return statsUpdater;
	}

	/**
	 * Get a reference to the object that generates the statistics events of which all related listeners are notified.
	 * @return a <code>JPPFDriverStatsManager</code> instance.
	 */
	public JPPFDriverStatsManager getStatsManager()
	{
		return statsManager;
	}

	/**
	 * Get the object that manages and monitors the jobs thoughout their processing within this driver.
	 * @return an instance of <code>JPPFJobManager</code>.
	 */
	public JPPFJobManager getJobManager()
	{
		return jobManager;
	}

	/**
	 * Get this driver's initializer.
	 * @return a <code>DriverInitializer</code> instance.
	 */
	public DriverInitializer getInitializer()
	{
		return initializer;
	}

	/**
	 * Get the object that manages information about the nodes.
	 * @return a {@link NodeInformationHandler} instance.
	 */
	public NodeInformationHandler getNodeHandler()
	{
		return nodeHandler;
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
				throw new JPPFException("The driver should be run with an argument representing a valid TCP port or 'noLauncher'");
			}
			if (!"noLauncher".equals(args[0]))
			{
				int port = Integer.parseInt(args[0]);
				new LauncherListener(port).start();
			}

			instance = new JPPFDriver();
			instance.run();
			//JPPFDriver driver = getInstance();
			//driver.run();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
