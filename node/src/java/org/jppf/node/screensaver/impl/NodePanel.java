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
package org.jppf.node.screensaver.impl;

import java.awt.*;
import java.text.NumberFormat;

import javax.swing.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class enables launching a JPPF node as an applet, from a web browser.
 * @author Laurent Cohen
 */
class NodePanel extends JPanel
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodePanel.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Path to the images to display in the UI.
   */
  public static final String IMAGE_PATH = "org/jppf/node";
  /**
   * Image displaying a bright green traffic light.
   */
  static final ImageIcon BRIGHT_GREEN = loadImage(IMAGE_PATH + '/' + "active_greenlight.gif");
  /**
   * Image displaying a dark green traffic light.
   */
  static final ImageIcon DARK_GREEN = loadImage(IMAGE_PATH + '/' + "inactive_greenlight.gif");
  /**
   * Image displaying a bright red traffic light.
   */
  static final ImageIcon BRIGHT_RED = loadImage(IMAGE_PATH + '/' + "active_redlight.gif");
  /**
   * Image displaying a dark red traffic light.
   */
  static final ImageIcon DARK_RED = loadImage(IMAGE_PATH + '/' + "inactive_redlight.gif");
  /**
   * Default path for the central image.
   */
  static final String DEFAULT_IMG = IMAGE_PATH + '/' + "jppf@home.gif";
  /**
   * Number of tasks executed by the node.
   */
  private long taskCount = 0L;
  /**
   * Holds the statuses for the node connection and tasks execution.
   */
  public boolean[] status = new boolean[2];
  /**
   * These labels contain the status icons for the nodes connection and task execution activity.
   * Each status is represented by a green light and a red light, each light dark or bright depending on the node status.
   */
  public JLabel[] statusLabels = new JLabel[2];
  /**
   * Labels used to display the number of tasks executed by each node.
   */
  public JLabel countLabel = null;
  /**
   * Label used to display how long the node has been active.
   */
  public JLabel timeLabel = null;
  /**
   * The time this panel was started.
   */
  public long startedAt = 0L;
  /**
   * 
   */
  private NumberFormat nf = null;

  /**
   * Initialize this UI.
   */
  public NodePanel()
  {
    try
    {
      createUI();
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Initialize the user interface for this applet.
   */
  private void createUI()
  {
    initNodeState();
    GridBagLayout g = new GridBagLayout();
    setLayout(g);
    GridBagConstraints c = new GridBagConstraints();
    c.gridx = 0;
    setBackground(Color.BLACK);
    //ImageIcon logo = loadImage(IMAGE_PATH + '/' + "jppf-at-home.gif");
    String path = JPPFConfiguration.getProperties().getString("jppf.screensaver.centerimage", DEFAULT_IMG);
    ImageIcon logo = loadImage(path);
    if (logo == null) logo = loadImage(DEFAULT_IMG);
    JLabel logoLabel = new JLabel(logo);
    addLayoutComp(this, g, c, logoLabel);
    addLayoutComp(this, g, c, Box.createVerticalStrut(10));
    addLayoutComp(this, g, c, createNodePanel());
    addLayoutComp(this, g, c, Box.createVerticalStrut(5));
  }

  /**
   * Initialize this node state.
   */
  private void initNodeState()
  {
    nf = NumberFormat.getNumberInstance();
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(0);
    nf.setMinimumIntegerDigits(1);
    startedAt = System.currentTimeMillis();
    for (int i=0; i<statusLabels.length; i++)
    {
      statusLabels[i] = new JLabel(NodePanel.DARK_RED);
    }
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

    timeLabel = new JLabel("Active for: "+NodePanel.toStringDuration(0));
    timeLabel.setBackground(Color.BLACK);
    timeLabel.setForeground(Color.WHITE);
  }

  /**
   * Create a panel showing the activity of a node.
   * @return a panel with some node information about is activity.
   */
  private JPanel createNodePanel()
  {
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
  private void addLayoutComp(final JPanel panel, final GridBagLayout g, final GridBagConstraints c, final Component comp)
  {
    g.setConstraints(comp, c);
    panel.add(comp);
  }

  /**
   * Generate a panel display the status of the connection or execution.
   * @param statusIdx index of the status to display: 0 for connection, 1 for execution.
   * @param text the text to display on the left of the status lights.
   * @return a <code>JPanel</code> instance.
   */
  private JPanel makeStatusPanel(final int statusIdx, final String text)
  {
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

    labelPanel.setPreferredSize(new Dimension(60, 20));
    panel.add(labelPanel);
    panel.add(Box.createHorizontalStrut(5));
    statusPanel.setPreferredSize(new Dimension(8, 20));
    panel.add(statusPanel);
    panel.setPreferredSize(new Dimension(73, 20));
    return panel;
  }

  /**
   * Load an icon from the specified path.
   * @param file the file to get the icon from.
   * @return an <code>ImageIcon</code> instance.
   */
  public static ImageIcon loadImage(final String file)
  {
    byte[] buf = null;
    try
    {
      buf = FileUtils.getPathAsByte(file);
    }
    catch (Exception e)
    {
      String msg = "Could not load image '{}' : {}";
      if (debugEnabled) log.debug(msg, file, ExceptionUtils.getStackTrace(e));
      else log.warn(msg, file, ExceptionUtils.getMessage(e));
    }
    return (buf == null) ? null : new ImageIcon(Toolkit.getDefaultToolkit().createImage(buf));
  }

  /**
   * Free resources used by the nodes.
   */
  public void cleanup()
  {
    try
    {
    }
    catch (Throwable t)
    {
    }
  }

  /**
   * Update the status of the connection to the server.
   * @param ok <code>true</code> if the status is ok, <code>false</code> otherwise.
   */
  public void updateConnectionStatus(final boolean ok)
  {
    statusLabels[0].setIcon(ok ? NodePanel.BRIGHT_GREEN : NodePanel.DARK_RED);
  }

  /**
   * Update the status of the execution to the server.
   * @param ok <code>true</code> if the status is ok, <code>false</code> otherwise.
   */
  public void updateExecutionStatus(final boolean ok)
  {
    statusLabels[1].setIcon(ok ? NodePanel.BRIGHT_GREEN : NodePanel.DARK_RED);
  }

  /**
   * Increment the number of tasks executed by the node.
   * @param increment the number by which to increment.
   * @return the number of tasks as a long value.
   */
  public synchronized long incTaskCount(final long increment)
  {
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
  public static String toStringDuration(final long duration)
  {
    if (integerFormatter == null)
    {
      integerFormatter = NumberFormat.getInstance();
      integerFormatter.setGroupingUsed(false);
      integerFormatter.setMinimumFractionDigits(0);
      integerFormatter.setMaximumFractionDigits(0);
      integerFormatter.setMinimumIntegerDigits(2);
    }

    long elapsed = duration;
    StringBuilder sb = new StringBuilder();
    sb.append(integerFormatter.format(elapsed / 3600000L)).append(" hrs ");
    elapsed = elapsed % 3600000L;
    sb.append(integerFormatter.format(elapsed / 60000L)).append(" mn ");
    elapsed = elapsed % 60000L;
    sb.append(integerFormatter.format(elapsed / 1000L));
    sb.append(" sec");
    return sb.toString();
  }
}
