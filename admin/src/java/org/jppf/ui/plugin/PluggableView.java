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

package org.jppf.ui.plugin;

import javax.swing.JComponent;

import org.jppf.client.monitoring.jobs.JobMonitor;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.ui.monitoring.data.StatsHandler;

/**
 * This is the interface for user-defined view added to the administration console.
 * @author Laurent Cohen
 * @since 5.0
 */
public abstract class PluggableView {
  /**
   * The {@link TopologyManager} associated with the administration console.
   */
  private TopologyManager topologyManager;
  /**
   * The object which monitors and maintains a representation of the jobs hierarchy.
   */
  private JobMonitor jobMonitor;

  /**
   * Get the GUI component which contains the view.
   * @return a {@link JComponent} instance.
   */
  public abstract JComponent getUIComponent();

  /**
   * Get the {@link TopologyManager} associated with the administration console.
   * @return a {@link TopologyManager} object.
   */
  public final TopologyManager getTopologyManager() {
    return topologyManager;
  }

  /**
   * Set the {@link TopologyManager} associated with the administration console.
   * @param topologyManager a {@link TopologyManager} object.
   * @exclude
   */
  final void setTopologyManager(final TopologyManager topologyManager) {
    this.topologyManager = topologyManager;
  }

  /**
   * Get the {@link JobMonitor} associated with the administration console.
   * @return a {@link JobMonitor} object.
   * @since 5.1
   */
  public final JobMonitor getJobMonitor() {
    return jobMonitor;
  }

  /**
   * Set the {@link JobMonitor} associated with the administration console.
   * @param jobMonitor a {@link JobMonitor} object.
   * @since 5.1
   */
  final void setJobMonitor(final JobMonitor jobMonitor) {
    this.jobMonitor = jobMonitor;
  }

  /**
   * Determine whether IP addresses are shown in the console, instead of host names.
   * @return {@code true} if IP adresses are displayed, {@code false} otherwise.
   * @since 5.1
   */
  public final boolean isShowIP() {
    return StatsHandler.getInstance().getShowIPHandler().isShowIP();
  }
}
