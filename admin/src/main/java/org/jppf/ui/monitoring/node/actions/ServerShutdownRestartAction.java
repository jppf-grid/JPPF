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
package org.jppf.ui.monitoring.node.actions;

import java.awt.event.ActionEvent;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.slf4j.*;

/**
 * This action stops a server and optionally restarts it after a specified delay.
 */
public class ServerShutdownRestartAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ServerShutdownRestartAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement panel = null;
  /**
   * Number of threads.
   */
  private long shutdownDelay = 1L;
  /**
   * Threads priority.
   */
  private long restartDelay = 0L;

  /**
   * Initialize this action.
   */
  public ServerShutdownRestartAction() {
    setupIcon("/org/jppf/ui/resources/server_restart.gif");
    setupNameAndTooltip("shutdown.restart.driver");
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
    for (final Object o: selectedElements) {
      if (!(o instanceof AbstractTopologyComponent)) continue;
      final AbstractTopologyComponent data = (AbstractTopologyComponent) o;
      if (!data.isNode()) {
        setEnabled(true);
        return;
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
    final List<JMXDriverConnectionWrapper> list = new ArrayList<>();
    for (final Object o: selectedElements) {
      if (!(o instanceof AbstractTopologyComponent)) continue;
      final AbstractTopologyComponent data = (AbstractTopologyComponent) o;
      if (data.isDriver()) list.add(((TopologyDriver) data).getJmx());
    }

    final AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    if (selectedElements.isEmpty()) return;
    try {
      panel = loadWithPreferences("org/jppf/ui/options/xml/DriverShutdownRestartPanel.xml");
      final OptionsHandler.OptionNode optionNode = OptionsHandler.buildPersistenceGraph(panel);
      OptionsHandler.loadPreferences(optionNode, OptionsHandler.getPreferences());
      final JButton okBtn = (JButton) panel.findFirstWithName("driverShutdownRestartOK").getUIComponent();
      final JButton cancelBtn = (JButton) panel.findFirstWithName("serverShutdownRestartCancel").getUIComponent();
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("shutdown.restart.driver.label"), false);
      dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/server_restart.gif").getImage());
      final Action okAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          disposeDialog(dialog);
          doOK(list);
        }
      };
      okBtn.addActionListener(okAction);
      final Action cancelAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          disposeDialog(dialog);
        }
      };
      cancelBtn.addActionListener(cancelAction);
      setOkCancelKeys(panel, okAction, cancelAction);
      readyDialog(dialog, panel.getUIComponent(), location);
    } catch(final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Perform the action.
   * @param driverConnections - the list of driver jmx connections to send the shutdown or restart command to.
   */
  private void doOK(final List<JMXDriverConnectionWrapper> driverConnections) {
    savePreferences(panel);
    AbstractOption option = (AbstractOption) panel.findFirstWithName("Shutdown_delay");
    shutdownDelay = ((Number) option.getValue()).longValue();
    option = (AbstractOption) panel.findFirstWithName("Restart");
    final boolean restart = (Boolean) option.getValue();
    if (restart) {
      option = (AbstractOption) panel.findFirstWithName("Restart_delay");
      restartDelay = ((Number) option.getValue()).longValue();
    } else restartDelay = -1L;
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        for (JMXDriverConnectionWrapper jmx: driverConnections) {
          try {
            jmx.restartShutdown(shutdownDelay, restartDelay);
          } catch(final Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    runAction(r);
  }
}
