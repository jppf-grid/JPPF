/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package test.deadlock;

import org.jppf.server.protocol.JPPFTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class SampleJPPFTask extends JPPFTask
{
  /**
   * 
   */
  private int idx = 0;

  /**
   * Perform initializations.
   * @param idx the idx.
   */
  public SampleJPPFTask(final int idx)
  {
    this.idx = idx;
  }

  /**
   * This method contains the code that will be executed by a node.
   */
  @Override
  public void run()
  {
    //System.out.println("Hello, this is the node executing a template JPPF task");
    //for (int i=0; i<10; i++) getClass().getClassLoader().getResource("this/will/not/be/found/" + i + ".txt");
    try
    {
      Thread.sleep(1);
      //getClass().getClassLoader().getResource("this/will/not/be/found/nf" + idx + ".txt");
      setResult("the execution was performed successfully");
    }
    catch (Exception e)
    {
      setException(e);
    }
  }

  @Override
  public void onCancel()
  {
    System.out.println("task id " + idx + " cancelled!");
  }
}
