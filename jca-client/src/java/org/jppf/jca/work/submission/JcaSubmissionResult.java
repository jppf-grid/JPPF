/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.jca.work.submission;

import java.util.*;

import org.jppf.client.*;
import org.jppf.client.event.*;
import org.slf4j.*;

/**
 * This class encapsulates the results of an asynchronous tasks submission.
 * @author Laurent Cohen
 */
public class JcaSubmissionResult extends JPPFResultCollector
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JcaSubmissionResult.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private boolean debugEnabled = log.isDebugEnabled();
  /**
   * The status of this submission.
   */
  private SubmissionStatus status = SubmissionStatus.SUBMITTED;
  /**
   * The unique id of this submission.
   */
  private String id = null;
  /**
   * List of listeners registered to receive this submission's status change notifications.
   */
  private final List<SubmissionStatusListener> listeners = new ArrayList<SubmissionStatusListener>();

  /**
   * Initialize this collector.
   * @param job the job to execute.
   */
  JcaSubmissionResult(final JPPFJob job)
  {
    super(job);
    this.id = job.getUuid();
  }

  /**
   * Get the status of this submission.
   * @return a {@link SubmissionStatus} enumerated value.
   */
  public synchronized SubmissionStatus getStatus()
  {
    return status;
  }

  /**
   * Set the status of this submission.
   * @param status a {@link SubmissionStatus} enumerated value.
   */
  public synchronized void setStatus(final SubmissionStatus status)
  {
    if (debugEnabled) log.debug("submission [" + id + "] status changing from '" + this.status + "' to '" + status + "'");
    this.status = status;
    fireStatusChangeEvent(this.id, this.status);
  }

  /**
   * Get the unique id of this submission.
   * @return the id as a string.
   */
  public String getId()
  {
    return id;
  }

  /**
   * Add a listener to the list of status listeners.
   * @param listener the listener to add.
   */
  public void addSubmissionStatusListener(final SubmissionStatusListener listener)
  {
    synchronized(listeners)
    {
      if (debugEnabled) log.debug("submission [" + id + "] adding status listener " + listener);
      if (listener != null) listeners.add(listener);
    }
  }

  /**
   * Remove a listener from the list of status listeners.
   * @param listener the listener to remove.
   */
  public void removeSubmissionStatusListener(final SubmissionStatusListener listener)
  {
    synchronized(listeners)
    {
      if (debugEnabled) log.debug("submission [" + id + "] removing status listener " + listener);
      if (listener != null) listeners.remove(listener);
    }
  }

  /**
   * Notify all listeners of a change of status for this submission.
   * @param id the id for submission event.
   * @param status the status for submission event.
   */
  protected void fireStatusChangeEvent(final String id, final SubmissionStatus status)
  {
    synchronized(listeners)
    {
      if (listeners.isEmpty()) return;
      if (debugEnabled) log.debug("submission [" + id + "] fire status changed event for '" + status + "'");
      SubmissionStatusEvent event = new SubmissionStatusEvent(id, status);
      for (SubmissionStatusListener listener: listeners)
      {
        listener.submissionStatusChanged(event);
      }
    }
  }

  /**
   * Reset this submission result for new submission of the same tasks.
   */
  /*
	synchronized void reset()
	{
		resultMap.clear();
		results = null;
		count = job.getTasks().size() - job.getResultMap().size();
		pendingCount = count;
	}
   */
}
