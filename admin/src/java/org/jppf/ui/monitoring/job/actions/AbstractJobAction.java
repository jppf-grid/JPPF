/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import java.util.*;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.ui.actions.AbstractUpdatableAction;

/**
 * Common super class for job actions.
 * @author Laurent Cohen
 */
public abstract class AbstractJobAction extends AbstractUpdatableAction {
  /**
   * Constant for an empty <code>JobData</code> array.
   */
  private static final Job[] EMPTY_JOB_ARRAY = new Job[0];
  /**
   * Constant for an empty <code>JobData</code> array.
   */
  private static final JobDispatch[] EMPTY_JOB_DISPATCH_ARRAY = new JobDispatch[0];
  /**
   * The object representing the JPPF jobs in the tree table.
   */
  protected Job[] jobDataArray = EMPTY_JOB_ARRAY;
  /**
   * The object representing the JPPF sub-jobs in the tree table.
   */
  protected JobDispatch[] subjobDataArray = EMPTY_JOB_DISPATCH_ARRAY;

  /**
   * Initialize this action.
   */
  public AbstractJobAction() {
    BASE = "org.jppf.ui.i18n.JobDataPage";
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements - a list of objects.
   * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    final List<Job> jobList = new ArrayList<>();
    final List<JobDispatch> subjobList = new ArrayList<>();
    for (final Object o : selectedElements) {
      if (o instanceof Job) jobList.add((Job) o);
      else if (o instanceof JobDispatch) subjobList.add((JobDispatch) o);
    }
    jobDataArray = jobList.toArray(new Job[jobList.size()]);
    subjobDataArray = subjobList.toArray(new JobDispatch[subjobList.size()]);
  }
}
