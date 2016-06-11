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

package org.jppf.example.android.demo;

import org.jppf.node.protocol.AbstractTask;

/**
 * A simple task example that is dynamically loaded and executed on an Android node.
 * This implicitely demonstrates the serialization compatibility between Java and Dalvik/Art.
 * @author Laurent Cohen
 */
public class DemoAndroidTask extends AbstractTask<String> {
  @Override
  public void run() {
    // converted to Log.i("system.out", "I am a demo Android task !!!") Logcat call
    System.out.println("I am a demo Android task !!!");
    setResult("demo Android task successful");
  }
}
