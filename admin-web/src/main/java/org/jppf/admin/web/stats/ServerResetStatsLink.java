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

package org.jppf.admin.web.stats;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.auth.JPPFRoles;
import org.jppf.admin.web.utils.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.JMXDriverConnectionWrapper;
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
  static boolean debugEnabled = log.isDebugEnabled();

  /**
   * 
   */
  public ServerResetStatsLink() {
    super("stats.server_reset_stats", Model.of("Reset statistics"), "server_reset_stats.gif");
    final boolean allowed = ((JPPFWebSession.get() != null) && (JPPFWebSession.get().getRoles() != null)) ? JPPFWebSession.get().getRoles().hasRole(JPPFRoles.MANAGER) : true;
    setEnabled(allowed);
    setAction(new AbstractManagerRoleAction() {
      @Override
      public boolean isAuthorized() {
        return allowed;
      } 
    });
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    if (debugEnabled) log.debug("clicked on server reset stats");
    final JPPFWebSession session = JPPFWebSession.get();
    final TopologyDriver driver = session.getCurrentDriver();
    try {
      final JMXDriverConnectionWrapper jmx =  driver.getJmx();
      if ((jmx != null) && jmx.isConnected()) jmx.resetStatistics();
    } catch (final Exception e) {
      log.warn(e.getMessage(), e);
    }
    final StatisticsPage page = (StatisticsPage) target.getPage();
    target.add(page.getTablesContainer());
  }
}
