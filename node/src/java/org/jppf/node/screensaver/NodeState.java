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
package org.jppf.node.screensaver;

import java.awt.*;
import java.awt.event.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.*;

import org.jppf.node.event.*;

/**
 * Instances of this class represent information about a node.
 */
class NodeState implements NodeLifeCycleListener
{
  /**
   * Contains the threads in which the nodes run.
   */
  public NodeThread nodeThread = null;
  /**
   * Number of tasks executed by the node.
   */
  public AtomicInteger taskCount = new AtomicInteger(0);
  /**
   * Holds the statuses for the node connection and tasks execution.
   */
  public boolean[][] status = new boolean[2][2];
  /**
   * These labels contain the status icons for the nodes connection and task execution activity.
   * Each status is represented by a green light and a red light, each light dark or bright depending on the node status.
   */
  public JLabel[][] statusLabels = new JLabel[2][2];
  /**
   * Labels used to display the number of tasks executed by each node.
   */
  public JLabel countLabel = null;
  /**
   * Label used to display how long the node has been active.
   */
  public JLabel timeLabel = null;
  /**
   * Buttons used to start and stop the node.
   */
  public JButton[] btn = new JButton[2];
  /**
   * Determine whether the node has already been started at least once.
   */
  public boolean startedOnce = false;
  /**
   * The time this panel was started.
   */
  public long startedAt = 0L;

  /**
   * Initialize this node state.
   */
  public NodeState()
  {
    startedAt = System.currentTimeMillis();
    for (int i=0; i<statusLabels.length; i++)
    {
      statusLabels[i][0] = new JLabel(NodePanel.DARK_GREEN);
      statusLabels[i][1] = new JLabel(NodePanel.BRIGHT_RED);
    }
    Dimension d = new Dimension(8, 8);
    for (JLabel[] statusLabel : statusLabels) {
      for (JLabel aStatusLabel : statusLabel) {
        aStatusLabel.setMinimumSize(d);
        aStatusLabel.setMaximumSize(d);
        aStatusLabel.setBackground(Color.BLACK);
      }
    }
    countLabel = new JLabel("" + taskCount);
    d = new Dimension(60, 20);
    countLabel.setMinimumSize(d);
    countLabel.setMaximumSize(d);
    countLabel.setBackground(Color.BLACK);
    countLabel.setForeground(Color.WHITE);

    timeLabel = new JLabel("Active for: "+NodePanel.toStringDuration(0));
    timeLabel.setBackground(Color.BLACK);
    timeLabel.setForeground(Color.WHITE);
    nodeThread = new NodeThread(this);
    btn[0] = new JButton("Start");
    btn[0].addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(final ActionEvent event)
      {
        startNode();
      }
    });

    btn[1] = new JButton("Stop");
    btn[1].addActionListener(new ActionListener()
    {
      @Override
      public void actionPerformed(final ActionEvent event)
      {
        stopNode();
      }
    });
    btn[1].setEnabled(false);
  }

  /**
   * Start the node.
   */
  public void startNode()
  {
    btn[0].setEnabled(false);
    btn[1].setEnabled(true);
    if (!startedOnce)
    {
      startedOnce = true;
      nodeThread.start();
    }
    else nodeThread.startNode();
  }

  /**
   * Stop the node.
   */
  public void stopNode()
  {
    btn[0].setEnabled(true);
    btn[1].setEnabled(false);
    nodeThread.stopNode();
  }

  /**
   * Called when the underlying node sends an event notification.
   * @param event the event that triggered the call to this method.
   * @see org.jppf.node.event.NodeListener#eventOccurred(org.jppf.node.event.NodeEvent)
   */
  /*
	@Override
	public void eventOccurred(NodeEvent event)
	{
		switch (event.getType())
		{
			case START_CONNECT:
				statusLabels[0][0].setIcon(NodePanel.DARK_GREEN);
				statusLabels[0][1].setIcon(NodePanel.BRIGHT_RED);
				break;

			case END_CONNECT:
				statusLabels[0][0].setIcon(NodePanel.BRIGHT_GREEN);
				statusLabels[0][1].setIcon(NodePanel.DARK_RED);
				break;

			case DISCONNECTED:
				statusLabels[0][0].setIcon(NodePanel.DARK_GREEN);
				statusLabels[0][1].setIcon(NodePanel.BRIGHT_RED);
				statusLabels[1][0].setIcon(NodePanel.DARK_GREEN);
				statusLabels[1][1].setIcon(NodePanel.DARK_RED);
				break;

			case START_EXEC:
				statusLabels[1][0].setIcon(NodePanel.BRIGHT_GREEN);
				statusLabels[1][1].setIcon(NodePanel.DARK_RED);
				break;

			case END_EXEC:
				statusLabels[1][0].setIcon(NodePanel.DARK_GREEN);
				statusLabels[1][1].setIcon(NodePanel.BRIGHT_RED);
				break;

			case TASK_EXECUTED:
				int n = taskCount.incrementAndGet();
				countLabel.setText(Integer.toString(n));
				break;
		}
	}
   */

  /**
   * {@inheritDoc}
   */
  @Override
  public void nodeStarting(final NodeLifeCycleEvent event)
  {
    statusLabels[0][0].setIcon(NodePanel.BRIGHT_GREEN);
    statusLabels[0][1].setIcon(NodePanel.DARK_RED);
    statusLabels[1][0].setIcon(NodePanel.DARK_GREEN);
    statusLabels[1][1].setIcon(NodePanel.BRIGHT_RED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void nodeEnding(final NodeLifeCycleEvent event)
  {
    statusLabels[0][0].setIcon(NodePanel.DARK_GREEN);
    statusLabels[0][1].setIcon(NodePanel.BRIGHT_RED);
    statusLabels[1][0].setIcon(NodePanel.DARK_GREEN);
    statusLabels[1][1].setIcon(NodePanel.DARK_RED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void jobStarting(final NodeLifeCycleEvent event)
  {
    statusLabels[1][0].setIcon(NodePanel.BRIGHT_GREEN);
    statusLabels[1][1].setIcon(NodePanel.DARK_RED);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void jobEnding(final NodeLifeCycleEvent event)
  {
    statusLabels[1][0].setIcon(NodePanel.DARK_GREEN);
    statusLabels[1][1].setIcon(NodePanel.BRIGHT_RED);
  }
}
