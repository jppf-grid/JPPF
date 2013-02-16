/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package test.ipprobe;

import java.net.Socket;
import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 */
public class Probe
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Probe.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Main entry point.
   * @param args not used.
   */
  public static void main(final String...args)
  {
    try
    {
      TypedProperties config = JPPFConfiguration.getProperties();
      String host = config.getString("ipprobe.host", "localhost");
      String[] sports = config.getString("ipprobe.ports").trim().split("\\s");
      Integer[] ports = new Integer[sports.length];
      for (int i=0; i<sports.length; i++) ports[i] = Integer.valueOf(sports[i]);
      Timer timer = new Timer();
      ProbeTask task = new ProbeTask(host, ports);
      timer.schedule(task, 10, config.getLong("ipprobe.interval", 1000L));
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Timer task that attempts to connect to specified TCP ports.
   */
  public static class ProbeTask extends TimerTask
  {
    /**
     * The host to connect to.
     */
    private String host = null;
    /**
     * The ports to connect to.
     */
    private Integer[] ports = null;

    /**
     * Initialize this task with the specified host and ports.
     * @param host - the host to connect to.
     * @param ports - the ports to connect to.
     */
    public ProbeTask(final String host, final Integer... ports)
    {
      this.host = host;
      this.ports = ports;
    }

    /**
     * Execute this task.
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run()
    {
      for (int n: ports)
      {
        try
        {
          Socket s = new Socket(host, n);
          s.close();
        }
        catch(Exception e)
        {
          if (debugEnabled) log.debug(e.getMessage(), e);
          else log.error(e.getMessage());
        }
      }
    }
  }
}
