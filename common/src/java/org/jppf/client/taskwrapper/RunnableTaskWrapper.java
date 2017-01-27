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
package org.jppf.client.taskwrapper;


/**
 * Task wrapper for classes implementing {@link java.lang.Runnable Runnable}.
 * @author Laurent Cohen
 * @exclude
 */
public class RunnableTaskWrapper extends AbstractTaskObjectWrapper
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The object on which to execute a method, or the class of the object if the method is static.
   */
  private Runnable runnable = null;

  /**
   * Initialize this wrapper with the specified <code>Runnable</code> object.
   * @param taskObject the runnable object to execute.
   */
  public RunnableTaskWrapper(final Runnable taskObject)
  {
    this.runnable = taskObject;
  }

  /**
   * Execute the <code>run()</code> method of this runnable task.
   * @return the result of the execution.
   * @throws Exception if an error occurs during the execution.
   * @see org.jppf.client.taskwrapper.TaskObjectWrapper#execute()
   */
  @Override
  public Object execute() throws Exception
  {
    runnable.run();
    return null;
  }

  /**
   * Return the object on which a method or constructor is called.
   * @return an object or null if the invoked method is static.
   * @see org.jppf.client.taskwrapper.TaskObjectWrapper#getTaskObject()
   */
  @Override
  public Object getTaskObject()
  {
    return runnable;
  }
}
