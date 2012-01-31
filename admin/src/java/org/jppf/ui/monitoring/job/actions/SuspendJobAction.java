/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import org.slf4j.*;


/**
 * This action suspends a job and causes all sub-jobs currently executing to lwft executing until completion.
 */
public class SuspendJobAction extends AbstractSuspendJobAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SuspendJobAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this action.
   */
  public SuspendJobAction()
  {
    setupIcon("/org/jppf/ui/resources/suspend.gif");
    putValue(NAME, localize("job.suspend.label"));
    requeue = false;
  }
}
