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

import java.io.*;

import org.jppf.node.protocol.JPPFDistributedJob;

/**
 * This interface represents the objects that are persisted in a job persistence store.
 * @author Laurent Cohen
 * @since 6.0
 */
public interface PersistenceInfo extends Serializable {
  /**
   * Get the job uuid.
   * @return the uuid of the related job as a string.
   */
  String getJobUuid();

  /**
   * Get the related job information.
   * @return a {@link JPPFDistributedJob} instance, or {@code null} if the job is not loaded.
   */
  JPPFDistributedJob getJob();

  /**
   * Get the type of perssted object.
   * @return one of the possible values of the {@link PersistenceObjectType} enum.
   */
  PersistenceObjectType getType();

  /**
   * Get the position of the task in the job. 
   * @return the position of the task in the job, starting at {@code 0},
   * or {@code -1} if the persisted object is neither a {@link PersistenceObjectType#TASK task} nor a {@link PersistenceObjectType#TASK_RESULT task result}.
   */
  int getTaskPosition();

  /**
   * Get an input stream for the persisted object. 
   * @return an {@link InputStream} instance, or {@code null} if the object has not yet been loaded.
   * @throws Exception if any error occurs.
   */
  InputStream getInputStream() throws Exception;

  /**
   * Get the size in bytes of the persisted object. 
   * @return the size of the persisted data, or {@code -1} if the size is not known.
   */
  int getSize();
}
