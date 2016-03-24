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

package org.jppf.dotnet;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * This class wraps a .Net job monitoring listener to which job monitoring events are delegated.
 * @author Laurent Cohen
 * @since 5.1
 * @exclude
 */
public class DotnetJobMonitoringListenerWrapper extends AbstractDotnetListenerWrapper implements JobMonitoringListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DotnetJobMonitoringListenerWrapper.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Initialize this wrapper with the specified proxy to a .Net listener.
   * @param dotnetListener a proxy to a .Net listener.
   */
  public DotnetJobMonitoringListenerWrapper(final system.Object dotnetListener) {
    super(debugEnabled, dotnetListener, "DriverAdded", "DriverRemoved", "JobAdded", "JobRemoved", "JobUpdated", "JobDispatchAdded", "JobDispatchRemoved");
  }

  @Override
  public void driverAdded(JobMonitoringEvent event) {
    if (debugEnabled) log.debug(event.toString());
    delegate(event, "DriverAdded");
  }

  @Override
  public void driverRemoved(JobMonitoringEvent event) {
    if (debugEnabled) log.debug(event.toString());
    delegate(event, "DriverRemoved");
  }

  @Override
  public void jobAdded(JobMonitoringEvent event) {
    if (debugEnabled) log.debug(event.toString());
    delegate(event, "JobAdded");
  }

  @Override
  public void jobRemoved(JobMonitoringEvent event) {
    if (debugEnabled) log.debug(event.toString());
    delegate(event, "JobRemoved");
  }

  @Override
  public void jobUpdated(JobMonitoringEvent event) {
    if (debugEnabled) log.debug(event.toString());
    delegate(event, "JobUpdated");
  }

  @Override
  public void jobDispatchAdded(JobMonitoringEvent event) {
    delegate(event, "JobDispatchAdded");
  }

  @Override
  public void jobDispatchRemoved(JobMonitoringEvent event) {
    if (debugEnabled) log.debug(event.toString());
    delegate(event, "JobDispatchRemoved");
  }
}
