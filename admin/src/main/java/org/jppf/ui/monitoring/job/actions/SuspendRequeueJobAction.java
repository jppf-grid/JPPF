/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

/**
 * This action suspends a job and causes all sub-jobs currently executing to be canceled and requeued on the server.
 */
public class SuspendRequeueJobAction extends AbstractSuspendJobAction {
  /**
   * Initialize this action.
   */
  public SuspendRequeueJobAction() {
    setupIcon("/org/jppf/ui/resources/suspend_requeue.gif");
    putValue(NAME, localize("job.suspend_requeue.label"));
    requeue = true;
  }
}
