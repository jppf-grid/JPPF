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
package org.jppf.ui.monitoring.node.actions;

import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.load.balancer.LoadBalancingInformation;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This allows to update the selected driver's load-balancing settings.
 */
public class LoadBalancingAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LoadBalancingAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement panel = null;

  /**
   * Initialize this action.
   */
  public LoadBalancingAction() {
    setupIcon("/org/jppf/ui/resources/balance.png");
    setupNameAndTooltip("load.balancing.settings");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * This method sets the enabled state to true if at list one driver is selected in the tree.
   * @param selectedElements a list of objects.
   * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    for (Object o: selectedElements) {
      if (o instanceof AbstractTopologyComponent) {
        final AbstractTopologyComponent data = (AbstractTopologyComponent) o;
        if (data.isDriver()) {
          setEnabled(true);
          return;
        }
      }
    }
    setEnabled(false);
  }

  /**
   * Perform the action.
   * @param event encapsulates the source of the event and additional information.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    final AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    if (selectedElements.isEmpty()) return;
    try {
      final TopologyDriver driver = (TopologyDriver) selectedElements.get(0);
      panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/LoadBalancingSettings.xml");
      final JButton applyBtn = (JButton) panel.findFirstWithName("Apply").getUIComponent();
      final JButton refreshBtn = (JButton) panel.findFirstWithName("Refresh").getUIComponent();
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("load.balancing.caption", driver.getDisplayName()), false);
      dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/balance.png").getImage());
      final Action applyAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) { doApply(driver); }
      };
      applyBtn.addActionListener(applyAction);
      final Action refreshAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) { doRefresh(driver); }
      };
      refreshBtn.addActionListener(refreshAction);
      final Action closeAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      };
      setKeyAction(applyBtn, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), closeAction, "close");
      dialog.getContentPane().add(panel.getUIComponent());
      doRefresh(driver);
      dialog.pack();
      dialog.setLocationRelativeTo(null);
      dialog.setLocation(location);
      dialog.setSize(500, 500);
      dialog.setVisible(true);
    } catch(final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Perform the action.
   * @param driver the driver to apply the settings to.
   */
  private void doApply(final TopologyDriver driver) {
    final String params = (String) ((AbstractOption) panel.findFirstWithName("LoadBalancingParameters")).getValue();
    final String algo = (String) ((AbstractOption) panel.findFirstWithName("Algorithm")).getValue();
    final JMXDriverConnectionWrapper jmx = driver.getJmx();
    try {
      final TextAreaOption option = (TextAreaOption) panel.findFirstWithName("/LoadBalancingMessages");
      final String msg = jmx.changeLoadBalancerSettings(algo, new TypedProperties().fromString(params));
      if (msg != null) option.append(LocalizationUtils.getLocalized("org.jppf.server.i18n.server_messages", msg) + " (" + algo + ")");
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Perform the action.
   * @param driver the driver to refresh the settings from.
   */
  private void doRefresh(final TopologyDriver driver) {
    final AbstractOption paramsOption = (AbstractOption) panel.findFirstWithName("LoadBalancingParameters");
    final ComboBoxOption algoOption = (ComboBoxOption) panel.findFirstWithName("Algorithm");
    final JMXDriverConnectionWrapper jmx = driver.getJmx();
    try {
      final LoadBalancingInformation lbi = jmx.loadBalancerInformation();
      algoOption.setItems(lbi.getAlgorithmNames());
      algoOption.setValue(lbi.getAlgorithm());
      paramsOption.setValue(lbi.getParameters().asString());
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }
}
