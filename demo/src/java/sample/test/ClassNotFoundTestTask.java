/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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


/**
 * Test task to test that the framework behaves correctly when a class is not found
 * by the classloader.
 * @author Laurent Cohen
 */
public class ClassNotFoundTestTask extends JPPFTestTask
{
  /**
   * Initialize this task.
   */
  public ClassNotFoundTestTask()
  {
  }

  /**
   * Execute the task.
   * @see java.lang.Runnable#run()
   */
  public void test()
  {
    //new org.ujac.ui.editor.TextArea();
    String s = "Please make sure the library 'ujac-ui.jar' is NOT present in the node, server or client classpath";
    setResult(s);
    System.out.println(s);
  }
}
