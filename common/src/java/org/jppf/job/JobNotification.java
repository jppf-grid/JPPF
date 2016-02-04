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

package org.jppf.job;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.*;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.server.job.management.DriverJobManagementMBean;

/**
 * Instances of this class represent events emitted by a JPPFJobManager.
 * @author Laurent Cohen
 */
public class JobNotification extends Notification {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Count of instances of this class.
   */
  private static final AtomicLong INSTANCE_COUNT = new AtomicLong(0L);
  /**
   * Count of instances of this class.
   */
  private static final ObjectName SOURCE = createObjectName();
  /**
   * Information about a node.
   */
  private final JPPFManagementInfo nodeInfo;
  /**
   * The type of this job event.
   */
  private final JobEventType eventType;
  /**
   * Information about the job.
   */
  private final JobInformation jobInfo;

  /**
   * Initialize this event with the specified job and node information.
   * @param driverUuid the type of this job event.
   * @param eventType the type of this job event.
   * @param jobInfo information about the job.
   * @param nodeInfo information about the node.
   * @param timestamp the creation timestamp for this event.
   */
  public JobNotification(final String driverUuid, final JobEventType eventType, final JobInformation jobInfo, final JPPFManagementInfo nodeInfo, final long timestamp) {
    super("jobEvent", SOURCE, INSTANCE_COUNT.incrementAndGet());
    setUserData(driverUuid);
    this.eventType = eventType;
    this.jobInfo = jobInfo;
    this.nodeInfo = nodeInfo;
    setTimeStamp(timestamp);
  }

  /**
   * Get the information about the job.
   * @return a <code>JobInformation</code> instance.
   */
  public JobInformation getJobInformation() {
    return jobInfo;
  }

  /**
   * Get the information about the node.
   * @return a <code>NodeManagementInfo</code> instance.
   */
  public JPPFManagementInfo getNodeInfo() {
    return nodeInfo;
  }

  /**
   * Get the type of this job event.
   * @return a <code>JobManagerEventType</code> enum value.
   */
  public JobEventType getEventType() {
    return eventType;
  }

  /**
   * Get the uuid of the driver that sent this notification.
   * @return the driver uuid as a string.
   */
  public String getDriverUuid() {
    return (String) getUserData();
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("eventType=").append(eventType);
    sb.append(", jobInfo=").append(jobInfo);
    sb.append(", nodeInfo=").append(nodeInfo);
    sb.append(", objectName=").append(getSource());
    sb.append(", sequenceNumber=").append(getSequenceNumber());
    sb.append(", timeStamp=").append(getTimeStamp());
    sb.append(", objectName=").append(getSource());
    sb.append(']');
    return sb.toString();
  }

  /**
   * Create an ObjectName for the job management MBean.
   * @return an instance of {@link ObjectName}.
   */
  private static ObjectName createObjectName() {
    ObjectName name = null;
    try {
      name = new ObjectName(DriverJobManagementMBean.MBEAN_NAME);
    } catch (Exception e) {
    }
    return name;
  }
}
