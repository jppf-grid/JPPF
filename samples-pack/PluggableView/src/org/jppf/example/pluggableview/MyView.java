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

package org.jppf.example.pluggableview;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.plugin.PluggableView;
import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.JPPFConfiguration;

/**
 * Example of a pluggable view that can be added to the JPPF administration and monitoring console.
 * <p>This class builds a view which displays a log of the JPPF topology events that occur, when drivers
 * or nodes are added to or removed from the topology.
 * <p>The view comprises 3 panels:
 * <ul>
 * <li>the top panel displays a JPPF logo with a formatted title</li>
 * <li>the middle panel is a {@link JList} where timestamped events scroll down as they occur</li>
 * <li>the bottom panel has 2 buttons to clear the log and copy all log entries to the clipboard</li>
 * </ul>
 * <p>To plug the view into the admin console, the following properties are added to the conosle's
 * configuration file:
 * <pre>
 * <font color="green"># enable / disable the custom view. defaults to true (enabled)</font>
 * jppf.admin.console.view.MyView.enabled = true
 * <font color="green"># name of a class extending org.jppf.ui.plugin.PluggableView</font>
 * jppf.admin.console.view.MyView.class = test.console.MyView
 * <font color="green"># the title for the view; only used if placed in a tabbed pane</font>
 * jppf.admin.console.view.MyView.title = Events Log
 * <font color="green"># path to the icon for the view; only used if placed in a tabbed pane</font>
 * jppf.admin.console.view.MyView.icon = /test.gif
 * <font color="green"># the built-in view it is attached to; "Main" is the main tabbed pane</font>
 * jppf.admin.console.view.MyView.addto = Main
 * <font color="green"># the position at which the custom view is inserted withing the enclosing tabbed pane
 * # a negative value means insert at the end; defaults to -1 (insert at the end)</font>
 * jppf.admin.console.view.MyView.position = 1
 * <font color="green"># whether to automatically select the view on startup; defaults to false</font>
 * jppf.admin.console.view.MyView.autoselect = true
 * </pre>
 * @author Laurent Cohen
 */
public class MyView extends PluggableView {
  /**
   * The maximum number of entries in the log.
   */
  private static final int MAX_MESSAGES = JPPFConfiguration.getProperties().getInt("org.jppf.example.pluggableview.max_log_lines", 1_000);
  /**
   * The view's backgroud color.
   */
  private static final Color BKG = new Color(232, 234, 253);
  /**
   * The Swing component enclosing the view.
   */
  private JPanel mainPanel = null;
  /**
   * A JList where the log entries are displayed
   */
  private JList<String> logView;
  /**
   * The model associated with the {@link JList}.
   */
  private DefaultListModel<String> listModel;
  /**
   * The scroll pane enclosing the {@link JList}.
   */
  private JScrollPane listScroller;
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
    if (mainPanel == null) {
      try {
        mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEtchedBorder());
        mainPanel.setBackground(BKG);
        mainPanel.add(Box.createVerticalStrut(5));
        // add the JPPF logo
        mainPanel.add(createLogoPanel());
        mainPanel.add(Box.createVerticalStrut(5));
        // add the text area that displays the log messages
        mainPanel.add(createLogView());
        mainPanel.add(Box.createVerticalStrut(5));
        // add the action buttons
        mainPanel.add(createButtonsPanel());
        // susbcribe this view to topology events to update the log accordingly
        getTopologyManager().addTopologyListener(new MyTopologyListener());
        // subscribe this view to job monitoring events to update the log accordingly
        getJobMonitor().addJobMonitoringListener(new MyJobMonitorListener());
      } catch(Throwable t) {
        t.printStackTrace();
      }
    }
    return mainPanel;
  }

  /**
   * Create the logo area.
   * @return the {@link JComponent} enclosing the text area.
   */
  private JComponent createLogoPanel() {
    JPanel logoPanel = new JPanel();
    logoPanel.setBackground(BKG);
    logoPanel.setLayout(new BoxLayout(logoPanel, BoxLayout.Y_AXIS));
    // add the JPPF logo
    logoPanel.add(new JLabel(GuiUtils.loadIcon("/jppf_logo.gif")));
    // add a formatted text just below the logo
    JLabel label = new JLabel("JPPF pluggable view");
    label.setForeground(new Color(109, 120, 182));
    Font font = label.getFont();
    label.setFont(new Font("Arial", Font.BOLD, 2*font.getSize()));
    logoPanel.add(label);
    return logoPanel;
  }

  /**
   * Create the text area which displays the log messages.
   * It is enclosed within a {@link JScrollPane} with a border and title.
   * @return the {@link JComponent} enclosing the text area.
   */
  private JComponent createLogView() {
    logView = new JList<>(listModel = new DefaultListModel<>());
    logView.setBackground(BKG);
    listScroller = new JScrollPane(logView);
    listScroller.setBorder(BorderFactory.createTitledBorder("Grid Events"));
    listScroller.setBackground(BKG);
    return listScroller;
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
          listModel.clear();
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
        StringBuilder sb = new StringBuilder();
        synchronized(MyView.this) {
          Enumeration<?> elements =  listModel.elements();
          while (elements.hasMoreElements()) {
            sb.append(elements.nextElement()).append('\n');
          }
        }
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(new StringSelection(sb.toString()), null);
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
    Dimension d = new Dimension(2800, 48);
    panel.setPreferredSize(d);
    panel.setMaximumSize(d);
    panel.setMinimumSize(new Dimension(100, 48));
    return panel;
  }

  /**
   * This method adds a new entry to the messages log.
   * @param message the message to add.
   */
  private void newMessage(final String message) {
    // create and format a timestamp
    String date = sdf.format(new Date());
    // add the new, timestamped message to the log
    String formatted = String.format("[%s] %s", date, message);
    synchronized(this) {
      logSize++;
      // if number of log entries > max, remove the oldest ones
      while (logSize > MAX_MESSAGES) {
        logSize--;
        listModel.remove(0);
      }
      // add the log entry to the JList
      listModel.addElement(formatted);
      // scroll to the end of the log
      JScrollBar scrollBar = listScroller.getVerticalScrollBar();
      scrollBar.setValue(scrollBar.getMaximum());
    }
  }

  /**
   * Our topology events listener implementation.
   */
  private class MyTopologyListener extends TopologyListenerAdapter {
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

  /**
   * Our job monitoring events listener implementation.
   * <br>Driver events are not processed here, since the topology listener already handles them.
   */
  private class MyJobMonitorListener extends JobMonitoringListenerAdapter {
    @Override
    public void jobAdded(final JobMonitoringEvent event) {
      Job job = event.getJob();
      String message = String.format("added job '%s' to driver %s", job.getDisplayName(), event.getJobDriver().getDisplayName());
      newMessage(message);
    }

    @Override
    public void jobRemoved(final JobMonitoringEvent event) {
      Job job = event.getJob();
      // we can't use job.getJobDriver(), since the job is already removed from its parent driver
      String message = String.format("removed job '%s' from driver %s", job.getDisplayName(), event.getJobDriver().getDisplayName());
      newMessage(message);
    }

    @Override
    public void jobDispatchAdded(final JobMonitoringEvent event) {
      JobDispatch dispatch = event.getJobDispatch();
      TopologyNode node = dispatch.getNode();
      String message = String.format("job '%s' dispatched to %s %s by driver %s",
        event.getJob().getDisplayName(), node.isPeer() ? "peer node" : "node", dispatch.getDisplayName(), event.getJobDriver().getDisplayName());
      newMessage(message);
    }

    @Override
    public void jobDispatchRemoved(final JobMonitoringEvent event) {
      JobDispatch dispatch = event.getJobDispatch();
      TopologyNode node = dispatch.getNode();
      // we can't use dispatch.getJob(), since the job dispatch is already removed from its parent job
      String message = String.format("job '%s' returned from %s %s to driver %s", event.getJob().getDisplayName(),
        node.isPeer() ? "peer node" : "node", dispatch.getDisplayName(), event.getJobDriver().getDisplayName());
      newMessage(message);
    }
  }
}
