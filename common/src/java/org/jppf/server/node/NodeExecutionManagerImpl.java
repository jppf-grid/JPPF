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

package org.jppf.server.node;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.JPPFNodeReconnectionNotification;
import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.*;
import org.jppf.node.ThreadManager.UsedClassLoader;
import org.jppf.node.event.LifeCycleEventHandler;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFScheduleHandler;
import org.jppf.server.protocol.*;
import org.jppf.task.storage.DataProvider;
import org.jppf.utils.*;
import org.jppf.utils.configuration.ConfigurationHelper;
import org.slf4j.*;

/**
 * Instances of this class manage the execution of JPPF tasks by a node.
 * @author Laurent Cohen
 * @author Martin JANDA
 * @author Paul Woodward
 * @exclude
 */
public class NodeExecutionManagerImpl implements NodeExecutionManager
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeExecutionManagerImpl.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node that uses this execution manager.
   */
  private NodeInternal node = null;
  /**
   * Timer managing the tasks timeout.
   */
  private final JPPFScheduleHandler timeoutHandler = new JPPFScheduleHandler("Task Timeout Timer");
  /**
   * The bundle whose tasks are currently being executed.
   */
  private TaskBundle bundle = null;
  /**
   * The list of tasks to execute.
   */
  private List<Task<?>> taskList = null;
  /**
   * The uuid path of the current bundle.
   */
  private List<String> uuidList = null;
  /**
   * Holds a the tasks submitted tot he executor.
   */
  private List<NodeTaskWrapper> taskWrapperList = null;
  /**
   * Dispatches tasks notifications to registered listeners.
   */
  private final TaskExecutionDispatcher taskNotificationDispatcher;
  /**
   * Determines whether the number of threads or their priority has changed.
   */
  private final AtomicBoolean configChanged = new AtomicBoolean(true);
  /**
   * Set if the node must reconnect to the driver.
   */
  private AtomicReference<JPPFNodeReconnectionNotification> reconnectionNotification = new AtomicReference<>(null);
  /**
   * The thread manager that is used for execution.
   */
  private final ThreadManager threadManager;
  /**
   * Determines whether the current job has been cancelled.
   */
  private AtomicBoolean jobCancelled = new AtomicBoolean(false);
  /**
   * The class loader used to load the tasks and the classes they need from the client.
   */
  private UsedClassLoader usedClassLoader = null;
  /**
   * The data provider for the current job.
   */
  private DataProvider dataProvider = null;
  /**
   * The total accumulated elapsed time of the tasks in the current bundle.
   */
  private final AtomicLong accumulatedElapsed = new AtomicLong(0L);

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   */
  public NodeExecutionManagerImpl(final AbstractNode node) {
    this(node, "jppf.processing.threads", "processing.threads");
  }

  /**
   * Initialize this execution manager with the specified node.
   * @param node the node that uses this execution manager.
   * @param nbThreadsProperty the name of the property which configures the number of threads.
   * @param legacyNbThreadsProperty the legacy name of the property which configures the number of threads.
   */
  public NodeExecutionManagerImpl(final NodeInternal node, final String nbThreadsProperty, final String legacyNbThreadsProperty) {
    if (node == null) throw new IllegalArgumentException("node is null");
    this.node = node;
    taskNotificationDispatcher = new TaskExecutionDispatcher(getClass().getClassLoader());
    TypedProperties config = JPPFConfiguration.getProperties();
    ConfigurationHelper helper = new ConfigurationHelper(config);
    int poolSize = helper.getInt(nbThreadsProperty, legacyNbThreadsProperty, Runtime.getRuntime().availableProcessors());
    if (poolSize <= 0) {
      poolSize = Runtime.getRuntime().availableProcessors();
      config.setInt(nbThreadsProperty, poolSize);
    }
    log.info("running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    threadManager = createThreadManager(config, poolSize);
  }

  /**
   * Create the thread manager instance. Default is {@link ThreadManagerThreadPool}.
   * @param config The JPPF configuration properties.
   * @param poolSize the initial pool size.
   * @return an instance of {@link ThreadManager}.
   */
  private static ThreadManager createThreadManager(final TypedProperties config, final int poolSize)
  {
    ThreadManager result = null;
    String s = config.getString("jppf.thread.manager.class", "default");
    if (!"default".equalsIgnoreCase(s) && !"org.jppf.server.node.ThreadManagerThreadPool".equals(s) && s != null) {
      try {
        Class clazz = Class.forName(s);
        Object instance = ReflectionHelper.invokeConstructor(clazz, new Class[]{Integer.TYPE}, poolSize);
        if (instance instanceof ThreadManager) {
          result = (ThreadManager) instance;
          log.info("Using custom thread manager: " + s);
        }
      } catch(Exception e) {
        log.error(e.getMessage(), e);
      }
    }
    if (result == null) {
      log.info("Using default thread manager");
      return new ThreadManagerThreadPool(poolSize);
    }
    config.setInt("processing.threads", result.getPoolSize());
    log.info("Node running " + poolSize + " processing thread" + (poolSize > 1 ? "s" : ""));
    boolean cpuTimeEnabled = result.isCpuTimeEnabled();
    config.setBoolean("cpuTimeSupported", cpuTimeEnabled);
    log.info("Thread CPU time measurement is " + (cpuTimeEnabled ? "" : "not ") + "supported");
    return result;
  }

  @Override
  public void execute(final TaskBundle bundle, final List<Task<?>> taskList) throws Exception {
    if ((taskList == null) || taskList.isEmpty()) return;
    if (debugEnabled) log.debug("executing " + taskList.size() + " tasks");
    try {
      setup(bundle, taskList);
      if (!isJobCancelled()) {
        int count = 0;
        ExecutorCompletionService<NodeTaskWrapper> ecs = new ExecutorCompletionService<>(getExecutor());
        for (Task task : taskList) {
          if (!(task instanceof JPPFExceptionResult)) {
            if (task instanceof AbstractTask) ((AbstractTask) task).setExecutionDispatcher(taskNotificationDispatcher);
            NodeTaskWrapper taskWrapper = new NodeTaskWrapper(task, usedClassLoader.getClassLoader(), timeoutHandler);
            taskWrapperList.add(taskWrapper);
            Future<NodeTaskWrapper> f =  ecs.submit(taskWrapper, taskWrapper);
            count++;
          }
        }
        for (int i=0; i<count; i++) {
          try {
            Future<NodeTaskWrapper> future = ecs.take();
            if (!future.isCancelled()) {
              NodeTaskWrapper taskWrapper = future.get();
              JPPFNodeReconnectionNotification notif = taskWrapper.getReconnectionNotification();
              if (notif != null) {
                cancelAllTasks(true, false);
                throw notif;
              }
              taskEnded(taskWrapper);
            }
          } catch (final Exception e) {
            log.debug("Exception when executing task", e);
          }
        }
      }
    } finally {
      cleanup();
    }
  }

  @Override
  public void cancelAllTasks(final boolean callOnCancel, final boolean requeue) {
    if (debugEnabled) log.debug("cancelling all tasks with: callOnCancel=" + callOnCancel + ", requeue=" + requeue);
    if (requeue && (bundle != null)) {
      synchronized(bundle) {
        bundle.setRequeue(true);
        bundle.getSLA().setSuspended(true);
      }
    }
    if (taskWrapperList != null) {
      for (NodeTaskWrapper ntw: taskWrapperList) cancelTask(ntw, callOnCancel);
    }
  }

  /**
   * Cancel the execution of the tasks with the specified id.
   * @param taskWrapper the index of the task to cancel.
   * @param callOnCancel determines whether the onCancel() callback method of each task should be invoked.
   */
  private void cancelTask(final NodeTaskWrapper taskWrapper, final boolean callOnCancel) {
    if (debugEnabled) log.debug("cancelling task = " + taskWrapper);
    Future<?> future = taskWrapper.getFuture();
    if (!future.isDone()) {
      if (debugEnabled) log.debug("calling future.cancel(true) for task = " + taskWrapper);
      taskWrapper.cancel(callOnCancel);
      Task<?> task = taskWrapper.getTask();
      boolean interrupt = task instanceof Interruptibility ? ((Interruptibility) task).isInterruptible() : true;
      future.cancel(interrupt);
      taskWrapper.cancelTimeoutAction();
      taskEnded(taskWrapper);
    }
  }

  @Override
  public void shutdown() {
    getExecutor().shutdownNow();
    timeoutHandler.clear(true);
    taskNotificationDispatcher.close();
  }

  /**
   * Prepare this execution manager for executing the tasks of a bundle.
   * @param bundle the bundle whose tasks are to be executed.
   * @param taskList the list of tasks to execute.
   */
  @SuppressWarnings("unchecked")
  private void setup(final TaskBundle bundle, final List<Task<?>> taskList) {
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
  @SuppressWarnings("unchecked")
  private void cleanup() {
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
   * Notification sent by a node task wrapper when a task is complete.
   * @param taskWrapper the task that just ended.
   * @exclude
   */
  private void taskEnded(final NodeTaskWrapper taskWrapper) {
    long elapsedTime = taskWrapper.getElapsedTime();
    accumulatedElapsed.addAndGet(elapsedTime);
    NodeExecutionInfo info = taskWrapper.getExecutionInfo();
    long cpuTime = (info == null) ? 0L : (info.cpuTime / 1000000L);
    Task task = taskWrapper.getTask();
    taskNotificationDispatcher.fireTaskEnded(task, getCurrentJobId(), cpuTime, elapsedTime/1000000L, task.getThrowable() != null);
  }

  @Override
  public String getCurrentJobId() {
    return (bundle != null) ? bundle.getUuid() : null;
  }

  @Override
  public ExecutorService getExecutor() {
    return threadManager.getExecutorService();
  }

  @Override
  public boolean checkConfigChanged() {
    return configChanged.compareAndSet(true, false);
  }

  @Override
  public void triggerConfigChanged() {
    configChanged.compareAndSet(false, true);
  }

  @Override
  public void setThreadPoolSize(final int size) {
    if (size <= 0) {
      log.warn("ignored attempt to set the thread pool size to 0 or less: " + size);
      return;
    }
    int oldSize = getThreadPoolSize();
    threadManager.setPoolSize(size);
    int newSize = getThreadPoolSize();
    if (oldSize != newSize) {
      log.info("Node thread pool size changed from " + oldSize + " to " + size);
      JPPFConfiguration.getProperties().setProperty("jppf.processing.threads", Integer.toString(size));
      triggerConfigChanged();
    }
  }

  @Override
  public int getThreadPoolSize() {
    return threadManager.getPoolSize();
  }

  @Override
  public int getThreadsPriority() {
    return threadManager.getPriority();
  }

  @Override
  public void updateThreadsPriority(final int newPriority) {
    threadManager.setPriority(newPriority);
  }

  @Override
  public ThreadManager getThreadManager() {
    return threadManager;
  }

  @Override
  public boolean isJobCancelled() {
    return jobCancelled.get();
  }

  @Override
  public void setJobCancelled(final boolean jobCancelled) {
    this.jobCancelled.set(jobCancelled);
  }

  @Override
  public TaskBundle getBundle() {
    return bundle;
  }

  @Override
  public void setBundle(final TaskBundle bundle) {
    this.bundle = bundle;
  }

  /**
   * Get the appropiate class loader for the specfied task.
   * @param task the task from which to get the class laoder.
   * @return an instance of {@link ClassLoader}.
   */
  private ClassLoader getTaskClassLoader(final Task<?> task) {
    Object o = task.getTaskObject();
    return (o == null) ? task.getClass().getClassLoader() : o.getClass().getClassLoader();
  }

  @Override
  public TaskExecutionDispatcher getTaskNotificationDispatcher()
  {
    return taskNotificationDispatcher;
  }
}
