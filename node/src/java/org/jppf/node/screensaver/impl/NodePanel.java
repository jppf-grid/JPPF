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
package org.jppf.node.screensaver.impl;

import java.awt.*;
import java.text.NumberFormat;

import javax.swing.*;

import org.jppf.node.screensaver.ScreenSaverMain;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * This class displays the connection status, execution status and number of tasks for a node.
 * @author Laurent Cohen
 */
public class NodePanel extends JPanel {
  /**
   * Path to the images to display in the UI.
   */
  public static final String IMAGE_PATH = "org/jppf/node";
  /**
   * Image displaying a bright green traffic light.
   */
  static final ImageIcon BRIGHT_GREEN = ScreenSaverMain.loadImage(IMAGE_PATH + '/' + "active_greenlight.gif");
  /**
   * Image displaying a dark red traffic light.
   */
  static final ImageIcon DARK_RED = ScreenSaverMain.loadImage(IMAGE_PATH + '/' + "inactive_redlight.gif");
  /**
   * Default path for the central image.
   */
  static final String DEFAULT_IMG = JPPFProperties.SCREENSAVER_CENTERIMAGE.getDefaultValue();
  /**
   * Number of tasks executed by the node.
   */
  private long taskCount = 0L;
  /**
   * These labels contain the status icons for the nodes connection and task execution activity.
   * Each status is represented by a green light and a red light, each light dark or bright depending on the node status.
   */
  private JLabel[] statusLabels = new JLabel[2];
  /**
   * Labels used to display the number of tasks executed by each node.
   */
  private JLabel countLabel;
  /**
   * Label used to display how long the node has been active.
   */
  private JLabel timeLabel;
  /**
   * The time this panel was started.
   */
  private long startedAt = 0L;
  /**
   * 
   */
  private NumberFormat nf = null;

  /**
   * Initialize this UI.
   */
  public NodePanel() {
    super(true);
    createUI();
  }

  /**
   * Initialize the user interface for this applet.
   */
  private void createUI() {
    initNodeState();
    GridBagLayout g = new GridBagLayout();
    setLayout(g);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    setBackground(Color.BLACK);
    addLayoutComp(this, g, c, createTopPanel());
    addLayoutComp(this, g, c, Box.createVerticalStrut(10));
    addLayoutComp(this, g, c, createNodePanel());
    addLayoutComp(this, g, c, Box.createVerticalStrut(5));
  }

  /**
   * Create the panel on top of the node monitoring panel.
   * @return the panel as a {@link JComponent}.
   */
  protected JComponent createTopPanel() {
    String path = JPPFConfiguration.get(JPPFProperties.SCREENSAVER_CENTERIMAGE);
    ImageIcon logo = ScreenSaverMain.loadImage(path);
    if (logo == null) logo = ScreenSaverMain.loadImage(DEFAULT_IMG);
    return new JLabel(logo);
  }

  /**
   * Initialize this node state.
   */
  private void initNodeState() {
    nf = NumberFormat.getNumberInstance();
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
    nf.setMinimumIntegerDigits(1);
    startedAt = System.currentTimeMillis();
    for (int i=0; i<statusLabels.length; i++) statusLabels[i] = new JLabel(NodePanel.DARK_RED);
    Dimension d = new Dimension(8, 8);
    for (JLabel aStatusLabel : statusLabels) {
      aStatusLabel.setMinimumSize(d);
      aStatusLabel.setMaximumSize(d);
      aStatusLabel.setBackground(Color.BLACK);
    }
    countLabel = new JLabel(Long.toString(taskCount));
    d = new Dimension(60, 20);
    countLabel.setMinimumSize(d);
    countLabel.setMaximumSize(d);
    countLabel.setBackground(Color.BLACK);
    countLabel.setForeground(Color.WHITE);

    timeLabel = new JLabel("Active for: " + toStringDuration(0L));
    timeLabel.setBackground(Color.BLACK);
    timeLabel.setForeground(Color.WHITE);
  }

  /**
   * Create a panel showing the activity of a node.
   * @return a panel with some node information about is activity.
   */
  private JPanel createNodePanel() {
    JPanel panel = new JPanel();
    GridBagLayout g = new GridBagLayout();
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    panel.setLayout(g);
    panel.setBackground(Color.BLACK);

    addLayoutComp(panel, g, c, Box.createHorizontalStrut(25));
    JLabel label = new JLabel("JPPF Node");
    label.setBackground(Color.BLACK);
    label.setForeground(Color.WHITE);
    addLayoutComp(panel, g, c, label);
    addLayoutComp(panel, g, c, Box.createHorizontalStrut(50));
    addLayoutComp(panel, g, c, timeLabel);
    addLayoutComp(panel, g, c, Box.createHorizontalStrut(25));
    addLayoutComp(panel, g, c, makeStatusPanel(0, "connection"));
    addLayoutComp(panel, g, c, Box.createHorizontalStrut(15));
    addLayoutComp(panel, g, c, makeStatusPanel(1, "execution"));
    addLayoutComp(panel, g, c, Box.createHorizontalStrut(15));
    label = new JLabel("tasks");
    label.setBackground(Color.BLACK);
    label.setForeground(Color.WHITE);
    addLayoutComp(panel, g, c, label);
    panel.add(Box.createHorizontalStrut(5));
    countLabel.setPreferredSize(new Dimension(60, 20));
    addLayoutComp(panel, g, c, countLabel);

    return panel;
  }

  /**
   * Add a component to a panel with the specified constraints.
   * @param panel the panel to add the component to.
   * @param g the <code>GridBagLayout</code> set on the panel.
   * @param c the constraints to apply to the component.
   * @param comp the component to add.
   */
  protected void addLayoutComp(final JPanel panel, final GridBagLayout g, final GridBagConstraints c, final Component comp) {
    g.setConstraints(comp, c);
    panel.add(comp);
  }

  /**
   * Generate a panel display the status of the connection or execution.
   * @param statusIdx index of the status to display: 0 for connection, 1 for execution.
   * @param text the text to display on the left of the status lights.
   * @return a <code>JPanel</code> instance.
   */
  private JPanel makeStatusPanel(final int statusIdx, final String text) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setBackground(Color.BLACK);

    JLabel label = new JLabel(text);
    label.setBackground(Color.BLACK);
    label.setForeground(Color.WHITE);
    JPanel labelPanel = new JPanel();
    labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.X_AXIS));
    labelPanel.add(label);
    labelPanel.setBackground(Color.BLACK);

    JPanel statusPanel = new JPanel();
    statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
    statusPanel.setBackground(Color.BLACK);
    statusPanel.add(statusLabels[statusIdx]);

    labelPanel.setPreferredSize(new Dimension(65, 20));
    panel.add(labelPanel);
    panel.add(Box.createHorizontalStrut(5));
    statusPanel.setPreferredSize(new Dimension(8, 20));
    panel.add(statusPanel);
    panel.setPreferredSize(new Dimension(78, 20));
    return panel;
  }

  /**
   * Update the status of the connection to the server.
   * @param ok <code>true</code> if the status is ok, <code>false</code> otherwise.
   */
  public synchronized void updateConnectionStatus(final boolean ok) {
    statusLabels[0].setIcon(ok ? NodePanel.BRIGHT_GREEN : NodePanel.DARK_RED);
  }

  /**
   * Update the status of the execution to the server.
   * @param ok <code>true</code> if the status is ok, <code>false</code> otherwise.
   */
  public synchronized void updateExecutionStatus(final boolean ok) {
    statusLabels[1].setIcon(ok ? NodePanel.BRIGHT_GREEN : NodePanel.DARK_RED);
  }

  /**
   * Update the text of the label indicating how long the screensaver has been active.
   */
  public synchronized void updateTimeLabel() {
    String s = toStringDuration(System.currentTimeMillis() - startedAt);
    timeLabel.setText("Active for: " + s);
  }

  /**
   * Increment the number of tasks executed by the node.
   * @param increment the number by which to increment.
   * @return the number of tasks as a long value.
   */
  public synchronized long incTaskCount(final long increment) {
    taskCount += increment;
    countLabel.setText(nf.format(taskCount));
    return taskCount;
  }

  /**
   * Used to nicely format integer values.
   */
  private static NumberFormat integerFormatter = null;
  /**
   * Transform a duration in milliseconds into a string with hours, minutes, seconds and milliseconds..
   * @param duration the duration to transform, expressed in milliseconds.
   * @return a string specifying the duration in terms of hours, minutes, seconds and milliseconds.
   */
  public static String toStringDuration(final long duration) {
    if (integerFormatter == null) {
      integerFormatter = NumberFormat.getInstance();
      integerFormatter.setGroupingUsed(false);
      integerFormatter.setMaximumFractionDigits(0);
      integerFormatter.setMinimumIntegerDigits(2);
    }
    long elapsed = duration;
    StringBuilder sb = new StringBuilder();
    sb.append(integerFormatter.format(elapsed / 3_600_000L)).append(" hrs ");
    sb.append(integerFormatter.format((elapsed %= 3_600_000L) / 60_000L)).append(" mn ");
    sb.append(integerFormatter.format((elapsed %= 60_000L) / 1_000L)).append(" sec");
    return sb.toString();
  }
}
