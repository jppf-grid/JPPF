/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.jppf.management.*;
import org.jppf.node.provisioning.JPPFNodeProvisioningMBean;
import org.jppf.ui.monitoring.node.TopologyData;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.TypedProperties;
import org.slf4j.*;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class ProvisioningAction extends AbstractTopologyAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProvisioningAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  protected static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the "OK" button was pressed.
   */
  private boolean isOk = false;
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement thisPanel = null;
  /**
   * 
   */
  private String overrides = "";
  /**
   * 
   */
  private int nbSlaves = 0;

  /**
   * Initialize this action.
   */
  public ProvisioningAction()
  {
    setupIcon("/org/jppf/ui/resources/weather-overcast.png");
    setupNameAndTooltip("node.provisioning");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects selected in the tree table.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    this.selectedElements = selectedElements;
    List<TopologyData> list = new ArrayList<>();
    for (Object o: selectedElements) {
      TopologyData data = (TopologyData) o;
      JPPFManagementInfo info = data.getNodeInformation();
      if ((info != null) && info.isMasterNode()) list.add(data);
    }
    dataArray = list.toArray(list.isEmpty() ? EMPTY_TOPOLOGY_DATA_ARRAY : new TopologyData[list.size()]);
    setEnabled(dataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event - not used.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(final ActionEvent event)
  {
    AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    thisPanel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/ProvisioningPanel.xml");
    OptionsHandler.OptionNode optionNode = OptionsHandler.buildPersistenceGraph(thisPanel);
    OptionsHandler.loadPreferences(optionNode, OptionsHandler.getPreferences());
    TextAreaOption textArea = (TextAreaOption) thisPanel.findFirstWithName("configOverrides");
    //textArea.setValue(overrides);

    JButton okBtn = (JButton) thisPanel.findFirstWithName("/provisioningOK").getUIComponent();
    JButton cancelBtn = (JButton) thisPanel.findFirstWithName("/provisioningCancel").getUIComponent();
    final JFrame frame = new JFrame("Update the JPPF configuration");
    frame.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/weather-overcast.png").getImage());
    AbstractAction okAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        frame.setVisible(false);
        frame.dispose();
        doOK();
      }
    };
    AbstractAction cancelAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        TextAreaOption textArea = (TextAreaOption) thisPanel.findFirstWithName("configOverrides");
        overrides = (String) textArea.getValue();
        frame.setVisible(false);
        frame.dispose();
      }
    };
    okBtn.addActionListener(okAction);
    cancelBtn.addActionListener(cancelAction);
    setKeyAction(thisPanel, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), okAction, "ok");
    setKeyAction(thisPanel, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelAction, "cancel");
    frame.getContentPane().add(thisPanel.getUIComponent());
    frame.pack();
    frame.setLocationRelativeTo(null);
    if (location != null) frame.setLocation(location);
    frame.setVisible(true);
  }

  /**
   * Perform the action.
   */
  private void doOK()
  {
    OptionsHandler.OptionNode optionNode = OptionsHandler.buildPersistenceGraph(thisPanel);
    OptionsHandler.savePreferences(optionNode, OptionsHandler.getPreferences());
    TextAreaOption textArea = (TextAreaOption) thisPanel.findFirstWithName("configOverrides");
    final Boolean b = (Boolean) ((BooleanOption) thisPanel.findFirstWithName("useOverrides")).getValue();
    overrides = (String) textArea.getValue();
    final TypedProperties props = ((b != null) && b.booleanValue()) ? getPropertiesAsMap(overrides) : null;
    nbSlaves = ((Number) ((SpinnerNumberOption) thisPanel.findFirstWithName("nbSlaves")).getValue()).intValue();
    Runnable r = new Runnable() {
      @Override
      public void run() {
        TopologyData parent = dataArray[0].getParent();
        if (parent == null) return;
        try {
          String[] uuids = new String[dataArray.length];
          for (int i=0; i<dataArray.length; i++) uuids[i] = dataArray[i].getUuid();
          parent.getNodeForwarder().forwardInvoke(new NodeSelector.UuidSelector(uuids), JPPFNodeProvisioningMBean.MBEAN_NAME, "provisionSlaveNodes",
            new Object[] {nbSlaves, props}, new String[] {int.class.getName(), TypedProperties.class.getName()});
        } catch(IOException e) {
          parent.initializeProxies();
          log.error(e.getMessage(), e);
        } catch(Exception e) {
          log.error(e.getMessage(), e);
        }
      }
    };
    runAction(r);
  }

  /**
   * Get the properties defined in the text area as a map.
   * @param source - the text from which to read the properties.
   * @return a map of string keys to string values.
   */
  private static TypedProperties getPropertiesAsMap(final String source)
  {
    TypedProperties props = new TypedProperties();
    try (Reader reader = new StringReader(source)) {
      props.load(reader);
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return props;
  }
}
