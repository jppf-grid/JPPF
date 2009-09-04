/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.ui.monitoring.job.actions;

import java.util.*;

import org.jppf.ui.actions.AbstractUpdatableAction;
import org.jppf.ui.monitoring.job.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractJobAction extends AbstractUpdatableAction
{
	/**
	 * The object representing the JPPF jobs in the tree table.
	 */
	protected JobData[] jobDataArray = new JobData[0];
	/**
	 * The object representing the JPPF sub-jobs in the tree table.
	 */
	protected JobData[] subjobDataArray = new JobData[0];

	/**
	 * Initialize this action.
	 */
	public AbstractJobAction()
	{
		BASE = "org.jppf.ui.i18n.JobDataPage";
	}

	/**
	 * Update this action's enabled state based on a list of selected elements.
	 * @param selectedElements - a list of objects.
	 * @see org.jppf.ui.actions.AbstractUpdatableAction#updateState(java.util.List)
	 */
	public void updateState(List<Object> selectedElements)
	{
		super.updateState(selectedElements);
		List<JobData> jobList = new ArrayList<JobData>();
		List<JobData> subjobList = new ArrayList<JobData>();
		for (Object o: selectedElements)
		{
			JobData data = (JobData) o;
			if (JobDataType.JOB.equals(data.getType())) jobList.add(data);
			else if (JobDataType.SUB_JOB.equals(data.getType())) subjobList.add(data);
		}
		jobDataArray = jobList.toArray(new JobData[0]);
		subjobDataArray = subjobList.toArray(new JobData[0]);
	}
}
