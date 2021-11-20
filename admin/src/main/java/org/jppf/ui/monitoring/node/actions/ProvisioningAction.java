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
import java.io.*;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class ProvisioningAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ProvisioningAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  protected static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement panel = null;
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
  public ProvisioningAction() {
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
    final List<AbstractTopologyComponent> list = new ArrayList<>();
    for (final Object o: selectedElements) {
      final AbstractTopologyComponent data = (AbstractTopologyComponent) o;
      final JPPFManagementInfo info = data.getManagementInfo();
      if ((info != null) && info.isMasterNode()) list.add(data);
    }
    dataArray = list.toArray(list.isEmpty() ? EMPTY_TOPOLOGY_DATA_ARRAY : new AbstractTopologyComponent[list.size()]);
    setEnabled(dataArray.length > 0);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    panel = loadWithPreferences("org/jppf/ui/options/xml/ProvisioningPanel.xml");
    final JButton okBtn = (JButton) panel.findFirstWithName("/provisioningOK").getUIComponent();
    final JButton cancelBtn = (JButton) panel.findFirstWithName("/provisioningCancel").getUIComponent();
    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("provisioning.frame.caption"), false);
    dialog.setIconImage(GuiUtils.loadIcon("/org/jppf/ui/resources/weather-overcast.png").getImage());
    final AbstractAction okAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        disposeDialog(dialog);
        doOK();
      }
    };
    final AbstractAction cancelAction = new AbstractAction() {
      @Override public void actionPerformed(final ActionEvent event) {
        final CodeEditorOption textArea = (CodeEditorOption) panel.findFirstWithName("configOverrides");
        overrides = (String) textArea.getValue();
        disposeDialog(dialog);
      }
    };
    okBtn.addActionListener(okAction);
    cancelBtn.addActionListener(cancelAction);
    setOkCancelKeys(panel, okAction, cancelAction);
    readyDialog(dialog, panel.getUIComponent(), location);
  }

  /**
   * Perform the action.
   */
  private void doOK() {
    savePreferences(panel);
    final CodeEditorOption textArea = (CodeEditorOption) panel.findFirstWithName("configOverrides");
    final Boolean b = (Boolean) ((BooleanOption) panel.findFirstWithName("useOverrides")).getValue();
    overrides = (String) textArea.getValue();
    final TypedProperties props = ((b != null) && b.booleanValue()) ? getPropertiesFromString(overrides) : null;
    nbSlaves = ((Number) ((SpinnerNumberOption) panel.findFirstWithName("nbSlaves")).getValue()).intValue();
    final Boolean interruptIfRunning = (Boolean) ((BooleanOption) panel.findFirstWithName("interruptIfRunning")).getValue();
    final CollectionMap<TopologyDriver, String> map = new ArrayListHashMap<>();
    for (final AbstractTopologyComponent data: dataArray) {
      if (data.getParent() == null) continue;
      map.putValue((TopologyDriver) data.getParent(), data.getUuid());
    }
    final Runnable r = new Runnable() {
      @Override public void run() {
        for (final Map.Entry<TopologyDriver, Collection<String>> en: map.entrySet()) {
          final TopologyDriver parent = en.getKey();
          final NodeSelector selector = new UuidSelector(en.getValue());
          try {
            final ResultsMap<String, Void> result = parent.getForwarder().provisionSlaveNodes(selector, nbSlaves, interruptIfRunning, props);
            printForwardingRequestErrors(result);
          } catch(final Exception e) {
            log.error(e.getMessage(), e);
          }
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
  private static TypedProperties getPropertiesFromString(final String source) {
    final TypedProperties props = new TypedProperties();
    try (final Reader reader = new StringReader(source)) {
      props.load(reader);
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
    return props;
  }

  /**
   * Prints the eventual errors resulting from a node forwarding request.
   * @param result the map containing the results for the request.
   */
  private static void printForwardingRequestErrors(final ResultsMap<String, Void> result) {
    if (debugEnabled) {
      for (Map.Entry<String, InvocationResult<Void>> en2: result.entrySet()) {
        if (en2.getValue().isException()) {
          final Throwable t = en2.getValue().exception();
          if (debugEnabled) log.debug("provisioning request for node '{}' resulted in error: {}", en2.getKey(), ExceptionUtils.getStackTrace(t));
        }
      }
    }
  }
}
