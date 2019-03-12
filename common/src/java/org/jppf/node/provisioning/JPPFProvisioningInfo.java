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

package org.jppf.node.provisioning;

import java.io.Serializable;
import java.util.*;

/**
 * This class provides information on a slave node, sent as a notification whenever a slave is started or stopped.
 * @author Laurent Cohen
 * @since 6.1
 */
public class JPPFProvisioningInfo implements Serializable {
  /**
   * The uuid of the master node that launched the slave.
   */
  private final String masterUuid;
  /**
   * The id of the slave, relative to the master.
   */
  private final int slaveId;
  /**
   * The slave node process exit code.
   */
  private final int exitCode;
  /**
   * The command line used to start the slave node process.
   */
  private final List<String> launchCommand;
  
  /**
   * Initialize this provisiong information.
   * @param masterUuid the uuid of the master node that launched the slave.
   * @param slaveId the id of the slave, relative to the master.
   * @param exitCode the slave node process exit code.
   * @param launchCommand the command line used to start the slave node process.
   * @exclude
   */
  public JPPFProvisioningInfo(final String masterUuid, final int slaveId, final int exitCode, final List<String> launchCommand) {
    this.masterUuid = masterUuid;
    this.slaveId = slaveId;
    this.exitCode = exitCode;
    this.launchCommand = new ArrayList<>(launchCommand);
  }

  /**
   * Get the command line used to start the slave node process.
   * @return a list containing all the command line arguments.
   */
  public List<String> getLaunchCommand() {
    return launchCommand;
  }

  /**
   * Get the uuid of the master node that launched the slave.
   * @return a string holding the master node's uuid.
   */
  public String getMasterUuid() {
    return masterUuid;
  }

  /**
   * Get the id of the slave, relative to the master.
   * @return an integer id, unique only for the master node.
   */
  public int getSlaveId() {
    return slaveId;
  }

  /**
   * Get the slave node process exit code.
   * @return the slave's exit code, or {@code -1} is the slave process is not yet terminated. 
   */
  public int getExitCode() {
    return exitCode;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("masterUuid=").append(masterUuid)
      .append(", slaveId=").append(slaveId)
      .append(", exitCode=").append(exitCode)
      .append(", launchCommand=").append(launchCommand)
      .append(']').toString();
  }
}
