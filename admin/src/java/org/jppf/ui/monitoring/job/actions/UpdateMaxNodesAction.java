/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.awt.event.*;
import java.util.List;

import javax.swing.*;

import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.ui.monitoring.job.JobData;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.utils.LoggingUtils;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the "OK" button was pressed.
   */
  private boolean isOk = false;
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
    AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    if (selectedElements.isEmpty()) return;
    try {
      panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/JobMaxNodesPanel.xml");
      maxNodes = Integer.MAX_VALUE;
      for (JobData data: jobDataArray) {
        int n = data.getJobInformation().getMaxNodes();
        if (n < maxNodes) maxNodes = n;
      }
      ((AbstractOption) panel.findFirstWithName("job.max.nodes")).setValue(maxNodes);
      ((AbstractOption) panel.findFirstWithName("job.nolimit.toggle")).setValue(maxNodes == Integer.MAX_VALUE);

      JButton okBtn = (JButton) panel.findFirstWithName("/job.max.nodes.OK").getUIComponent();
      JButton cancelBtn = (JButton) panel.findFirstWithName("/job.max.nodes.Cancel").getUIComponent();
      final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), "Enter the number of threads and their priority", false);
      dialog.setIconImage(((ImageIcon) getValue(Action.SMALL_ICON)).getImage());
      okBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          dialog.setVisible(false);
          dialog.dispose();
          doOK();
        }
      });
      cancelBtn.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      });
      dialog.getContentPane().add(panel.getUIComponent());
      dialog.pack();
      dialog.setLocationRelativeTo(null);
      dialog.setLocation(location);
      dialog.setVisible(true);
    } catch(Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Perform the action.
   */
  private void doOK() {
    AbstractOption noLimitOption = (AbstractOption) panel.findFirstWithName("job.nolimit.toggle");
    AbstractOption maxNodesOption = (AbstractOption) panel.findFirstWithName("job.max.nodes");
    boolean noLimit = (Boolean) noLimitOption.getValue();
    maxNodes = noLimit ? Integer.MAX_VALUE : ((Number) maxNodesOption.getValue()).intValue();
    Runnable r = new Runnable() {
      @Override
      public void run() {
        for (JobData data: jobDataArray) {
          try {
            JMXDriverConnectionWrapper jmx = data.getJmxWrapper();
            jmx.updateMaxNodes(data.getJobInformation().getJobUuid(), maxNodes);
          } catch(Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    new Thread(r).start();
  }
}
