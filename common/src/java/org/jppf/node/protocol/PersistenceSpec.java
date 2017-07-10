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

package org.jppf.node.protocol;

import java.io.Serializable;

/**
 * This class defines whether a job should be persisted and the behavior of the persistence facility upon recovery.
 * @author Laurent Cohen
 * @since 6.0
 */
public class PersistenceSpec implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Whether the job is persisted in the driver.
   */
  private boolean persistent = false;
  /**
   * Whether the driver should automatically execute the job, then delete it from the persistence store, after a restart.
   */
  private boolean autoExecuteOnRestart = false;
  /**
   * Whether the persisteed job should be deleted from the persistence store when it terminates.
   */
  private boolean deleteOnCompletion = true;

  /**
   * Determine whether the job is persisted in the driver.
   * The default value of this attribute is {@code false}.
   * @return {@code true} if the job is persisted, {@code false} otherwise.
   */
  public boolean isPersistent() {
    return persistent;
  }

  /**
   * Specify whether the job is persisted in the driver.
   * @param persistent {@code true} if the job is to be persisted, {@code false} otherwise.
   * @return this {@code PersistenceSpec} instance, for method call chaining.
   */
  public PersistenceSpec setPersistent(final boolean persistent) {
    this.persistent = persistent;
    return this;
  }

  /**
   * Determine whether the driver should automatically execute the job, then delete it from the persistence store, after a restart.
   * The default value of this attribute is {@code false}.
   * @return {@code true} if the job is automatically executed upon recovery, {@code false} otherwise.
   */
  public boolean isAutoExecuteOnRestart() {
    return autoExecuteOnRestart;
  }

  /**
   * Specify whether the driver should automatically execute the job, then delete it from the persistence store, after a restart.
   * @param autoExecuteOnRestart {@code true} if the job is to be automatically executed upon recovery, {@code false} otherwise.
   * @return this {@code PersistenceSpec} instance, for method call chaining.
   */
  public PersistenceSpec setAutoExecuteOnRestart(final boolean autoExecuteOnRestart) {
    this.autoExecuteOnRestart = autoExecuteOnRestart;
    return this;
  }

  /**
   * Determine whether the persisteed job should be deleted from the persistence store when it terminates.
   * The default value of this attribute is {@code true}.
   * @return {@code true} if the job should be deleted from persistence, {@code false} otherwise.
   */
  public boolean isDeleteOnCompletion() {
    return deleteOnCompletion;
  }

  /**
   * Determine whether the persisteed job should be deleted from the persistence store when it terminates.
   * @param deleteOnCompletion {@code true} if the job should be deleted from persistence, {@code false} otherwise.
   * @return this {@code PersistenceSpec} instance, for method call chaining.
   */
  public PersistenceSpec setDeleteOnCompletion(final boolean deleteOnCompletion) {
    this.deleteOnCompletion = deleteOnCompletion;
    return this;
  }
}
