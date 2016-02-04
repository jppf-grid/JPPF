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
package sample.test;

import org.jppf.node.protocol.AbstractTask;

/**
 * @author Laurent Cohen
 */
public class TemplateJPPFTask extends AbstractTask<String>
{
  /**
   * Some Id.
   */
  private int taskId = 0;

  /**
   * Perform initializations on the client side,
   * before the task is executed by the node.
   * @param taskId Some Id.
   */
  public TemplateJPPFTask(final int taskId)
  {
    // perform initializations here ...
    this.taskId = taskId;
  }

  /**
   * This method contains the code that will be executed by a node.
   */
  @Override
  public void run()
  {
    try
    {
      // write your task code here.
      System.out.println("Hello, this is the node executing a template JPPF task");

      for (int i = 0; i < 10; i++)
      {
        System.out.println(i);
        try {Thread.sleep(1000);} catch (Exception e) {System.out.println(e);}
      }
      // eventually set the execution results
      setResult("the execution was performed successfully");
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }
}
