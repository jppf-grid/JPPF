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

package org.jppf.admin.web;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.jppf.client.monitoring.topology.TopologyManager;

/**
 * A the header panel.
 * @author Laurent Cohen
 */
public class FooterPanel extends Panel {
  /**
   * 
   */
  public FooterPanel() {
    super("jppf.footer");
    int nbServers = 0, nbNodes = 0;
    String user = JPPFWebSession.getSignedInUser();
    WebMarkupContainer gridInfo = new WebMarkupContainer("jppf.footer.grid.info");
    add(gridInfo);
    if (user != null) {
      TopologyManager mgr =JPPFWebConsoleApplication.get().getTopologyManager();
      nbServers = mgr.getDriverCount();
      nbNodes = mgr.getNodeCount();
    }
    gridInfo.add(new Label("jppf.footer.servers.value", Model.of(nbServers)));
    gridInfo.add(new Label("jppf.footer.nodes.value", Model.of(nbNodes)));
    gridInfo.setVisible(user != null);
  }
}
