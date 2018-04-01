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

package org.jppf.ui.console;

import javax.swing.JComponent;

import org.jppf.client.JPPFClient;
import org.jppf.client.monitoring.jobs.JobMonitor;
import org.jppf.client.monitoring.topology.TopologyManager;
import org.jppf.ui.monitoring.ConsoleLauncher;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to launch the JPPF admininstration console
 * and to embed it within an external GUI application.
 * @author Laurent Cohen
 * @since 5.0
 */
public class JPPFAdminConsole {
  /**
   * Logger for this class.
   */
  static final Logger log = LoggerFactory.getLogger(JPPFAdminConsole.class);
  /**
   * Whether the charts library is present in the classpath.
   * @exclude
   */
  public static final boolean HAS_CHARTS = checkChartsPresent();
  /**
   * Path to the UI descriptor file when running with charts.
   * @exclude
   */
  public static final String CONSOLE_WITH_CHARTS = "org/jppf/ui/options/xml/JPPFAdminTool.xml";
  /**
   * Path to the UI descriptor file when running with charts.
   * @exclude
   */
  public static final String CONSOLE_WITHOUT_CHARTS = "org/jppf/ui/options/xml/JPPFAdminToolNoCharts.xml";

  /**
   * Launch the JPPF administration console.
   * @param args the command-line arguments are not used.
   */
  public static void main(final String[] args) {
    ConsoleLauncher.main(HAS_CHARTS ? CONSOLE_WITH_CHARTS : CONSOLE_WITHOUT_CHARTS, "file");
  }

  /**
   * Get the administration console as a {@link JComponent} that can be added to any Swing container.
   * This method always returns the same {@code JComponent} instance.
   * @return a {@link JComponent} enclosing the JPPF administration console.
   */
  public static JComponent getAdminConsole() {
    return ConsoleLauncher.loadAdminConsole();
  }

  /**
   * Get the JPPF client used by the admin console.
   * This method always returns the same {@code JPPFClient} instance.
   * @return a {@link JPPFClient} instance used by the admin console.
   */
  public static JPPFClient getJPPFClient() {
    final TopologyManager manager = getTopologyManager();
    return (manager == null) ? null : manager.getJPPFClient();
  }

  /**
   * Get the topology manager used by the admin console.
   * This method always returns the same {@code TopologyManager} instance.
   * @return a {@link TopologyManager} instance used by the admin console.
   */
  public static TopologyManager getTopologyManager() {
    final StatsHandler handler = StatsHandler.getInstance();
    return (handler == null) ? null : handler.getTopologyManager();
  }

  /**
   * Get the job monitor used by the admin console.
   * This method always returns the same {@code JobMonitor} instance.
   * @return a {@link JobMonitor} instance used by the admin console.
   */
  public static JobMonitor getJobMonitor() {
    final StatsHandler handler = StatsHandler.getInstance();
    return (handler == null) ? null : handler.getJobMonitor();
  }

  /**
   * Determines whether the charts library can be found in the class path.
   * @return true if the charts library is found, false ortherwise.
   */
  private static boolean checkChartsPresent() {
    try {
      Class.forName("org.jfree.chart.JFreeChart", true, ReflectionHelper.getCurrentClassLoader());
      return true;
    } catch(final Exception|LinkageError e) {
      log.info("The charts library coud not be found in the classpath, starting the console without charts [error message: {}]", ExceptionUtils.getMessage(e));
      return false;
    }
  }
}
