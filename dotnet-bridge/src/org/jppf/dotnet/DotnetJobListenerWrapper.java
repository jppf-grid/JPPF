/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.lang.reflect.Method;
import java.util.List;

import org.jppf.client.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This class wraps a .Net job listener to which job event notifications are delegated.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class DotnetJobListenerWrapper implements JobListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DotnetJobListenerWrapper.class);
  /**
   * A proxy to a .Net job listener.
   */
  private Object dotnetListener;

  /**
   * Initialize this wrapper with the specified proxy to a .Net job listener.
   * @param dotnetListener a proxy to a .Net job listener.
   */
  public DotnetJobListenerWrapper(final system.Object dotnetListener) {
    if (dotnetListener == null) throw new IllegalArgumentException(".Net listener cannot be null");
    //System.out.printf("Creating job listener with dotnetListener=%s, class=%s%n", dotnetListener, dotnetListener.getClass());
    this.dotnetListener = dotnetListener;
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

  /**
   * Delegate the specified event to the specified .Net method
   * @param event the event to send.
   * @param method name the name of the method to invoke on the proxy to the .Net listener.
   */
  private void delegate(final JobEvent event, final String methodName) {
    if (dotnetListener == null) return;
    try {
      int[] positions = null;
      List<Task<?>> tasks = event.getJobTasks();
      if (tasks != null) {
        positions = new int[tasks.size()];
        for (int i=0; i<tasks.size(); i++) positions[i] = tasks.get(i).getPosition();
      }
      Class<?> clazz = dotnetListener.getClass();
      Method m = clazz.getMethod(methodName, int[].class);
      m.invoke(dotnetListener, positions);
    } catch (Exception e) {
      log.error("error invoking {}() : {}", methodName, ExceptionUtils.getStackTrace(e));
    }
  }
}
