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
package test.node.nativelib;

import org.jppf.node.protocol.AbstractTask;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class NativeLibTask extends AbstractTask<String>
{
  /**
   * Perform initializations on the client side,
   * before the task is executed by the node.
   */
  public NativeLibTask()
  {
  }

  @Override
  public void run()
  {
    // write your task code here.
    System.out.println("Hello, this is the node executing a template JPPF task");

    // ...
    try
    {
      //System.out.println("java.library.path = " + System.getProperty("java.library.path"));
      String path = System.getProperty("java.library.path");
      System.setProperty("java.library.path", path + System.getProperty("path.separator") + "C:/temp");
      Class.forName("test.node.nativelib.NativeLibLoader");
      setResult("the execution was performed successfully");
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }
}
