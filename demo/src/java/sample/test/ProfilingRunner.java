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
package sample.test;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.server.JPPFStats;
import org.jppf.utils.JPPFConfiguration;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ProfilingRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(ProfilingRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Size of the data in each task, in KB.
	 */
	private static int dataSize = JPPFConfiguration.getProperties().getInt("profiling.data.size");

	/**
	 * Entry point for this class, submits the tasks with a set duration to the server.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			MyClientListener clientListener = new MyClientListener();
			jppfClient = new JPPFClient(clientListener);
			// .... submit the jobs ....
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jppfClient.close();
		}
	}

	/**
	 * Check the status of a connection and determine grid availability.
	 * @param connection the driverconnection to check.
	 * @return true if the grid is available, false otherwise.
	 * @throws Exception if nay error occurs.
	 */
	protected static boolean isGridAvailable(JPPFClientConnection connection) throws Exception
	{
		boolean gridAvailable = false;

		if (connection != null)
		{
			if (connection.getStatus().equals(JPPFClientConnectionStatus.ACTIVE))
			{
				JPPFStats statistics = jppfClient.requestStatistics();

				if (statistics != null && statistics.getNbNodes() > 0)
				{
					gridAvailable = true;
					log.info("Grid available");
				}
				else
				{
					log.warn("Nodes on grid not available");
				}
			}
			else
			{
				log.warn("Connection to grid not active: " + connection.getStatus());
			}
		}
		else
		{
			log.warn("Connection to grid not available");
		}

		return false;
	}

  /**
   * Listener for connection status change events.
   */
  public static class MyStatusListener implements ClientConnectionStatusListener
  {
    /**
     * {@inheritDoc}
     */
    public void statusChanged(ClientConnectionStatusEvent event)
    {
      JPPFClientConnection connection = (JPPFClientConnection) event.getClientConnectionStatusHandler();
      JPPFClientConnectionStatus status = connection.getStatus();
      log.info("Connection " + connection.getName() + " status changed to " + status);
      try
      {
      	boolean available = isGridAvailable(connection);
      }
      catch(Exception e)
      {
      	log.error(e.getMessage(), e);
      }
    }
  }

  /**
   * Listener for new driver connections.
   */
  public static class MyClientListener implements ClientListener
  {
    /**
     * {@inheritDoc}
     */
    public void newConnection(ClientEvent event)
    {
      // the new connection is the source of the event
      JPPFClientConnection connection = event.getConnection();
      log.info("New connection with name " + connection.getName());
      // register to receive staus events on the new connection
      connection.addClientConnectionStatusListener(new MyStatusListener());
    }
  }
}
