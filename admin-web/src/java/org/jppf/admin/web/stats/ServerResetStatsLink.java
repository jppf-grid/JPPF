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

package org.jppf.admin.web.stats;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.auth.JPPFRoles;
import org.jppf.admin.web.utils.AbstractActionLink;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 * Reset statistics button in the server statistics page.
 * @author Laurent Cohen
 */
public class ServerResetStatsLink extends AbstractActionLink {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(ServerResetStatsLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * 
   */
  public ServerResetStatsLink() {
    super("stats.server_reset_stats", Model.of("Reset statistics"), "server_reset_stats.gif");
    setEnabled(JPPFWebSession.get().getRoles().hasRole(JPPFRoles.MANAGER));
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on server reset stats");
    JPPFWebSession session = JPPFWebSession.get();
    TopologyDriver driver = session.getCurrentDriver();
    try {
      JMXDriverConnectionWrapper jmx =  driver.getJmx();
      if ((jmx != null) && jmx.isConnected()) jmx.resetStatistics();
    } catch (Exception e) {
      log.warn(e.getMessage(), e);
    }
    StatisticsPage page = (StatisticsPage) target.getPage();
    target.add(page.getTablesContainer());
  }
}
