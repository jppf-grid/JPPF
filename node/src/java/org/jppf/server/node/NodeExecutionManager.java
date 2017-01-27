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

package org.jppf.server.node;

import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.execute.*;
import org.jppf.management.NodeConfigNotifier;
import org.jppf.node.NodeInternal;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public class NodeExecutionManager extends AbstractExecutionManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeExecutionManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The node that uses this execution manager.
   */
  private final NodeInternal node;

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   */
  public NodeExecutionManager(final NodeInternal node) {
    this(node, JPPFProperties.PROCESSING_THREADS);
  }

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   */
  public NodeExecutionManager(final NodeInternal node, final JPPFProperty<Integer> nbThreadsProperty) {
    super(nbThreadsProperty);
    if (node == null) throw new IllegalArgumentException("node is null");
    this.node = node;
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  @Override
  protected void setup(final TaskBundle bundle, final List<Task<?>> taskList) {
    taskNotificationDispatcher.setBundle(this.bundle = bundle);
    this.taskList = taskList;
    this.taskWrapperList = new ArrayList<>(taskList.size());
    this.dataProvider = taskList.get(0).getDataProvider();
    this.uuidList = bundle.getUuidPath().getList();
    ClassLoader taskClassLoader = null;
    try {
      taskClassLoader = node instanceof ClassLoaderProvider ? ((ClassLoaderProvider) node).getClassLoader(uuidList) : getTaskClassLoader(taskList.get(0));
      usedClassLoader = threadManager.useClassLoader(taskClassLoader);
    } catch (Exception e) {
      String msg = ExceptionUtils.getMessage(e) + " - class loader lookup failed for uuidPath=" + uuidList;
      if (debugEnabled) log.debug(msg, e);
      else log.warn(msg);
    }
    accumulatedElapsed.set(0L);
    LifeCycleEventHandler handler = node.getLifeCycleEventHandler();
    if (handler != null) handler.fireJobStarting(bundle, taskClassLoader instanceof AbstractJPPFClassLoader ? (AbstractJPPFClassLoader) taskClassLoader : null,
      taskList, dataProvider);
  }

  /**
   * Cleanup method invoked when all tasks for the current bundle have completed.
   */
  @Override
  protected void cleanup() {
    bundle.setParameter(BundleParameter.NODE_BUNDLE_ELAPSED_PARAM, accumulatedElapsed.get());
    ClassLoader cl = usedClassLoader.getClassLoader();
    LifeCycleEventHandler handler = node.getLifeCycleEventHandler();
    if (handler != null) handler.fireJobEnding(bundle, cl instanceof AbstractJPPFClassLoader ? (AbstractJPPFClassLoader) cl : null, taskList, dataProvider);
    this.dataProvider = null;
    usedClassLoader.dispose();
    usedClassLoader = null;
    taskNotificationDispatcher.setBundle(this.bundle = null);
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
  private ClassLoader getTaskClassLoader(final Task<?> task) {
    return task.getTaskClassLoader();
  }

  @Override
  public void triggerConfigChanged() {
    super.triggerConfigChanged();
    NodeConfigNotifier.getInstance().sendNotification(node.getUuid(), JPPFConfiguration.getProperties());
  }

  @Override
  protected void taskEnded(final NodeTaskWrapper taskWrapper) {
    // Workaoround for the Android issue https://code.google.com/p/android/issues/detail?id=211596
    Task<?> task = taskWrapper.getTask();
    Throwable t = task.getThrowable();
    if (node.isAndroid() && (t instanceof ReflectiveOperationException)) {
      task.setThrowable(new JPPFTaskSerializationException(t));
    }
    super.taskEnded(taskWrapper);
  }
}
