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

package org.jppf.example.extendedclassloading.client;

import org.jppf.example.extendedclassloading.clientlib2.MyClientDynamicClass2;
import org.jppf.server.protocol.JPPFTask;

/**
 * A simple {@link JPPFTask} implementation which demonstrates
 * the use of classes downloaded via the library repository management facility.
 * @author Laurent Cohen
 */
public class MyTask2 extends JPPFTask {
  @Override
  public void run() {
    try {
      new MyClientDynamicClass2().printHello();
      setResult("Successful execution");
    } catch (Exception e) {
      e.printStackTrace();
      setThrowable(e);
    }
  }
}
