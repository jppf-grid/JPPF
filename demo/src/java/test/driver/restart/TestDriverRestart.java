/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.driver.restart;

import java.io.IOException;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.utils.ExceptionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class TestDriverRestart {
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
  /** */
  private static AtomicBoolean initializing = new AtomicBoolean(false);
  /** */
  private static JPPFNodeForwardingMBean nodeForwarder = null;
  /** */
  private static DiagnosticsMBean diagnostics = null;

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      client = new JPPFClient();

      JMXDriverConnectionWrapper jmx = null;
      while ((jmx = getJmxWrapper()) == null) Thread.sleep(10L);
      initializeProxies();
      while (diagnostics == null) Thread.sleep(10L);
      performGC();

      jmx.restartShutdown(1L, 1L); // restart the driver
      System.out.println("first try after restart");
      performGC(); // fails with IOException : this is expected since driver was restarted

      // wait until connection is initialized and retry
      while ((getJmxWrapper() == null) || (diagnostics == null)) Thread.sleep(10L);
      System.out.println("second try after restart");
      performGC();
    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (client != null) client.close();
    }
  }

  /**
   * Get a connection to the remote driver's MBean server..
   * @return a {@link JMXDriverConnectionWrapper} instance.
   */
  public static JMXDriverConnectionWrapper getJmxWrapper() {
    try {
      return client.awaitActiveConnectionPool().awaitJMXConnections(Operator.AT_LEAST, 1, true).get(0);
    } catch(Exception e) {
      e.printStackTrace();
      return null;
    }
    /*
    JMXDriverConnectionWrapper jmxConnection = null;
    final List<JPPFClientConnection> connections = client.getAllConnections();
    for (final JPPFClientConnection c : connections) {
      if (c.getStatus() == JPPFClientConnectionStatus.ACTIVE) {
        jmxConnection = c.getJmxConnection();
        if (jmxConnection != null && jmxConnection.isConnected()) {
          break;
        } else {
          jmxConnection = null;
        }
      }
    }
    return jmxConnection;
    */
  }

  /** */
  private static class KillDriverTask extends TimerTask {
    @Override
    public void run() {
      try {
        JPPFDriverAdminMBean jmx = getJmxWrapper();
        jmx.restartShutdown(10L, 10L);
      } catch (Exception e) {
      }
    }
  }

  /** */
  private static void initializeProxies() {
    if (initializing.compareAndSet(false, true)) {
      nodeForwarder = null;
      diagnostics = null;
      new Thread(new ProxySettingTask()).start();
    }
  }

  /** */
  private static class ProxySettingTask implements Runnable {
    @Override
    public void run() {
      try {
        boolean hasNullProxy = true;
        while (hasNullProxy) {
          final JMXDriverConnectionWrapper jmxWrapper = getJmxWrapper();
          if (jmxWrapper != null) {
            try {
              if (nodeForwarder == null) nodeForwarder = jmxWrapper.getNodeForwarder();
            } catch (final Exception ignore) {
            }
            try {
              if (diagnostics == null) {
                diagnostics = jmxWrapper.getDiagnosticsProxy();
              }
            } catch (final Exception ignore) {
            }
          }
          hasNullProxy = (nodeForwarder == null) || (diagnostics == null);
          try {
            if (hasNullProxy) {
              Thread.sleep(500L);
            }
          } catch (final InterruptedException ignore) {
          }
        }
      } finally {
        initializing.set(false);
      }
    }
  }

  /** */
  private static void performGC() {
    System.out.println("Performing garbage collection.");
    // Do the gc() in the drivers
    try {
      if (diagnostics == null) {
        return;
      }

      final HealthSnapshot healthSnapshot = diagnostics.healthSnapshot();
      System.out.println("health snapshot = " + healthSnapshot);
      if (healthSnapshot.getHeapUsedRatio() >= 0.6 || healthSnapshot.getNonheapUsedRatio() >= 0.6) {
        diagnostics.gc();
      }
    } catch (final IOException ex) {
      System.out.println("Unable to collect garbage in the drivers." + ExceptionUtils.getStackTrace(ex));
      initializeProxies();
    } catch (final Exception ex) {
      System.out.println("Unable to collect garbage in the drivers." + ExceptionUtils.getStackTrace(ex));
    }
  }
}
