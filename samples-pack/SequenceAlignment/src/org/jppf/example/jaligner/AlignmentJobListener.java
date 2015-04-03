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

package org.jppf.example.jaligner;

import java.util.List;

import javax.swing.SwingUtilities;

import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.slf4j.*;

/**
 * Result collector that updates the progress bar's value during the computation.
 */
public class AlignmentJobListener extends JobListenerAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AlignmentJobListener.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Total number of expected results.
   */
  private int initialCount = 0;
  /**
   * Count of results not yet received.
   */
  private int pendingCount = 0;

  /**
   * Initialize this collector with a specified number of tasks.
   * @param count the count of submitted tasks.
   */
  public AlignmentJobListener(final int count) {
    this.pendingCount = count;
    this.initialCount = count;
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param event a notification of completion for a set of submitted tasks.
   */
  @Override
  public synchronized void jobReturned(final JobEvent event) {
    List<Task<?>> tasks = event.getJobTasks();
    if (debugEnabled) log.debug("Received results for " + tasks.size() + " tasks");
    pendingCount -= tasks.size();
    final int n = (100 * (initialCount-pendingCount)) / initialCount;
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        SequenceAlignmentRunner.updateProgress(n);
      }
    });
  }
}
