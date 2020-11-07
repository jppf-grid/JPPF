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
import org.jppf.execute.async.*;
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
public class AsyncNodeExecutionManager extends AbstractAsyncExecutionManager {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(AsyncNodeExecutionManager.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node that uses this execution manager.
   */
  private final NodeInternal node;

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   */
  public AsyncNodeExecutionManager(final NodeInternal node) {
    super(node.getConfiguration(), JPPFProperties.PROCESSING_THREADS);
    this.node = node;
  }

  @Override
  protected JobProcessingEntry setup(final BundleWithTasks bundleWithTasks) {
    final List<Task<?>> taskList = bundleWithTasks.getTasks();
    final TaskBundle bundle = bundleWithTasks.getBundle();
    if (debugEnabled) log.debug("setting up bundle {}", bundle);
    final JobProcessingEntry jobEntry = new JobProcessingEntry();
    jobEntry.bundle = bundle;
    jobEntry.taskList = taskList;
    jobEntry.taskWrapperList = new ArrayList<>(taskList.size());
    jobEntry.dataProvider = taskList.get(0).getDataProvider();
    jobEntry.uuidList = bundle.getUuidPath().getList();
    ClassLoader taskClassLoader = null;
    try {
      taskClassLoader = node instanceof ClassLoaderProvider ? ((ClassLoaderProvider) node).getClassLoader(jobEntry.uuidList) : taskList.get(0).getTaskClassLoader();
      jobEntry.usedClassLoader = threadManager.useClassLoader(taskClassLoader);
    } catch (final Exception e) {
      final String msg = ExceptionUtils.getMessage(e) + " - class loader lookup failed for uuidPath=" + jobEntry.uuidList;
      if (debugEnabled) log.debug(msg, e);
      else log.warn(msg);
    }
    jobEntry.accumulatedElapsed.set(0L);
    final LifeCycleEventHandler handler = node.getLifeCycleEventHandler();
    if (handler != null) handler.fireJobStarting(bundle, taskClassLoader instanceof AbstractJPPFClassLoader ? (AbstractJPPFClassLoader) taskClassLoader : null,
      taskList, jobEntry.dataProvider);
    if (debugEnabled) log.debug("finished setting up bundle {}", bundle);
    return jobEntry;
  }

  @Override
  protected void cleanup(final JobProcessingEntry jobEntry) {
    final TaskBundle bundle = jobEntry.bundle;
    if (debugEnabled) log.debug("cleaning up bundle {}", bundle);
    //jobEntry.bundle = null;
    bundle.setParameter(BundleParameter.NODE_BUNDLE_ELAPSED_PARAM, jobEntry.accumulatedElapsed.get());
    final ClassLoader cl = jobEntry.getClassLoader();
    final LifeCycleEventHandler handler = node.getLifeCycleEventHandler();
    if (handler != null) handler.fireJobEnding(bundle, cl instanceof AbstractJPPFClassLoader ? (AbstractJPPFClassLoader) cl : null, jobEntry.taskList, jobEntry.dataProvider);
    jobEntry.dataProvider = null;
    if (jobEntry.usedClassLoader != null) jobEntry.usedClassLoader.dispose();
    jobEntry.usedClassLoader = null;
    jobEntry.taskList = null;
    jobEntry.uuidList = null;
    jobEntry.taskWrapperList = null;
    timeoutHandler.clear();
    if (debugEnabled) log.debug("cleaned up bundle {}", bundle);
  }

  @Override
  public void triggerConfigChanged() {
    super.triggerConfigChanged();
    node.getNodeConfigNotifier().sendNotification(node.getUuid(), node.getConfiguration());
  }
}
