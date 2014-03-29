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

package test.serialization;

import java.security.PrivilegedAction;
import java.util.List;

import org.jppf.classloader.*;
import org.jppf.node.event.*;
import org.jppf.node.protocol.TaskBundle;
import org.jppf.server.node.*;

/**
 * 
 */
public class MyNodeListener extends NodeLifeCycleListenerAdapter {
  /**
   * Unique identifier for the client that submitted the previous job.
   */
  private List<String> prevUuidPath = null;

  @Override public void jobEnding(final NodeLifeCycleEvent event) {
    TaskBundle bundle = (TaskBundle) event.getJob();
    prevUuidPath = bundle.getUuidPath().getList();
  }

  @Override public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
    if (prevUuidPath != null) {
      String uuid = event.getJob().getUuid();
      final JPPFNode node = (JPPFNode) event.getNode();
      try {
        // get the previously used class loader
        JPPFContainer cont = node.getContainer(prevUuidPath);
        final AbstractJPPFClassLoader prevCL = cont.getClassLoader();
        // create and initialize a new class loader
        AbstractJPPFClassLoader newCL = new PrivilegedAction<AbstractJPPFClassLoader>() {
          @Override public AbstractJPPFClassLoader run() {
            AbstractJPPFClassLoader newCL;
            // create a local or remote class loader based on the type of node
            if (node.isLocal()) newCL = new JPPFLocalClassLoader(prevCL.getConnection(), prevCL.getParent(), prevUuidPath);
            else newCL = new JPPFClassLoader(prevCL.getConnection(), prevCL.getParent(), prevUuidPath);
            return newCL;
          }
        }.run();
        newCL.setRequestUuid(prevCL.getRequestUuid());
        cont.setClassLoader(newCL);
        // close the previously used class loader
        prevCL.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
