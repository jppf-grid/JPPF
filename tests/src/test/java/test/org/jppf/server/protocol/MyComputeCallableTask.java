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

package test.org.jppf.server.protocol;

import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.*;

/**
 * A simple Task which calls its <code>compute()</code> method.
 */
public class MyComputeCallableTask extends AbstractTask<Object> {
  /** */
  private static final long serialVersionUID = 1L;
  /**
   * The class name for the callable to invoke.
   */
  private final String callableClassName;
  /**
   * The uuid of the node obtained via {@code Task.getNode().getUuid()}.
   */
  public String uuidFromNode, nodeUuid;

  /** */
  public MyComputeCallableTask() {
    this.callableClassName = null;
  }

  /**
   * @param callableClassName the class name for the callable to invoke.
   */
  public MyComputeCallableTask(final String callableClassName) {
    this.callableClassName = callableClassName;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void run() {
    try {
      System.out.printf("this task's class loader = %s\n", getClass().getClassLoader());
      if (callableClassName != null) {
        final Class<?> clazz = Class.forName(callableClassName);
        final JPPFCallable<String> callable = (JPPFCallable<String>) clazz.newInstance();
        final String s = compute(callable);
        System.out.printf("result of MyCallable.call() = %s\n", s);
        setResult(s);
      } else {
        final boolean b = isInNode();
        System.out.printf("isInNode() = %b\n", b);
        setResult(b);
      }
      nodeUuid = isInNode() ? getNode().getConfiguration().getString("jppf.node.uuid") : JPPFConfiguration.getProperties().getString("jppf.node.uuid");
      if (isInNode()) uuidFromNode = getNode().getUuid();
      System.out.printf("this task's nodeUuid = %s, uuidFromNode = %s\n", nodeUuid, uuidFromNode);
    } catch (final Exception e) {
      setThrowable(e);
    }
  }
}
