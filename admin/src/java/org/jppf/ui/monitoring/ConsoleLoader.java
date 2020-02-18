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
package org.jppf.ui.monitoring;

import org.slf4j.*;

/**
 * This class provides a graphical interface for monitoring the status and health of the JPPF servers and nodes.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes.
 * @author Laurent Cohen
 */
public class ConsoleLoader {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ConsoleLoader.class);

  /**
   * Start the console UI, optionally with the charting components.
   * @param args not used.
   */
  public static void main(final String...args) {
    try {
      start();
      log.info("terminating");
    } catch(final Exception e) {
      e.printStackTrace();
      log.error(e.getMessage(), e);
      System.exit(1);
    }
  }

  /**
   * Check if the charting library classes are avaialble from the classpath.
   * @return <code>true</code> if the classes are available, <code>false</code> otherwise.
   */
  private static boolean checkChartClassesAvailable() {
    try {
      Class.forName("org.jfree.chart.ChartFactory");
      return true;
    } catch(final ClassNotFoundException e) {
      log.error(e.getMessage(), e);
      return false;
    }
  }

  /**
   * Start the console UI.
   * @throws Exception if any error occurs.
   */
  private static void start() throws Exception {
    final boolean present = checkChartClassesAvailable();
    final String xmlPath = "org/jppf/ui/options/xml/JPPFAdminTool" + (present ? "" : "NoCharts") + ".xml";
    ConsoleLauncher.main(xmlPath, "file");
  }
}
