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

package org.jppf.job.persistence;

import java.io.InputStream;

import org.jppf.io.DataLocation;
import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * Implementation of the {@link PersistenceInfo} interface.
 * @author Laurent Cohen
 * @exclude
 */
public class PersistenceInfoImpl implements PersistenceInfo {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The job uuid.
   */
  private final String jobUuid;
  /**
   * The job information, if applicable.
   */
  private final JPPFDistributedJob job;
  /**
   * The type of persisted object.
   */
  private final PersistenceObjectType type;
  /**
   * The position of the task in the job, if applicable.
   */
  private final int taskPosition;
  /**
   * The location of the persisted object's data.
   */
  private final DataLocation dataLocation;

  /**
   * Initialize this persistence information object.
   * @param jobUuid the job uuid.
   * @param job the job information, if applicable.
   * @param type the type of persisted object.
   * @param taskPosition the position of the task in the job, if applicable.
   * @param dataLocation the location of the persisted object's data.
   */
  public PersistenceInfoImpl(final String jobUuid, final JPPFDistributedJob job, final PersistenceObjectType type, final int taskPosition, final DataLocation dataLocation) {
    this.jobUuid = jobUuid;
    this.job = job;
    this.type = type;
    this.taskPosition = taskPosition;
    this.dataLocation = dataLocation;
  }

  @Override
  public String getJobUuid() {
    return jobUuid;
  }

  @Override
  public JPPFDistributedJob getJob() {
    return job;
  }

  @Override
  public PersistenceObjectType getType() {
    return type;
  }

  @Override
  public int getTaskPosition() {
    return taskPosition;
  }

  @Override
  public InputStream getInputStream() throws Exception {
    return dataLocation == null ? null : dataLocation.copy().getInputStream();
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("type=").append(type)
      .append(", taskPosition=").append(taskPosition)
      .append(", job=").append(job)
      .append(", jobUuid=").append(jobUuid)
      .append(']').toString();
  }

  @Override
  public int getSize() {
    return dataLocation == null ? -1 : dataLocation.getSize();
  }
}
