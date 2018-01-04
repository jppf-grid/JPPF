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

package org.jppf.admin.web.topology.loadbalancing;

import java.util.List;

import javax.swing.tree.DefaultMutableTreeNode;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.jppf.admin.web.JPPFWebSession;
import org.jppf.admin.web.topology.*;
import org.jppf.admin.web.utils.*;
import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * Implementation of the load-balancing.
 * @author Laurent Cohen
 */
public class LoadBalancingLink extends AbstractModalLink<LoadBalancingForm> {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LoadBalancingLink.class);

  /**
   * @param form .
   */
  public LoadBalancingLink(final Form<String> form) {
    super(TopologyConstants.SERVER_LOAD_BALANCING_ACTION, Model.of("Load balancing"), "balance.png", LoadBalancingPage.class, form);
    modal.setInitialWidth(565);
    modal.setInitialHeight(500);
  }

  @Override
  protected LoadBalancingForm createForm() {
    return new LoadBalancingForm(modal, new Runnable() { @Override public void run() { doOK(); } });
  }

  /**
   * Called when the ok button is closed.
   */
  private void doOK() {
    final JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TopologyTreeData data = session.getTopologyData();
    final List<TopologyDriver> selectedDrivers = TopologyTreeData.getSelectedDrivers(data.getSelectedTreeNodes());
    if (!selectedDrivers.isEmpty()) {
      try {
        final TopologyDriver driver = selectedDrivers.get(0);
        final TypedProperties props = new TypedProperties().fromString(modalForm.getProperties());
        driver.getJmx().changeLoadBalancerSettings(modalForm.getAlgorithm(), props);
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }

  @Override
  public void onClick(final AjaxRequestTarget target) {
    final JPPFWebSession session = (JPPFWebSession) getPage().getSession();
    final TopologyTreeData data = session.getTopologyData();
    final List<TopologyDriver> selectedDrivers = TopologyTreeData.getSelectedDrivers(data.getSelectedTreeNodes());
    if (!selectedDrivers.isEmpty()) {
      try {
        final TopologyDriver driver = selectedDrivers.get(0);
        final LoadBalancingInformation lbi = driver.getJmx().loadBalancerInformation();
        modalForm.setAlgorithm(lbi.getAlgorithm());
        modalForm.setAlgorithmChoices(lbi.getAlgorithmNames());
        modalForm.setProperties(lbi.getParameters().asString());
        modalForm.setDriverName(driver.getDisplayName());
      } catch(final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    super.onClick(target);
  }

  /**
   * 
   */
  public static class Action extends AbstractManagerRoleAction {
    @Override
    public void setEnabled(final List<DefaultMutableTreeNode> selected) {
      enabled = isDriverSelected(selected);
    }
  }
}
