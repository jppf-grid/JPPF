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

package test.jmx;

import java.util.*;

import org.jppf.management.*;
import org.jppf.server.JPPFStats;

/**
 * 
 * @author Laurent Cohen
 */
public class TestJMX
{
	/**
	 * Entry point.
	 * @param args - not used.
	 */
	public static void main(String...args)
	{
		try
		{
			TestJMX t = new TestJMX();
			int success = 0;
			int failure = 0;
			int firstFailure = -1;
			for (int i=0; i<1000; i++)
			{
				try
				{
					int n = t.getNumberOfNodes();
					//System.out.println("nb nodes: " + n);
					success++;
				}
				catch(Exception ignore)
				{
					failure++;
					if (firstFailure < 0) firstFailure = i;
				}
			}
			System.out.println("successes: " + success + ", failures: " + failure + ", first failure: " + firstFailure);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Retrieve the number of nodes from the server.
	 * @return the numbe rof nodes as an int.
	 * @throws Exception if any error occurs.
	 */
	public int getNumberOfNodes() throws Exception
	{
	  // create a JMX connection to the driver
	  // replace "your_host_address" and "your_port" with the appropriate values for your configuration
	  JMXDriverConnectionWrapper serverJmxConnection = new JMXDriverConnectionWrapper("localhost", 11198);
	  // start the connection process and wait until the connection is established
	  serverJmxConnection.connect();
	  while (!serverJmxConnection.isConnected()) Thread.currentThread().sleep(1);
	  // request the statistics from the driver
	  JPPFStats stats = serverJmxConnection.statistics();
    /*
	  while (stats == null)
    {
	    Thread.currentThread().sleep(50);
    	stats = jmxConnection.statistics();
    }
    */
	  serverJmxConnection.close();
	  // return the current number of nodes
	  return stats.nbNodes;
	}

	/**
	 * Stop all the nodes attached to a server.
	 * @param host the host on which the server is running.
	 * @param port the server management port (same value as "jppf.management.port" in server configuration).
	 */
	public void stopAllNodes(String host, int port)
	{
		try
		{
		  // create a JMX connection to the driver
		  JMXDriverConnectionWrapper serverJmxConnection = new JMXDriverConnectionWrapper(host, port);
		  // start the connection process
		  serverJmxConnection.connect();
		  // wait until the connection is established
		  while (!serverJmxConnection.isConnected()) Thread.currentThread().sleep(50);
		  // query the server for the attached nodes
		  Collection<NodeManagementInfo> nodeList = null;
		  nodeList = serverJmxConnection.nodesInformation();
		  // for each node
		  for (NodeManagementInfo info: nodeList)
		  {
		    // crate a JMX connection to each node
		    JMXNodeConnectionWrapper nodeJmxConnection = new JMXNodeConnectionWrapper(info.getHost(), info.getPort());
		    // perform the actual connection to the JMX server
		    nodeJmxConnection.connect();
				try
				{
				  // wait until the connection is established
				  while (!nodeJmxConnection.isConnected()) Thread.currentThread().sleep(50);
				  // stop the node
				  nodeJmxConnection.shutdown();
				}
			  catch(Exception e)
			  {
			  	System.out.println("could not stop node at " + info.getHost() + ":" + info.getPort());
			  	e.printStackTrace();
			  }
		  }
		}
	  catch(Exception e)
	  {
	  	System.out.println("could not query server at " + host + ":" + port);
	  	e.printStackTrace();
	  }
	}
}
