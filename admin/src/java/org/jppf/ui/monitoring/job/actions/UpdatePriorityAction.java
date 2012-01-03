/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
import org.slf4j.*;

/**
 * This action updates the maximum number of nodes a job can run on.
 */
public class UpdatePriorityAction extends AbstractJobAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(UpdatePriorityAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
  private int priority = Integer.MAX_VALUE;

  /**
   * Initialize this action.
   */
  public UpdatePriorityAction()
  {
    setupIcon("/org/jppf/ui/resources/priority.gif");
    putValue(NAME, localize("job.update.priority.label"));
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements)
  {
    super.updateState(selectedElements);
    setEnabled(jobDataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event not used.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(final ActionEvent event)
  {
    AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    if (selectedElements.isEmpty()) return;
    try
    {
      panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/JobPriorityPanel.xml");
      priority = Integer.MAX_VALUE;
      for (JobData data: jobDataArray)
      {
        int n = data.getJobInformation().getMaxNodes();
        if (n < priority) priority = n;
      }
      ((AbstractOption) panel.findFirstWithName("job.priority")).setValue(priority);

      JButton okBtn = (JButton) panel.findFirstWithName("/job.priority.OK").getUIComponent();
      JButton cancelBtn = (JButton) panel.findFirstWithName("/job.priority.Cancel").getUIComponent();
      final JFrame frame = new JFrame("Enter the new job priority");
      frame.setIconImage(((ImageIcon) getValue(Action.SMALL_ICON)).getImage());
      okBtn.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(final ActionEvent event)
        {
          frame.setVisible(false);
          frame.dispose();
          doOK();
        }
      });
      cancelBtn.addActionListener(new ActionListener()
      {
        @Override
        public void actionPerformed(final ActionEvent event)
        {
          frame.setVisible(false);
          frame.dispose();
        }
      });
      frame.getContentPane().add(panel.getUIComponent());
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setLocation(location);
      frame.setVisible(true);
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Perform the action.
   */
  private void doOK()
  {
    AbstractOption priorityOption = (AbstractOption) panel.findFirstWithName("job.max.nodes");
    priority = (Integer) priorityOption.getValue();
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        for (JobData data: jobDataArray)
        {
          try
          {
            JMXDriverConnectionWrapper jmx = data.getJmxWrapper();
            jmx.updateJobPriority(data.getJobInformation().getJobUuid(), priority);
          }
          catch(Exception e)
          {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    new Thread(r).start();
  }
}
