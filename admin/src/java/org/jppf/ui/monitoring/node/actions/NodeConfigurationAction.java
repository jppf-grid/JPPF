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
package org.jppf.ui.monitoring.node.actions;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class NodeConfigurationAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeConfigurationAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  protected static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement thisPanel = null;
  /**
   * Location at which to display the entry dialog.
   */
  private Point location = null;

  /**
   * Initialize this action.
   */
  public NodeConfigurationAction() {
    BASE = "org.jppf.ui.i18n.NodeDataPage";
    setupIcon("/org/jppf/ui/resources/update.gif");
    setupNameAndTooltip("update.configuration");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    setEnabled(dataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event - not used.
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    thisPanel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/JPPFConfigurationPanel.xml");
    CodeEditorOption textArea = (CodeEditorOption) thisPanel.findFirstWithName("configProperties");
    AbstractTopologyComponent data = dataArray[0];
    textArea.setValue(getPropertiesAsString(data));
    JButton okBtn = (JButton) thisPanel.findFirstWithName("/updateConfigOK").getUIComponent();
    JButton cancelBtn = (JButton) thisPanel.findFirstWithName("/updateConfigCancel").getUIComponent();
    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("nodeConfigurationUpdatePanel.label") + " " + TopologyUtils.getDisplayName(data), false);
    dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/update.gif").getImage());
    AbstractAction okAction = new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        dialog.setVisible(false);
        dialog.dispose();
        doOK();
      }
    };
    okBtn.addActionListener(okAction);
    AbstractAction cancelAction = new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        dialog.setVisible(false);
        dialog.dispose();
      }
    };
    cancelBtn.addActionListener(cancelAction);
    setOkCancelKeys(thisPanel, okAction, cancelAction);
    dialog.getContentPane().add(thisPanel.getUIComponent());
    dialog.pack();
    dialog.setLocationRelativeTo(null);
    if (location != null) dialog.setLocation(location);
    dialog.setVisible(true);
  }

  /**
   * Perform the action.
   */
  private void doOK() {
    CodeEditorOption textArea = (CodeEditorOption) thisPanel.findFirstWithName("configProperties");
    final Map<Object, Object> map = getPropertiesAsMap((String) textArea.getValue());
    final Boolean restart = (Boolean) ((BooleanOption) thisPanel.findFirstWithName("forceRestart")).getValue();
    final Boolean interrupt = (Boolean) ((BooleanOption) thisPanel.findFirstWithName("nodeConfig.interruptIfRunning")).getValue();
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyDriver parent = null;
        try {
          AbstractTopologyComponent data = dataArray[0];
          parent = (TopologyDriver) data.getParent();
          if (parent == null) return;
          parent.getForwarder().updateConfiguration(new UuidSelector(data.getUuid()), map, restart, interrupt);
        } catch(Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    };
    runAction(r);
  }

  /**
   * Obtain the JPPF configuration as a string, one property per line.
   * @param data the topology data from which to get the config.
   * @return the properties as a string.
   */
  public static String getPropertiesAsString(final AbstractTopologyComponent data) {
    StringBuilder sb = new StringBuilder();
    try {
      TopologyDriver parent = (TopologyDriver) data.getParent();
      if (parent == null) return "could not get the parent driver for the selected node";
      Map<String, Object> result = parent.getForwarder().systemInformation(new UuidSelector(data.getUuid()));
      if (result == null) return "could not retrieve system information for the selected node";
      Object o = result.get(data.getUuid());
      if (o == null) return "could not retrieve system information for the selected node";
      else if (o instanceof Exception) throw (Exception) o;
      JPPFSystemInformation info = (JPPFSystemInformation) o;
      TypedProperties props = info.getJppf();
      Set<String> keys = new TreeSet<>();
      for (Map.Entry<Object, Object> entry: props.entrySet()) keys.add((String) entry.getKey());
      for (String s: keys) sb.append(s).append(" = ").append(props.get(s)).append('\n');
    } catch (Exception e) {
      sb.append("an error occurred while retrieving the system information for the selected node: " + ExceptionUtils.getMessage(e));
    }
    return sb.toString();
  }

  /**
   * Get the properties defined in the text area as a map.
   * @param source - the text from which to read the properties.
   * @return a map of string keys to string values.
   */
  public static Map<Object, Object> getPropertiesAsMap(final String source) {
    try {
      Map<Object, Object> map = new HashMap<>();
      BufferedReader reader = new BufferedReader(new StringReader(source));
      try {
        while (true) {
          String s = reader.readLine();
          if (s == null) break;
          int idx = s.indexOf('=');
          if (idx < 0) idx = s.indexOf(' ');
          if (idx < 0) continue;
          String key = s.substring(0, idx).trim();
          String value = s.substring(idx+1).trim();
          map.put(key, value);
        }
      } finally {
        reader.close();
      }
      return map;
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }
}
