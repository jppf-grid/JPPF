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
package org.jppf.ui.monitoring.job.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.*;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.job.JobUuidSelector;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * This action updates the maximum number of nodes a job can run on.
 */
public class UpdateMaxNodesAction extends AbstractJobAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(UpdateMaxNodesAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Panel containing the dialog for entering the number of threads and their priority.
   */
  private OptionElement panel = null;
  /**
   * The maximum number of nodes.
   */
  private int maxNodes = Integer.MAX_VALUE;

  /**
   * Initialize this action.
   */
  public UpdateMaxNodesAction() {
    setupIcon("/org/jppf/ui/resources/impl_co.gif");
    BASE = "org.jppf.ui.i18n.JobDataPage";
    putValue(NAME, localize("job.update.max.nodes.label"));
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    setEnabled(jobDataArray.length > 0);
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
      panel = loadWithPreferences("org/jppf/ui/options/xml/JobMaxNodesPanel.xml");
      maxNodes = Integer.MAX_VALUE;
      for (Job data: jobDataArray) {
        final int n = data.getJobInformation().getMaxNodes();
        if (n < maxNodes) maxNodes = n;
      }
      ((AbstractOption) panel.findFirstWithName("job.max.nodes")).setValue(maxNodes);
      ((AbstractOption) panel.findFirstWithName("job.nolimit.toggle")).setValue(maxNodes == Integer.MAX_VALUE);

      final JButton okBtn = (JButton) panel.findFirstWithName("/job.max.nodes.OK").getUIComponent();
      final JButton cancelBtn = (JButton) panel.findFirstWithName("/job.max.nodes.Cancel").getUIComponent();
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("JobMaxNodesPanel.label"), false);
      dialog.setIconImage(((ImageIcon) getValue(Action.SMALL_ICON)).getImage());
      final AbstractAction okAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          disposeDialog(dialog);
          doOK();
        }
      };
      final AbstractAction cancelAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          disposeDialog(dialog);
        }
      };
      okBtn.addActionListener(okAction);
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
    final AbstractOption noLimitOption = (AbstractOption) panel.findFirstWithName("job.nolimit.toggle");
    final AbstractOption maxNodesOption = (AbstractOption) panel.findFirstWithName("job.max.nodes");
    final boolean noLimit = (Boolean) noLimitOption.getValue();
    maxNodes = noLimit ? Integer.MAX_VALUE : ((Number) maxNodesOption.getValue()).intValue();
    final CollectionMap<JobDriver, String> map = new SetHashMap<>();
    for (Job data : jobDataArray) map.putValue(data.getJobDriver(), data.getUuid());
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        for (JobDriver driver: map.keySet()) {
          try {
            final DriverJobManagementMBean jmx = driver.getTopologyDriver().getJobManager();
            if (jmx != null) jmx.updateMaxNodes(new JobUuidSelector(map.getValues(driver)), maxNodes);
          } catch (final Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    runAction(r);
  }
}
