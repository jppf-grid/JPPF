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

import java.lang.management.*;
import java.util.*;
import java.util.regex.Pattern;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.*;

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
      //t.testConnectAndWait();
      t.testDriverRestart();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Retrieve the number of nodes from the server.
   * @return the number rof nodes as an int.
   * @throws Exception if any error occurs.
   */
  public int testDriverRestart() throws Exception {
    JPPFClient client = null;
    try {
      TypedProperties config = JPPFConfiguration.getProperties();
      config.setProperty("jppf.pool.size", "10");
      output("reconnect.max.time = " + config.getString("reconnect.max.time"));
      ClientListener myListener = new ClientListener() {
        @Override
        public void newConnection(final ClientEvent event) {
        }
        @Override
        public void connectionFailed(final ClientEvent event) {
          AbstractJPPFClientConnection c = (AbstractJPPFClientConnection) event.getConnection();
          output("connection failed: " + c);
          if (c.getJmxConnection() != null) {
            try {
              c.getJmxConnection().close();
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      };
      client = new JPPFClient();
      //client = new JPPFClient(myListener);
      while (client.getAllConnections().size() < 10) Thread.sleep(100L);
      for (int i=0; i<1; i++) {
        output("iteration " + i);
        long start = System.nanoTime();
        restartDriver(client);
        long elapsed = System.nanoTime() - start;
        output("iteration " + i + " performed in " + StringUtils.toStringDuration(elapsed/1000000L));
        Thread.sleep(1000L);
      }
  
      String[] threadNames = threadNames("^JMX connection .*");
      output("*** found " + threadNames.length + " 'JMX connection ...' threads ***");
      return 0;
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Restart the driver.
   * @param client the JPPF client.
   * @throws Exception if any error occurs.
   */
  private void restartDriver(final JPPFClient client) throws Exception
  {
    JMXDriverConnectionWrapper jmx = null;
    while (jmx == null)
    {
      try
      {
        jmx = ((AbstractJPPFClientConnection) client.getClientConnection()).getJmxConnection();
      }
      catch (Exception e)
      {
        Thread.sleep(100L);
      }
    }
    jmx.restartShutdown(100L, 6000L);
    Thread.sleep(500L);
    int n = JPPFConfiguration.getProperties().getInt("jppf.pool.size");
    int count = 0;
    while (count < n)
    {
      try
      {
        count = 0;
        Thread.sleep(100L);
        List<JPPFClientConnection> list = client.getAllConnections();
        for (JPPFClientConnection conn: list)
        {
          if (((AbstractJPPFClientConnection) conn).getStatus() == JPPFClientConnectionStatus.ACTIVE) count++;
        }
      }
      catch (Exception ignore)
      {
      }
    }
  }

  /**
   * Print a message to the console and/or log file.
   * @param message the message to print.
   */
  private static void output(final String message)
  {
    System.out.println(message);
    //log.info(message);
  }

  /**
   * Get the names of all threads in this JVM mathcing the specified regex pattern.
   * @param pattern the pattern to match against.
   * @return an array of thread names.
   */
  public String[] threadNames(final String pattern)
  {
    Pattern p = pattern == null ? null : Pattern.compile(pattern);
    ThreadMXBean threadsBean = ManagementFactory.getThreadMXBean();
    long[] ids = threadsBean.getAllThreadIds();
    ThreadInfo[] infos = threadsBean.getThreadInfo(ids, 0);
    List<String> result = new ArrayList<String>();
    for (int i=0; i<infos.length; i++)
    {
      if ((p == null) || p.matcher(infos[i].getThreadName()).matches())
        result.add(infos[i].getThreadName());
    }
    return result.toArray(new String[result.size()]);
  }
}
