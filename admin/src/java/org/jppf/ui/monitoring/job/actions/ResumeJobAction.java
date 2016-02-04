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
package org.jppf.ui.monitoring.job.actions;

import java.awt.event.ActionEvent;
import java.util.List;

import org.jppf.client.monitoring.jobs.*;
import org.jppf.job.JobUuidSelector;
import org.jppf.server.job.management.DriverJobManagementMBean;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * This action suspends a job.
 */
public class ResumeJobAction extends AbstractJobAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ResumeJobAction.class);

  /**
   * Initialize this action.
   */
  public ResumeJobAction() {
    setupIcon("/org/jppf/ui/resources/resume.gif");
    putValue(NAME, localize("job.resume.label"));
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    super.updateState(selectedElements);
    if (jobDataArray.length > 0) {
      for (Job job: jobDataArray) {
        if (job.getJobInformation().isSuspended()) {
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
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    final CollectionMap<JobDriver, String> map = new SetHashMap<>();
    for (Job data : jobDataArray) map.putValue(data.getJobDriver(), data.getUuid());
    Runnable r = new Runnable() {
      @Override
      public void run() {
        for (JobDriver driver: map.keySet()) {
          try {
            DriverJobManagementMBean jmx = driver.getTopologyDriver().getJobManager();
            if (jmx != null) {
              jmx.resumeJobs(new JobUuidSelector(map.getValues(driver)));
            }
          } catch (Exception e) {
            log.error(e.getMessage(), e);
          }
        }
      }
    };
    runAction(r);
  }
}
