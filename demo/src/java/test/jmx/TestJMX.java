/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
  public static void main(final String...args)
  {
    try
    {
      TestJMX t = new TestJMX();
      t.testConnectAndWait();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Test the method that gets the number of nodes.
   * @throws Exception if any error occurs.
   */
  public void testNumberOfNodes() throws Exception
  {
    int success = 0;
    int failure = 0;
    int firstFailure = -1;
    for (int i=0; i<1000; i++)
    {
      try
      {
        int n = getNumberOfNodes();
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

  /**
   * Test the connectAndWait() method with the JMXMP connector.
   * @throws Exception if any error occurs.
   */
  public void testConnectAndWait() throws Exception
  {
    // for this test, make sure the corresponding node is NOT started.
    JMXNodeConnectionWrapper jmx = new JMXNodeConnectionWrapper("118.1.1.10", 12001);
    long start = System.nanoTime();
    System.out.println("before connectAndWait()");
    jmx.connectAndWait(2000L);
    long elapsed = System.nanoTime() - start;
    System.out.println("actually waited for " + (elapsed/1000000) + " ms");
    /*
		System.out.println("*** press any key to terminate ***");
		System.in.read();
     */
  }

  /**
   * Retrieve the number of nodes from the server.
   * @return the number rof nodes as an int.
   * @throws Exception if any error occurs.
   */
  public int getNumberOfNodes() throws Exception
  {
    // create a JMX connection to the driver
    // replace "your_host_address" and "your_port" with the appropriate values for your configuration
    JMXDriverConnectionWrapper jmxConnection = new JMXDriverConnectionWrapper("localhost", 11198);
    // start the connection process and wait until the connection is established
    jmxConnection.connectAndWait(1000);
    // request the statistics from the driver
    JPPFStats stats = jmxConnection.statistics();
    /*
	  while (stats == null)
    {
	    Thread.currentThread().sleep(50);
    	stats = jmxConnection.statistics();
    }
     */
    jmxConnection.close();
    // return the current number of nodes
    return (int) stats.getNodes().getLatest();
  }
}
