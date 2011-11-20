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
package org.jppf.ui.monitoring.job.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.jppf.ui.monitoring.job.JobData;
import org.slf4j.*;

/**
 * This action suspends a job.
 */
public abstract class AbstractSuspendJobAction extends AbstractJobAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractSuspendJobAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines if the suspended sub-job should be requeued on the driver side.
   */
  protected boolean requeue = true;

  /**
   * Initialize this action.
   */
  public AbstractSuspendJobAction()
  {
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements)
  {
    super.updateState(selectedElements);
    if (jobDataArray.length > 0)
    {
      for (JobData data: jobDataArray)
      {
        if (!data.getJobInformation().isSuspended())
        {
          setEnabled(true);
          return;
        }
      }
    }
    setEnabled(false);
  }

  /**
   * Perform the action.
   * @param event not used.
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(final ActionEvent event)
  {
    Runnable r = new Runnable()
    {
      @Override
      public void run()
      {
        for (JobData data: jobDataArray)
        {
          try
          {
            data.getJmxWrapper().suspendJob(data.getJobInformation().getJobUuid(), requeue);
          }
          catch(Exception e)
          {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    new Thread(r).start();
  }
}
