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

package org.jppf.dotnet;

import org.jppf.client.event.*;

/**
 * This class wraps a .Net job listener to which job event notifications are delegated.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class DotnetJobListenerWrapper extends AbstractDotnetListenerWrapper implements JobListener {
  /**
   * Initialize this wrapper with the specified proxy to a .Net job listener.
   * @param dotnetListener a proxy to a .Net job listener.
   */
  public DotnetJobListenerWrapper(final system.Object dotnetListener) {
    super(false, dotnetListener, "JobStarted", "JobEnded", "JobDispatched", "JobReturned");
    //System.out.printf("Creating job listener with dotnetListener=%s, class=%s%n", dotnetListener, dotnetListener.getClass());
  }

  @Override
  public void jobStarted(final JobEvent event) {
    delegate(event, "JobStarted");
  }

  @Override
  public void jobEnded(final JobEvent event) {
    delegate(event, "JobEnded");
  }

  @Override
  public void jobDispatched(final JobEvent event) {
    delegate(event, "JobDispatched");
  }

  @Override
  public void jobReturned(final JobEvent event) {
    delegate(event, "JobReturned");
  }
}
