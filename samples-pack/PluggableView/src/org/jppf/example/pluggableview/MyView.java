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

package org.jppf.example.pluggableview;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.plugin.PluggableView;
import org.jppf.ui.utils.GuiUtils;

/**
 * Example of a pluggable view that can be added to the JPPF administration and monitoring console.
 * <p>This class builds a view which displays a log of the JPPF topology events that occur, when drivers
 * or nodes are added to or removed from the topology.
 * <p>The view comprises 3 panels:
 * <ul>
 * <li>the top panel displays a JPPF logo with a formatted title</li>
 * <li>the middle panel is a text area where timestamped events scroll down as they occur</li>
 * <li>the bottom panel has 2 buttons to clear the log and copy all log entries to the clipboard</li>
 * </ul>
 * <p>To plug the view into the admin console, the following properties are added to the conosle's
 * configuration file:
 * <pre> # enable / disable the custom view. defaults to true (enabled)
 * jppf.admin.console.view.MyView.enabled = true
 * # name of a class extending org.jppf.ui.plugin.PluggableView
 * jppf.admin.console.view.MyView.class = test.console.MyView
 * # the title for the view; only used if placed in a tabbed pane
 * jppf.admin.console.view.MyView.title = Events Log
 * # path to the icon for the view; only used if placed in a tabbed pane
 * jppf.admin.console.view.MyView.icon = /test.gif
 * # the built-in view it is attached to; "Main" is the main tabbed pane
 * jppf.admin.console.view.MyView.addto = Main
 * # the position at which the custom view is inserted withing the enclosing tabbed pane
 * # a negative value means insert at the end; defaults to -1 (insert at the end)
 * jppf.admin.console.view.MyView.position = 1
 * # whether to automatically select the view on startup; defaults to false
 * #jppf.admin.console.view.MyView.autoselect = true
 * </pre>
 * @author Laurent Cohen
 */
public class MyView extends PluggableView {
  /**
   * The maximum number of entries in the log.
   */
  private static final int MAX_MESSAGES = 50_000;
  /**
   * The view's backgroud color.
   */
  private static final Color BKG = new Color(232, 234, 253);
  /**
   * The Swing component enclosing the view.
   */
  private JComponent component = null;
  /**
   * The text area where the log entries are displayed
   */
  private JTextArea textArea = null;
  /**
   * Rolling double-ended queue of log entries.
   */
  private Deque<String> messages = new ConcurrentLinkedDeque<>();
  /**
   * Maintains the current log size to avoid expensive computations.
   */
  private int logSize = 0;
  /**
   * The date format used to format the timestamps.
   */
  private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

  /**
   * Get or create the view as a Swing component.
   * @return a {@link JComponent}.
   */
  @Override
  public JComponent getUIComponent() {
    if (component == null) {
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.setBorder(BorderFactory.createEtchedBorder());
      panel.setBackground(BKG);
      panel.add(Box.createVerticalStrut(10));
      // add the JPPF logo
      panel.add(new JLabel(GuiUtils.loadIcon("/jppf_logo.gif")));
      // add a formatted text just below the logo
      JLabel label = new JLabel("JPPF pluggable view");
      label.setForeground(new Color(109, 120, 182));
      Font font = label.getFont();
      label.setFont(new Font("Arial", Font.BOLD, 2*font.getSize()));
      panel.add(label);
      panel.add(Box.createVerticalStrut(10));
      // add the text area that displays the log messages
      panel.add(createTextArea());
      // add the action buttons
      panel.add(createButtonsPanel());
      // register this view to listen to topology events to update the log accordingly
      getTopologyManager().addTopologyListener(new MyListener());
      panel.add(Box.createGlue());
      component = panel;
    }
    return component;
  }

  /**
   * Create the text area which displays the log messages.
   * It is enclosed within a {@link JScrollPane} with a border and title.
   * @return the {@link JComponent} enclosing the text area.
   */
  private JComponent createTextArea() {
    textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setBackground(BKG);
    JScrollPane scrollPane = new JScrollPane(textArea);
    scrollPane.setBorder(BorderFactory.createTitledBorder("Topology Events"));
    scrollPane.setPreferredSize(new Dimension(800, 400));
    scrollPane.setBackground(BKG);
    return scrollPane;
  }

  /**
   * Create the action buttons.
   * They are enclosed within a {@link JPanel} with a border and title.
   * @return a {@link JComponent} holding the action buttons.
   */
  private JComponent createButtonsPanel() {
    // create the button to clear the log entries
    JButton clearButton = new JButton("Clear", GuiUtils.loadIcon("/clear.gif"));
    clearButton.setToolTipText("clear all log entries");
    clearButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        synchronized(MyView.this) {
          messages.clear();
          textArea.setText("");
          logSize = 0;
        }
      }
    });
    // create the button to copy the log entries to the clipboard
    JButton copyButton = new JButton("Copy", GuiUtils.loadIcon("/copy.gif"));
    copyButton.setToolTipText("copy all log entries to the clipboard");
    copyButton.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        String text = null;
        synchronized(MyView.this) {
          text = textArea.getText();
        }
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(new StringSelection(text), null);
      }
    });
    JPanel panel = new JPanel();
    panel.setBackground(BKG);
    panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
    panel.setBorder(BorderFactory.createTitledBorder("Actions"));
    panel.add(Box.createHorizontalStrut(10));
    panel.add(clearButton);
    panel.add(Box.createHorizontalStrut(10));
    panel.add(copyButton);
    panel.add(Box.createHorizontalGlue());
    panel.setPreferredSize(new Dimension(800, 28));
    return panel;
  }

  /**
   * This method adds a new entry to the messages log.
   * @param message the message to add.
   */
  private synchronized void newMessage(final String message) {
    // create and format a timestamp
    String date = sdf.format(new Date());
    // add the new, timestamped message to the log
    messages.add(String.format("[%s] %s", date, message));
    logSize++;
    // if number of log entries > max, remove the oldest ones
    while (logSize > MAX_MESSAGES) {
      messages.removeFirst();
      logSize--;
    }
    // update the text of the text area
    StringBuilder sb = new StringBuilder();
    for (String msg: messages) sb.append(msg).append("\n");
    textArea.setText(sb.toString());
    // scroll to the end of the log
    textArea.setCaretPosition(sb.length() - 1);
  }

  /**
   * Our topology events listener implementation.
   */
  private class MyListener extends TopologyListenerAdapter {
    @Override
    public void driverAdded(final TopologyEvent event) {
      newMessage("added driver " + event.getDriver().getDisplayName());
    }

    @Override
    public void driverRemoved(final TopologyEvent event) {
      newMessage("removed driver " + event.getDriver().getDisplayName());
    }

    @Override
    public void nodeAdded(final TopologyEvent event) {
      TopologyNode node = event.getNodeOrPeer();
      String message = String.format("added %s %s to driver %s", (node.isNode() ? "node" :  "peer"), node.getDisplayName(), event.getDriver().getDisplayName());
      newMessage(message);
    }

    @Override
    public void nodeRemoved(final TopologyEvent event) {
      TopologyNode node = event.getNodeOrPeer();
      String message = String.format("removed %s %s from driver %s", (node.isNode() ? "node" :  "peer"), node.getDisplayName(), event.getDriver().getDisplayName());
      newMessage(message);
    }
  }
}
