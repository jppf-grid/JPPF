/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
 * JPPF task used to test how exceptions are handled within the nodes.
 * @author Laurent Cohen
 */
public class OutOfMemoryTestTask extends AbstractTask<String> {
  /**
   * Default constructor .
   */
  public OutOfMemoryTestTask() {
  }

  /**
   * This method throws a <code>OutOfMemoryError</code>.
   */
  @Override
  public void run() {
    final int n = 50 * 1024 * 1024;
    final byte[][] data = new byte[n][];
    for (int i = 0; i < n; i++) {
      data[i] = new byte[10];
    }
    System.out.println("allocated all arrays");
  }
}
