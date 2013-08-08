/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.ui.monitoring.diagnostics;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.ui.monitoring.diagnostics.Thresholds.Name;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.slf4j.*;

/**
 * This action displays an input panel for the user to type a new
 * thread pool size for a node, and updates the node with it.
 */
public class ThresholdSettingsAction extends AbstractTopologyAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThresholdSettingsAction.class);
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
   * Number of threads.
   */
  private int nbThreads = 1;
  /**
   * Threads priority.
   */
  private int priority = Thread.NORM_PRIORITY;
  /**
   * The JVM Health Panel option.
   */
  private final JVMHealthPanel healthPanel;

  /**
   * Initialize this action.
   * @param healthPanel the JVM Health Panel option.
   */
  public ThresholdSettingsAction(final JVMHealthPanel healthPanel)
  {
    this.healthPanel = healthPanel;
    setupIcon("/org/jppf/ui/resources/thresholds.gif");
    setupNameAndTooltip("health.update.thresholds");
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
    try
    {
      panel = OptionsHandler.loadPageFromXml("org/jppf/ui/options/xml/JVMHealthThresholdsPanel.xml");
      Map<Name, Double> values = healthPanel.thresholds.getValues();
      for (Map.Entry<Name, Double> entry: values.entrySet())
      {
        AbstractOption option = (AbstractOption) panel.findFirstWithName(entry.getKey().getName());
        option.setValue(entry.getValue()*100d);
      }

      JButton okBtn = (JButton) panel.findFirstWithName("/health.thresholds.buttons.ok").getUIComponent();
      JButton cancelBtn = (JButton) panel.findFirstWithName("/health.thresholds.buttons.cancel").getUIComponent();
      final JFrame frame = new JFrame("Alert Threshold Settings");
      frame.setIconImage(((ImageIcon) getValue(Action.SMALL_ICON)).getImage());
      AbstractAction okAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          frame.setVisible(false);
          frame.dispose();
          doOK();
        }
      };
      okBtn.addActionListener(okAction);
      AbstractAction cancelAction = new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent event) {
          frame.setVisible(false);
          frame.dispose();
        }
      };
      cancelBtn.addActionListener(cancelAction);
      setAllLabelsColors();
      frame.getContentPane().add(panel.getUIComponent());
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setLocation(location);
      setKeyAction(panel, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), okAction, "ok");
      setKeyAction(panel, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), cancelAction, "cancel");
      frame.setVisible(true);
    }
    catch(Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
  }

  /**
   * Set the color of the labels of the spinners.
   */
  private void setAllLabelsColors()
  {
    Map<Name, Double> values = healthPanel.thresholds.getValues();
    for (Map.Entry<Name, Double> entry: values.entrySet()) setLabelColors(entry.getKey());
  }

  /**
   * Set the color of the labels of the spinners.
   * @param name the name of the spinner option.
   */
  private void setLabelColors(final Name name)
  {
    Color c = null;
    switch(name)
    {
      case MEMORY_WARNING:
      case CPU_WARNING:
        c = AbstractTreeCellRenderer.SUSPENDED_COLOR;
        break;
      case MEMORY_CRITICAL:
      case CPU_CRITICAL:
        c = AbstractTreeCellRenderer.INACTIVE_COLOR;
        break;
    }
    if (c == null) return;
    SpinnerNumberOption option = (SpinnerNumberOption) panel.findFirstWithName(name.getName());
    JLabel label = option.getSpinnerLabel();
    if (label != null)
    {
      label.setOpaque(true);
      label.setBackground(c);
    }
  }

  /**
   * Perform the action.
   */
  private void doOK()
  {
    final Map<Thresholds.Name, Double> values = healthPanel.thresholds.getValues();
    final List<Thresholds.Name> list = new ArrayList<>(values.keySet());
    Runnable r = new Runnable() {
      @Override
      public void run() {
        try {
          for (Name name: list) {
            AbstractOption option = (AbstractOption) panel.findFirstWithName(name.getName());
            values.put(name, (Double) option.getValue()/100d);
          }
          healthPanel.saveThresholds();
        } catch (Exception e) {
          if (debugEnabled) log.debug(e.getMessage(), e);
        }
      }
    };
    new Thread(r).start();
  }
}
