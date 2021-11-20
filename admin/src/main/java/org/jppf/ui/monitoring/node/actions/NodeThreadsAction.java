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
import org.jppf.management.*;
import org.jppf.management.forwarding.NodeForwardingMBean;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.collections.CollectionMap;
import org.jppf.utils.concurrent.ThreadUtils;
import org.slf4j.*;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class NodeThreadsAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeThreadsAction.class);
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
  private int nbThreads = 1;
  /**
   * Threads priority.
   */
  private int priority = Thread.NORM_PRIORITY;

  /**
   * Initialize this action.
   */
  public NodeThreadsAction() {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    setupIcon("/org/jppf/ui/resources/threads.gif");
    setupNameAndTooltip("update.threads");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    setEnabled(dataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event not used.
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    final AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    if (selectedElements.isEmpty()) return;
    try {
      panel = loadWithPreferences("org/jppf/ui/options/xml/NodeThreadPoolPanel.xml");
      if (dataArray.length == 1) {
        nbThreads = ((TopologyNode) dataArray[0]).getNodeState().getThreadPoolSize();
        priority = ((TopologyNode) dataArray[0]).getNodeState().getThreadPriority();
      }
      ((AbstractOption) panel.findFirstWithName("nbThreads")).setValue(nbThreads);
      ((AbstractOption) panel.findFirstWithName("threadPriority")).setValue(priority);

      final JButton okBtn = (JButton) panel.findFirstWithName("/nodeThreadsOK").getUIComponent();
      final JButton cancelBtn = (JButton) panel.findFirstWithName("/nodeThreadsCancel").getUIComponent();
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("NodeThreadPoolPanel.label"), false);
      dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/threads.gif").getImage());
      final AbstractAction okAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          disposeDialog(dialog);
          doOK();
        }
      };
      okBtn.addActionListener(okAction);
      final AbstractAction cancelAction = new AbstractAction() {
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
   */
  private void doOK() {
    savePreferences(panel);
    final AbstractOption nbThreadsOption = (AbstractOption) panel.findFirstWithName("nbThreads");
    final AbstractOption priorityOption = (AbstractOption) panel.findFirstWithName("threadPriority");
    nbThreads = ((Number) nbThreadsOption.getValue()).intValue();
    priority = ((Number) priorityOption.getValue()).intValue();
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        final CollectionMap<TopologyDriver, String> map = getDriverMap();
        for (final Map.Entry<TopologyDriver, Collection<String>> entry: map.entrySet()) {
          try {
            final NodeForwardingMBean forwarder = entry.getKey().getForwarder();
            if (forwarder == null) continue;
            final NodeSelector selector = new UuidSelector(entry.getValue());
            forwarder.updateThreadPoolSize(selector, nbThreads);
            forwarder.updateThreadsPriority(selector, priority);
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    ThreadUtils.startThread(r, "NodeThreadsOK");
  }
}
