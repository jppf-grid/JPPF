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

package org.jppf.client.balancer;

import java.util.*;

import org.jppf.execute.AbstractExecutionManager;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperty;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public class ClientExecutionManager extends AbstractExecutionManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ClientExecutionManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this execution manager.
   * @param config the configuration to get the thread manager properties from.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   */
  public ClientExecutionManager(final TypedProperties config, final JPPFProperty<Integer> nbThreadsProperty) {
    super(config, nbThreadsProperty);
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  @Override
  protected void setup(final TaskBundle bundle, final List<Task<?>> taskList) {
    this.bundle = bundle;
    //taskNotificationDispatcher.setBundle(bundle);
    this.taskList = taskList;
    this.taskWrapperList = new ArrayList<>(taskList.size());
    this.dataProvider = taskList.get(0).getDataProvider();
    this.uuidList = bundle.getUuidPath().getList();
    ClassLoader taskClassLoader = null;
    try {
      taskClassLoader = getTaskClassLoader(taskList.get(0));
      usedClassLoader = threadManager.useClassLoader(taskClassLoader);
    } catch (final Exception e) {
      final String msg = ExceptionUtils.getMessage(e) + " - class loader lookup failed for uuidPath=" + uuidList;
      if (debugEnabled) log.debug(msg, e);
      else log.warn(msg);
    }
    accumulatedElapsed.set(0L);
  }

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   */
  @Override
  protected void cleanup() {
    bundle.setParameter(BundleParameter.NODE_BUNDLE_ELAPSED_PARAM, accumulatedElapsed.get());
    this.dataProvider = null;
    usedClassLoader.dispose();
    usedClassLoader = null;
    //taskNotificationDispatcher.setBundle(this.bundle = null);
    this.taskList = null;
    this.uuidList = null;
    setJobCancelled(false);
    this.taskWrapperList = null;
    timeoutHandler.clear();
  }

  /**
   * Get the appropiate class loader for the specfied task.
   * @param task the task from which to get the class laoder.
   * @return an instance of {@link ClassLoader}.
   */
  private static ClassLoader getTaskClassLoader(final Task<?> task) {
    return task.getTaskClassLoader();
  }
}
