/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.client.monitoring.jobs;

import java.util.*;
import java.util.concurrent.*;

import javax.management.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.job.JobNotification;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.concurrent.*;
import org.slf4j.*;

/**
 * Common superclass for classes that update the jobs hierarchy by subscribing toJMX notifications from the drivers.
 * @author Laurent Cohen
 * @since 5.1
 * @exclude
 */
abstract class AbstractJobNotificationsHandler implements NotificationListener, JobMonitoringHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractJobNotificationsHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * the associated job monitor.
   */
  final JobMonitor monitor;
  /**
   * Used to queue the JMX notifications in the same order they are received with a minimum of interruption.
   */
  final ExecutorService executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("JobNotificationsHandler"));
  /**
   * 
   */
  final Map<String, JmxInitializer> initializerMap = new HashMap<>();
  /**
   * 
   */
  final DriverListener driverListener;

  /**
   * Initialize with the specified job monitor.
   * @param monitor an instance of {@link JobMonitor}.
   */
  AbstractJobNotificationsHandler(final JobMonitor monitor) {
    this.monitor = monitor;
    monitor.getTopologyManager().addTopologyListener(driverListener = new DriverListener());
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    //if (log.isTraceEnabled()) log.trace("got jmx notification: {}", notification);
    executor.execute(new NotificationHandlingTask((JobNotification) notification));
  }

  /**
   * Handle a notification asynchronously.
   * @param notif the notification to handle.
   */
  abstract void handleNotificationAsync(final JobNotification notif);

  @Override
  public void close() {
    monitor.getTopologyManager().removeTopologyListener(driverListener);
    executor.shutdownNow();
    synchronized(initializerMap) {
      initializerMap.clear();
    }
  }

  /**
   * Instances of this task handle raw JMX notifications handed to the executor queue.
   */
  private class NotificationHandlingTask implements Runnable {
    /**
     * The notification to handle.
     */
    final JobNotification notif;

    /**
     * Create this initializer witht he specified driver.
     * @param notif the notification to handle.
     */
    NotificationHandlingTask(final JobNotification notif) {
      this.notif = notif;
    }

    @Override
    public void run() {
      handleNotificationAsync(notif);
    }
  }

  /**
   * Listens to driver added/removed events.
   */
  private class DriverListener extends TopologyListenerAdapter {
    @Override
    public void driverAdded(final TopologyEvent event) {
      TopologyDriver driver = event.getDriver();
      JmxInitializer jinit = new JmxInitializer(driver);
      synchronized(initializerMap) {
        initializerMap.put(driver.getUuid(), jinit);
      }
      new Thread(jinit).start();
    }

    @Override
    public void driverRemoved(final TopologyEvent event) {
      String uuid = event.getDriver().getUuid();
      if (uuid != null) {
        JmxInitializer jinit = null;
        synchronized(initializerMap) {
          jinit = initializerMap.remove(uuid);
        }
        if (jinit != null) jinit.setStopped(true);
      }
    }
  }

  /**
   * Initializer running in a separate thread for each driver, until it gets a working (connected) proxy to the job management MBean.
   */
  private class JmxInitializer extends ThreadSynchronization implements Runnable {
    /**
     * The driver to connect to.
     */
    final TopologyDriver driver;

    /**
     * Create this initializer witht he specified driver.
     * @param driver the driver to connect to.
     */
    JmxInitializer(final TopologyDriver driver) {
      this.driver = driver;
    }

    @Override
    public void run() {
      if (debugEnabled) log.debug("starting jmx intializer for " + driver);
      boolean done = false;
      while (!done && !isStopped()) {
        try {
          DriverJobManagementMBean mbean = driver.getJobManager();
          if (mbean != null) {
            done = true;
            mbean.addNotificationListener(AbstractJobNotificationsHandler.this, null, null);
            if (debugEnabled) log.debug("registered jmx listener for " + driver);
            synchronized(initializerMap) {
              initializerMap.remove(driver.getUuid());
            }
          } else goToSleep(10L);
        } catch (Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }
}
