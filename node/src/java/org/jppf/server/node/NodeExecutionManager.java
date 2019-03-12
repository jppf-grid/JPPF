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

package org.jppf.server.node;

import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.execute.*;
import org.jppf.node.NodeInternal;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
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
    super(node.getConfiguration(), JPPFProperties.PROCESSING_THREADS);
    this.node = node;
  }

  @Override
  protected void setup(final TaskBundle bundle, final List<Task<?>> taskList) {
    if (debugEnabled) log.debug("setting up bundle {}", bundle);
    //taskNotificationDispatcher.setBundle(this.bundle = bundle);
    this.taskList = taskList;
    this.taskWrapperList = new ArrayList<>(taskList.size());
    this.dataProvider = taskList.get(0).getDataProvider();
    this.uuidList = bundle.getUuidPath().getList();
    ClassLoader taskClassLoader = null;
    try {
      taskClassLoader = node instanceof ClassLoaderProvider ? ((ClassLoaderProvider) node).getClassLoader(uuidList) : taskList.get(0).getTaskClassLoader();
      usedClassLoader = threadManager.useClassLoader(taskClassLoader);
    } catch (final Exception e) {
      final String msg = ExceptionUtils.getMessage(e) + " - class loader lookup failed for uuidPath=" + uuidList;
      if (debugEnabled) log.debug(msg, e);
      else log.warn(msg);
    }
    accumulatedElapsed.set(0L);
    final LifeCycleEventHandler handler = node.getLifeCycleEventHandler();
    if (handler != null) handler.fireJobStarting(bundle, taskClassLoader instanceof AbstractJPPFClassLoader ? (AbstractJPPFClassLoader) taskClassLoader : null,
      taskList, dataProvider);
    if (debugEnabled) log.debug("finished setting up bundle {}", bundle);
  }

  @Override
  protected void cleanup() {
    if (debugEnabled) log.debug("cleaning up bundle {}", bundle);
    bundle.setParameter(BundleParameter.NODE_BUNDLE_ELAPSED_PARAM, accumulatedElapsed.get());
    final ClassLoader cl = usedClassLoader.getClassLoader();
    final LifeCycleEventHandler handler = node.getLifeCycleEventHandler();
    if (handler != null) handler.fireJobEnding(bundle, cl instanceof AbstractJPPFClassLoader ? (AbstractJPPFClassLoader) cl : null, taskList, dataProvider);
    this.dataProvider = null;
    usedClassLoader.dispose();
    usedClassLoader = null;
    //taskNotificationDispatcher.setBundle(this.bundle = null);
    this.taskList = null;
    this.uuidList = null;
    setJobCancelled(false);
    this.taskWrapperList = null;
    timeoutHandler.clear();
    if (debugEnabled) log.debug("cleaned up bundle {}", bundle);
  }

  @Override
  public void triggerConfigChanged() {
    super.triggerConfigChanged();
    node.getNodeConfigNotifier().sendNotification(node.getUuid(), node.getConfiguration());
  }

  @Override
  protected void taskEnded(final NodeTaskWrapper taskWrapper) {
    // Workaround for the Android issue https://code.google.com/p/android/issues/detail?id=211596
    final Task<?> task = taskWrapper.getTask();
    final Throwable t = task.getThrowable();
    if (node.isAndroid() && (t instanceof ReflectiveOperationException)) {
      task.setThrowable(new JPPFTaskSerializationException(t));
    }
    super.taskEnded(taskWrapper);
  }
}
