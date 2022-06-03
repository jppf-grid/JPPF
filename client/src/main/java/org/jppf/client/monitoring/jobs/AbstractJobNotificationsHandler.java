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

package org.jppf.client.monitoring.jobs;

import java.util.*;

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
   * The default maximum number of pending JMX notifications.
   */
  private static final int DEFAULT_MAX_NOTIFICATIONS = 5_000;
  /**
   * the associated job monitor.
   */
  final JobMonitor monitor;
  /**
   * Used to queue the JMX notifications in the same order they are received with a minimum of interruption.
   */
  final QueueHandler<JobNotification> notificationsQueue;
  /**
   * Mapping of driver uuids to takss initializing and regstering a driver job management notifications listener.
   */
  final Map<String, JmxInitializer> initializerMap = new HashMap<>();
  /**
   * Listens for drivers joining or leaving the grid.
   */
  final DriverListener driverListener;

  /**
   * Initialize with the specified job monitor.
   * @param monitor an instance of {@link JobMonitor}.
   */
  AbstractJobNotificationsHandler(final JobMonitor monitor) {
    if (debugEnabled) log.debug("initializing {} with {}", getClass().getSimpleName(), monitor);
    this.monitor = monitor;
    monitor.getTopologyManager().addTopologyListener(driverListener = new DriverListener());
    final int capacity = monitor.getTopologyManager().getJPPFClient().getConfig().getInt("jppf.job.monitor.max.notifications", DEFAULT_MAX_NOTIFICATIONS);
    final int cap = (capacity <= 0) ? DEFAULT_MAX_NOTIFICATIONS : capacity;
    notificationsQueue = QueueHandler.<JobNotification>builder()
      .named("JobNotificationsHandler")
      .withCapacity(cap)
      .handlingElementsAs(this::handleNotificationAsync)
      .handlingPeakSizeAs(n -> {
        if (debugEnabled && (n >= cap)) log.debug("maximum peak job notifications: {}", n);
      })
      .usingSingleDequuerThread()
      .build(); 
  }

  @Override
  public void handleNotification(final Notification notification, final Object handback) {
    if (log.isTraceEnabled()) log.trace("got jmx notification: {}", notification);
    try {
      notificationsQueue.put((JobNotification) notification);
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Handle a notification asynchronously.
   * @param notif the notification to handle.
   */
  abstract void handleNotificationAsync(final JobNotification notif);

  @Override
  public void close() {
    monitor.getTopologyManager().removeTopologyListener(driverListener);
    notificationsQueue.close();
    synchronized(initializerMap) {
      initializerMap.clear();
    }
  }

  /**
   * Listens to driver added/removed events.
   */
  private class DriverListener extends TopologyListenerAdapter {
    @Override
    public void driverAdded(final TopologyEvent event) {
      final TopologyDriver driver = event.getDriver();
      final JmxInitializer jinit = new JmxInitializer(driver);
      synchronized(initializerMap) {
        initializerMap.put(driver.getUuid(), jinit);
      }
      ThreadUtils.startDaemonThread(jinit, driver.toString());
    }

    @Override
    public void driverRemoved(final TopologyEvent event) {
      final String uuid = event.getDriver().getUuid();
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
          final DriverJobManagementMBean mbean = driver.getJobManager();
          if (mbean != null) {
            done = true;
            mbean.addNotificationListener(AbstractJobNotificationsHandler.this, null, null);
            if (debugEnabled) log.debug("registered jmx listener for " + driver);
            synchronized(initializerMap) {
              initializerMap.remove(driver.getUuid());
            }
          } else goToSleep(10L);
        } catch (final Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }
}
