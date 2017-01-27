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

package test.org.jppf.test.setup.common;

import java.util.*;

import org.jppf.client.JobStatus;
import org.jppf.client.event.*;

/**
 * A <code>SubmissionStatusListener</code> which collects all the events it receives
 * in a list of statuses.
 * @author Laurent Cohen
 */
public class GatheringStatusListener implements JobStatusListener
{
  /**
   * The statuses collected by this listener over time.
   */
  private final List<JobStatus> statuses = new ArrayList<>();

  @Override
  public void jobStatusChanged(final JobStatusEvent event)
  {
    //System.out.println("adding status '" + event.getStatus() + "'");
    statuses.add(event.getStatus());
  }

  /**
   * The statuses collected by this listener over time.
   * @return a list of <code>SubmissionStatus</code> objects.
   */
  public List<JobStatus> getStatuses()
  {
    return statuses;
  }
}
