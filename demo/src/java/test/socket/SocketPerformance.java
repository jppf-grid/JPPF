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

package test.socket;

import java.net.*;

import org.jppf.comm.socket.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class SocketPerformance
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(SocketPerformance.class);
	/**
	 * One kilobyte.
	 */
	private static final int KILO = 1024;
	/**
	 * One kilobyte.
	 */
	private static final int MEGA = 1024 * KILO;
	/**
	 * Determines whether the client is started in this JVM.
	 */
	private static boolean clientStarted = false;

	/**
	 * Main entry point.
	 * @param args no used.
	 */
	public static void main(String...args)
	{
		try
		{
			JPPFConfiguration.getProperties();
			if ((args == null) || (args.length < 1)) perform();
			else if ("server".equalsIgnoreCase(args[0])) performServer();
			else if ("client".equalsIgnoreCase(args[0])) performClient();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Start server and client in the same JVM.
	 * @throws Exception if any error occurs.
	 */
	private static void perform() throws Exception
	{
		Server server = new Server();
		server.start();
		Thread.sleep(500L);
		new Client().start();
		server.join();
	}

	/**
	 * Start the server.
	 * @throws Exception if any error occurs.
	 */
	private static void performServer() throws Exception
	{
		System.out.println("starting server");
		Server server = new Server();
		server.start();
		server.join();
		System.out.println("server ended");
	}

	/**
	 * Start server and client in the same JVM.
	 * @throws Exception if any error occurs.
	 */
	private static void performClient() throws Exception
	{
		System.out.println("starting client");
		Client client = new Client();
		client.start();
		client.join();
		System.out.println("client ended");
	}

	/**
	 * Server thread.
	 */
	private static class Server extends Thread
	{
		/**
		 * Default constructor.
		 */
		public Server()
		{
			super("Server");
			setDaemon(true);
		}

		/**
		 * Main processing loop.
		 * @see java.lang.Thread#run()
		 */
		public void run()
		{
			try
			{
				ServerSocket server = new ServerSocket(15555);
				while (true)
				{
					Socket s = server.accept();
					Connection c = new Connection(s);
					c.start();
					if (clientStarted)
					{
						c.join();
						break;
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}

	/**
	 * Server connection thread.
	 */
	private static class Connection extends Thread
	{
		/**
		 * Socket wrapper.
		 */
		private SocketWrapper socketWrapper = null;

		/**
		 * Initialize this server connection with thew specified socket.
		 * @param s - the socket to write to.
		 * @throws Exception if any error occurs.
		 */
		public Connection(Socket s) throws Exception
		{
			socketWrapper = new SocketClient(s);
			setDaemon(true);
		}

		/**
		 * Main processing loop.
		 * @see java.lang.Thread#run()
		 */
		public void run()
		{
			try
			{
				TypedProperties props = JPPFConfiguration.getProperties();
				int datasize = props.getInt("datasize.size", 1);
				int nbTasks = props.getInt("datasize.nbTasks", 10);
				String unit = props.getString("datasize.unit", "b").toLowerCase();
				if ("k".equals(unit)) datasize *= KILO;
				else if ("m".equals(unit)) datasize *= MEGA;
				byte[] data = new byte[datasize];
				for (int i=0; i<nbTasks; i++)
				{
					//log.info("Server: writing datasize");
					socketWrapper.writeInt(datasize);
					//log.info("Server: writing data");
					long start = System.currentTimeMillis();
					socketWrapper.write(data, 0, datasize);
					long elapsed = System.currentTimeMillis() - start;
					//log.info("Server: data written in " + elapsed + " ms");
				}
				socketWrapper.writeInt(0);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}


	/**
	 * Client thread.
	 */
	private static class Client extends Thread
	{
		/**
		 * Default constructor.
		 */
		public Client()
		{
			super("Client");
			setDaemon(true);
			clientStarted = true;
		}

		/**
		 * Main processing loop.
		 * @see java.lang.Thread#run()
		 */
		public void run()
		{
			try
			{
				SocketClient sc = new SocketClient("lolo-quad", 15555);
				byte[] data = null;
				while (true)
				{
					//log.info("Client: reading next datasize");
					int datasize = sc.readInt();
					if (datasize == 0)
					{
						log.info("Client: terminating");
						break;
					}
					//log.info("Client: read datasize = " + datasize + ", reading next data");
					if ((data == null) || (data.length < datasize)) data = new byte[datasize];
					long start = System.currentTimeMillis();
					sc.read(data, 0, datasize);
					long elapsed = System.currentTimeMillis() - start;
					log.info("Client: read data size = " + datasize + " in " + elapsed + " ms");
				}
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}
	}
}
