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

package org.jppf.example.fractals.moviegenerator.screensaver;

import java.awt.*;
import java.text.DecimalFormat;

import javax.swing.*;

/**
 * 
 * @author Laurent Cohen
 */
public class FractalProgressPanel extends JPanel {
  /**
   * Displays the progress ratio.
   */
  private JLabel label;
  /**
   * Total number of tasks in the current job.
   */
  private int totalTasks;
  /**
   * Number of executed tasks.
   */
  private int currentTasks;
  /**
   * 
   */
  private final DecimalFormat df = new DecimalFormat("##0.0 %");
  /**
   * 
   */
  private double ratio = 0.5d;

  /**
   * 
   */
  public FractalProgressPanel() {
    super(true);
    GridBagLayout g = new GridBagLayout();
    setLayout(g);
    GridBagConstraints c = new GridBagConstraints();
    c.gridy = 0;
    setBackground(Color.BLACK);
    Dimension dim = new Dimension(400, 20);
    setSize(dim);
    setPreferredSize(dim);
    setMinimumSize(dim);
    label = new JLabel(df.format(ratio));
    label.setForeground(Color.WHITE);
    double d = 0d;
    g.setConstraints(label, c);
    add(label);
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    double r = getRatio();
    g.setColor(Color.GREEN.darker());
    g.fillRect(0, 0, (int) (400 * r), 20);
  }

  /**
   * 
   * @return the ratio of current tasks / total tasks.
   */
  private synchronized double getRatio() {
    return ratio;
  }

  /**
   * Increment the current number of tasks by 1.
   */
  public synchronized void incNbTasks() {
    currentTasks++;
    ratio = (totalTasks > 0) ? (double) totalTasks  / (double) currentTasks : 0d;
    label.setText(df.format(ratio));
  }

  /**
   * .
   * @param n the number of tasks in the new job.
   */
  public synchronized void updateTotalTasks(final int n) {
    totalTasks = n;
    currentTasks = 0;
    ratio = 0d;
    label.setText(df.format(ratio));
  }
}
