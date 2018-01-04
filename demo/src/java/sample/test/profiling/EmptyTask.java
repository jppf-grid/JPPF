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
package sample.test.profiling;

import org.jppf.node.protocol.AbstractTask;

/**
 * Instances of this class do nothing and are intended for node profiling purposes,
 * to analyse the JPPF overhead for task execution.
 * @author Laurent Cohen
 */
public class EmptyTask extends AbstractTask<String> {
  /**
   * The data size in KB.
   */
  @SuppressWarnings("unused")
  private int dataSize = 0;
  /**
   * The data in this task.
   */
  @SuppressWarnings("unused")
  private byte[] data = null;

  /**
   * Initialize with the specified data size.
   * @param dataSize the data size in bytes.
   */
  public EmptyTask(final int dataSize) {
    this.dataSize = dataSize;
    data = new byte[dataSize];
  }

  /**
   * Perform the execution of this task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run() {
    final String[] res = { "res/res1.txt", "res/res2.txt", "res/res3.txt" };
    final ClassLoader cl = getClass().getClassLoader();
    for (final String s: res) {
      cl.getResource(s);
      //System.out.println("url for '" + s + "' : " + url);
    }
  }
}
