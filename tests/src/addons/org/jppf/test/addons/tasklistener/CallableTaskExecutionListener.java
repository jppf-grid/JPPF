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

package org.jppf.test.addons.tasklistener;

import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.node.event.*;
import org.jppf.node.protocol.Task;

/**
 * This task listener checks whether the user object in each notification is an instance
 * of Callable and, if it is checks whether the task result is a list. If the result is a list
 * the callable is executed and its result added to the list.
 * @author Laurent Cohen
 */
public class CallableTaskExecutionListener implements TaskExecutionListener {
  @Override
  public void taskExecuted(final TaskExecutionEvent event) {
    //process(event);
  }

  @Override
  public void taskNotification(final TaskExecutionEvent event) {
    process(event);
  }

  /**
   * Process the event.
   * @param event the event to process.
   */
  @SuppressWarnings("unchecked")
  private void process(final TaskExecutionEvent event) {
    if (event.getUserObject() instanceof Callable) {
      Task<Object> task = (Task<Object>) event.getTask();
      System.out.println("Id of notifying task: " + task.getId());
      Object o = task.getResult();
      if ((o != null) && !(o instanceof List)) return;
      List<Object> list = null;
      if (o == null) {
        list = new ArrayList<>();
        task.setResult(list);
      } else list = (List<Object>) o;
      try {
        Callable<?> callable = (Callable<?>) event.getUserObject();
        list.add(callable.call());
      } catch (Exception e) {
        list.add(e);
      }
    }
  }
}
