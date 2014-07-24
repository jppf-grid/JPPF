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
package org.jppf.application.template;

import org.jppf.node.protocol.AbstractTask;

/**
 * This class is a template for a standard JPPF task.
 * There are 3 parts to a task that is to be executed on a JPPF grid:
 * <ol>
 * <li>the task initialization: this is done on the client side, generally from the task constructor,
 * or via explicit method calls on the task from the application runner.</li>
 * <li>the task execution: this part is performed by the node. It consists in invoking the {@link #run() run()} method,
 * and handling an eventual uncaught {@link java.lang.Throwable Throwable} that would result from this invocation.</li>
 * <li>getting the execution results: the task itself, after its execution, is considered as the result.
 * JPPF provides the convenience methods {@link org.jppf.server.protocol.JPPFTask#setResult(java.lang.Object) setResult(Object)} and
 * {@link org.jppf.server.protocol.JPPFTask#getResult() getResult()}
 * to this effect, however any accessible attribute of the task will be available when the task is returned to the client.</li>
 * </ol>
 * @author Laurent Cohen
 */
public class TemplateJPPFTask extends AbstractTask<String> {
  /**
   * Perform initializations on the client side,
   * before the task is executed by the node.
   */
  public TemplateJPPFTask() {
    // perform initializations here ...
  }

  /**
   * This method contains the code that will be executed by a node.
   * Any uncaught {@link java.lang.Throwable Throwable} will be handled as follows:
   * <ul>
   * <li>if the {@link java.lang.Throwable Throwable} is an instance of {@link java.lang.Exception Exception},
   * it will be stored in the task via a call to {@link org.jppf.server.protocol.JPPFTask#setException(java.lang.Exception) JPPFTask.setException(Exception)}</li>
   * <li>otherwise, it will first be wrapped in a {@link org.jppf.JPPFException JPPFException},
   * then this <code>JPPFException</code> will be stored in the task via a call to {@link org.jppf.server.protocol.JPPFTask#setException(java.lang.Exception) JPPFTask.setException(Exception)}</li>
   * </ul>
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    // write your task code here.
    System.out.println("Hello, this is the node executing a template JPPF task");

    // ...

    // eventually set the execution results
    setResult("the execution was performed successfully");
  }
}
