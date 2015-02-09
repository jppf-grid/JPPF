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

package sample.test.resubmit;

import java.io.*;

import org.jppf.node.*;
import org.jppf.node.protocol.AbstractTask;

/**
 * 
 */
public class MyTask extends AbstractTask<String> {
  /**
   * Whether to kill the node.
   */
  private final boolean killNode;

  /**
   * 
   * @param killNode whether to kill the node.
   */
  public MyTask(final boolean killNode) {
    this.killNode = killNode;
  }

  @Override
  public void run() {
    try {
      File file = new File(ResubmitRunner.FILE_NAME);
      // dont kill the master !
      NodeInternal node = (NodeInternal) NodeRunner.getNode();
      int port = node.getJmxServer().getManagementPort();
      if (killNode && !file.exists() && (port != 12001)) {
        FileWriter writer = new FileWriter(file);
        writer.write("node terminated");
        writer.close();
        System.out.printf("%s created file 'nodeKilled.txt'%n", getId());
        System.exit(1);
      } else {
        Thread.sleep(2000L);
      }
      setResult("exec successful");
    } catch (Exception e) {
      setThrowable(e);
    }
  }
}
