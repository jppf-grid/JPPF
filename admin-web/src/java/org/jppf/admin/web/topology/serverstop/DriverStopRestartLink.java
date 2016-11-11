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

package org.jppf.admin.web.topology.serverstop;

import java.util.*;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.tabletree.AbstractModalLink;
import org.jppf.admin.web.topology.*;
import org.jppf.admin.web.topology.systeminfo.SystemInfoLink;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.utils.LoggingUtils;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
//@AuthorizeActions(actions = { @AuthorizeAction(action="RENDER", roles=JPPFRoles.MANAGER) })
public class DriverStopRestartLink extends AbstractModalLink<DriverStopRestartForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(SystemInfoLink.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * @param form .
   */
  public DriverStopRestartLink(final Form<String> form) {
    super(TopologyConstants.SERVER_STOP_RESTART_ACTION, Model.of("Server stop/restart"), "server_restart.gif", DriverStopRestartPage.class, form);
    modal.setInitialWidth(350);
    modal.setInitialHeight(180);
  }

  @Override
  protected DriverStopRestartForm createForm() {
    return new DriverStopRestartForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }

  /**
   * Called when the ok button is closed.
   */
  private void doOK() {
    JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TopologyTreeData data = session.getTopologyData();
    List<TopologyDriver> selectedDrivers = TopologyTreeData.getSelectedDrivers(data.getSelectedTreeNodes());
    if (!selectedDrivers.isEmpty()) {
      long shutdownDelay = modalForm.getShutdownDelay();
      long restartDelay = modalForm.isRestart() ? modalForm.getRestartDelay() : -1L;
      Set<String> uuids = new HashSet<>();
      List<JMXDriverConnectionWrapper> list = new ArrayList<>();
      for (TopologyDriver driver: selectedDrivers) {
        if (!uuids.contains(driver.getUuid())) {
          uuids.add(driver.getUuid());
          list.add(driver.getJmx());
        }
      }
      for (JMXDriverConnectionWrapper jmx: list) {
        try {
          jmx.restartShutdown(shutdownDelay, restartDelay);
        } catch(Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    }
  }
}
