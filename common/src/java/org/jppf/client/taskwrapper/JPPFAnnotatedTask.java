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

package org.jppf.client.taskwrapper;

import java.util.concurrent.Callable;

import org.jppf.JPPFException;
import org.jppf.server.protocol.JPPFTask;


/**
 * JPPF task wrapper for an object whose class is annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}.
 * @author Laurent Cohen
 */
public class JPPFAnnotatedTask extends JPPFTask
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Wrapper around a task that is not an instance of {@link org.jppf.server.protocol.JPPFTask JPPFTask}.
   */
  protected TaskObjectWrapper taskObjectWrapper = null;

  /**
   * Initialize this task with an object whose class is either annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable},
   * an instance of {@link java.lang.Runnable Runnable} or  an instance of {@link java.util.concurrent.Callable Callable}.
   * @param taskObject an object that encapsulates the task to execute.
   * @param args the optional arguments for a class that has one of its methods annotated with {@link org.jppf.server.protocol.JPPFRunnable JPPFRunnable}.
   * @throws JPPFException if an error is raised while initializing this task.
   */
  public JPPFAnnotatedTask(final Object taskObject, final Object...args) throws JPPFException
  {
    if (taskObject instanceof Runnable) taskObjectWrapper = new RunnableTaskWrapper((Runnable) taskObject);
    else if (taskObject instanceof Callable) taskObjectWrapper = new CallableTaskWrapper((Callable) taskObject);
    else taskObjectWrapper = new AnnotatedTaskWrapper(taskObject, args);
  }

  /**
   * Initialize this task from a POJO, given a method and its arguments to execute it.
   * @param taskObject either an instance of the POJO class if the method is non-static, or a class object if the method is static.
   * @param method the name of the method to execute.
   * @param args the arguments for the method to execute.
   * @throws JPPFException if an error is raised while initializing this task.
   */
  public JPPFAnnotatedTask(final Object taskObject, final String method, final Object...args) throws JPPFException
  {
    taskObjectWrapper = new PojoTaskWrapper(method, taskObject, args);
  }

  /**
   * Run the <code>JPPFRunnable</code>-annotated method of the task object.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      Object result = taskObjectWrapper.execute();
      setResult(result);
    }
    catch(Exception e)
    {
      setException(e);
    }
  }

  /**
   * Get the <code>JPPFRunnable</code>-annotated object or POJO wrapped by this task.
   * @return an object or class that is JPPF-annotated.
   * @see org.jppf.server.protocol.JPPFTask#getTaskObject()
   */
  @Override
  public Object getTaskObject()
  {
    return taskObjectWrapper.getTaskObject();
  }
}
