/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package sample.dist.manyjobs;

import org.jppf.client.JPPFClient;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Runner class for the &quot;Long Task&quot; demo.
 * @author Laurent Cohen
 */
public class ManyConnections {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ManyConnections.class);
  /**
   * JPPF client used to submit execution requests.
   */
  private static JPPFClient jppfClient = null;

  /**
   * Entry point for this class, submits the tasks with a set duration to the server.
   * @param args not used.
   */
  public static void main(final String... args) {
    try {
      final TypedProperties props = JPPFConfiguration.getProperties();
      props.set(JPPFProperties.DISCOVERY_ENABLED, true).set(JPPFProperties.POOL_SIZE, 50);
      final long start = System.nanoTime();
      jppfClient = new JPPFClient();
      //Thread.sleep(1000);
      int count = 0;
      while ((count = jppfClient.getAllConnectionsCount()) < 50) Thread.sleep(10L);
      final long elapsed = System.nanoTime() - start;
      print("found " + count + " connections in " + (elapsed / 1000000) + " ms");
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (jppfClient != null) jppfClient.close();
    }
  }

  /**
   * Print a message to the log and to the console.
   * @param msg the message to print.
   */
  private static void print(final String msg) {
    log.info(msg);
    System.out.println(msg);
  }
}
