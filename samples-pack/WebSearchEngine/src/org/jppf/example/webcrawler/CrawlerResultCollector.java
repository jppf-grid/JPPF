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

package org.jppf.example.webcrawler;

import javax.swing.SwingUtilities;

import org.jppf.client.JPPFJob;
import org.jppf.client.JPPFResultCollector;
import org.jppf.client.event.TaskResultEvent;
import org.jppf.node.protocol.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Result collector that updates the progress bar's value during the computation.
 */
public class CrawlerResultCollector extends JPPFResultCollector
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CrawlerResultCollector.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this collector with a specified number of tasks.
   * @param job the job for which ot get the results.
   */
  public CrawlerResultCollector(final JPPFJob job)
  {
    super(job);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param event a notification of completion for a set of submitted tasks.
   * @see org.jppf.client.event.TaskResultListener#resultsReceived(org.jppf.client.event.TaskResultEvent)
   */
  @Override
  public synchronized void resultsReceived(final TaskResultEvent event)
  {
    super.resultsReceived(event);
    if (event.getThrowable() == null)
    {
      int sum = 0;
      for (Task<?> task: event.getTasks()) sum += ((CrawlerTask) task).getToVisit().size();
      final int n = sum;
      SwingUtilities.invokeLater(new Runnable()
      {
        @Override
        public void run()
        {
          WebCrawlerRunner.updateProgress(n);
        }
      });
    }
    else event.getThrowable().printStackTrace();
  }
}
