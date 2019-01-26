/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.node.protocol;

import org.jppf.utils.TraversalList;

/**
 *
 * @author Laurent Cohen
 */
public abstract class AbstractNotificationBundle extends JPPFJobMetadata implements TaskBundle {
  @Override
  public String getName() {
    return null;
  }

  @Override
  public String getUuid() {
    return null;
  }

  @Override
  public JobSLA getSLA() {
    return null;
  }

  @Override
  public JobMetadata getMetadata() {
    return this;
  }

  @Override
  public int getTaskCount() {
    return 0;
  }

  @Override
  public TraversalList<String> getUuidPath() {
    return null;
  }

  @Override
  public void setUuidPath(final TraversalList<String> uuidPath) {
  }

  @Override
  public long getNodeExecutionTime() {
    return 0;
  }

  @Override
  public void setNodeExecutionTime(final long nodeExecutionTime) {
  }

  @Override
  public void setTaskCount(final int taskCount) {
  }

  @Override
  public void setInitialTaskCount(final int initialTaskCount) {
  }

  @Override
  public TaskBundle copy() {
    return null;
  }

  @Override
  public long getExecutionStartTime() {
    return 0;
  }

  @Override
  public void setExecutionStartTime(final long executionStartTime) {
  }

  @Override
  public int getInitialTaskCount() {
    return 0;
  }

  @Override
  public void setSLA(final JobSLA jobSLA) {
  }

  @Override
  public void setName(final String name) {
  }

  @Override
  public void setMetadata(final JobMetadata jobMetadata) {
  }

  @Override
  public void setUuid(final String jobUuid) {
  }

  @Override
  public int getCurrentTaskCount() {
    return 0;
  }

  @Override
  public void setCurrentTaskCount(final int currentTaskCount) {
  }

  @Override
  public boolean isPending() {
    return false;
  }

  @Override
  public boolean isRequeue() {
    return false;
  }

  @Override
  public void setRequeue(final boolean requeue) {
  }

  @Override
  public int getDriverQueueTaskCount() {
    return 0;
  }

  @Override
  public void setDriverQueueTaskCount(final int driverQueueTaskCount) {
  }

  @Override
  public boolean isHandshake() {
    return false;
  }

  @Override
  public void setHandshake(final boolean handshake) {
  }
}
