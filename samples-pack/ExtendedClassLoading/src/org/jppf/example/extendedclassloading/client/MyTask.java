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

package org.jppf.example.extendedclassloading.client;

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class MyTask extends JPPFTask
{
  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      // we use the fully qualified name of the class to avoid having an import statement,
      // otherwise, this would cause a NoClassDefFoundError when loading this class
      // MyServerDynamicClass was added to the classpath by the node startup
      new org.jppf.example.extendedclassloading.clientlib1.MyClientDynamicClass1().printHello();
      new org.jppf.example.extendedclassloading.clientlib2.MyClientDynamicClass2().printHello();
      setResult("Successful execution");
    }
    catch (Exception e)
    {
      e.printStackTrace();
      setException(e);
    }
  }
}
