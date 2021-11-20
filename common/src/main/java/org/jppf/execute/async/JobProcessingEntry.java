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

package org.jppf.execute.async;

import java.util.List;
import java.util.concurrent.atomic.*;

import org.jppf.execute.NodeTaskWrapper;
import org.jppf.execute.ThreadManager.UsedClassLoader;
import org.jppf.node.protocol.*;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public class JobProcessingEntry extends JobPendingEntry {
  /**
   * The list of tasks to execute.
   */
  public List<Task<?>> taskList;
  /**
   * The uuid path of the current bundle.
   */
  public List<String> uuidList;
  /**
   * Holds the tasks submitted to the executor.
   */
  public List<NodeTaskWrapper> taskWrapperList;
  /**
   * The class loader used to load the tasks and the classes they need from the client.
   */
  public UsedClassLoader usedClassLoader;
  /**
   * The data provider for the current job.
   */
  public DataProvider dataProvider;
  /**
   * The total accumulated elapsed time of the tasks in the current bundle.
   */
  public final AtomicLong accumulatedElapsed = new AtomicLong(0L);
  /**
   * The execution mabager that processes the job.
   */
  public AsyncExecutionManager executionManager;
  /**
   * The number of submitted tasks.
   */
  public int submittedCount;
  /**
   * The number of completed tasks.
   */
  public final AtomicInteger resultCount = new AtomicInteger(0);
  /**
   * A {@link Throwable} that prevented or interrupted the job processing.
   */
  public Throwable t;

  /**
   * @return the class loader fot htis task bundle.
   */
  public ClassLoader getClassLoader() {
    return (usedClassLoader == null) ? null : usedClassLoader.getClassLoader();
  }
}
