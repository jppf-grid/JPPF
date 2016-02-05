/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import javax.management.*;

import org.jppf.logging.jmx.JmxLogger;
import org.jppf.management.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 *
 * @author Laurent Cohen
 */
public class DriverJMX {
  /**
   *
   * @param args not used.
   */
  public static void main(final String[] args) {
    try  {
      System.out.println("connecting ...");
      // URL: service:jmx:jmxmp://52.29.74.112:11198
      JMXDriverConnectionWrapper jmx = new JMXDriverConnectionWrapper("52.29.74.112", 11198, false);
      jmx.connect();
      while (!jmx.isConnected()) Thread.sleep(10L);
      System.out.println("connected");
      JPPFSystemInformation info = jmx.systemInformation();
      String s = formatProperties(info);
      System.out.println("driver info:\n" + s);
      System.out.println("stats: " + jmx.statistics());
      System.out.println("diagnostics: " + jmx.getDiagnosticsProxy().healthSnapshot());
      registerForLogging(jmx);
      StreamUtils.waitKeyPressed();
      jmx.close();
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   * @param jmx .
   * @throws Exception .
   */
  private static void registerForLogging(final JMXDriverConnectionWrapper jmx) throws Exception {
    JmxLogger driverProxy =
        jmx.getProxy(JmxLogger.DEFAULT_MBEAN_NAME, JmxLogger.class);
    // use a handback object so we know where the log messages come from
    String source = "driver " + jmx.getHost() + ":" + jmx.getPort();
    // subscribe to all notifications from the MBean
    NotificationListener listener = new MyLoggingHandler();
    driverProxy.addNotificationListener(listener, null, source);
  }

  /**
   * Print the specified system info to a string.
   * @param info the information to print.
   * @return a String with the formatted information.
   */
  private static String formatProperties(final JPPFSystemInformation info) {
    final PropertiesTableFormat format = new TextPropertiesTableFormat("driver");
    format.start();
    if (info == null) format.print("No information was found");
    else {
      format.formatTable(info.getUuid(), "UUID");
      format.formatTable(info.getSystem(), "System Properties");
      format.formatTable(info.getEnv(), "Environment Variables");
      format.formatTable(info.getRuntime(), "Runtime Information");
      format.formatTable(info.getJppf(), "JPPF configuration");
      format.formatTable(info.getNetwork(), "Network configuration");
      format.formatTable(info.getStorage(), "Storage Information");
      format.formatTable(info.getOS(), "Operating System Information");
    }
    format.end();
    return format.getText();
  }

  /**
   * 
   */
  public static class MyLoggingHandler implements NotificationListener {
    @Override
    public void handleNotification(final Notification notification, final Object handback) {
      String message = notification.getMessage();
      //System.out.print(handback.toString() + ": " + message);
      System.out.print(message);
    }
  }
}
