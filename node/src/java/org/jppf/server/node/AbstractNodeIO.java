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

import static org.jppf.node.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.io.InvalidClassException;
import java.util.*;

import org.jppf.node.protocol.*;
import org.jppf.node.protocol.graph.*;
import org.jppf.utils.LoggingUtils;
import org.jppf.utils.collections.CollectionMap;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @param <N> the type of node.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractNodeIO<N extends AbstractCommonNode> implements NodeIO {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The node who owns this TaskIO.
   */
  protected final N node;

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public AbstractNodeIO(final N node) {
    this.node = node;
    HookFactory.registerConfigSingleHook(JPPFProperties.SERIALIZATION_EXCEPTION_HOOK, SerializationExceptionHook.class, new DefaultSerializationExceptionHook(), getClass().getClassLoader());
  }

  @Override
  public BundleWithTasks readJob() throws Exception {
    try {
      final Object[] result = readObjects();
      final TaskBundle currentBundle = (TaskBundle) result[0];
      final List<Task<?>> taskList = new ArrayList<>(result.length - 2);
      if (!currentBundle.isHandshake() && (currentBundle.getParameter(NODE_EXCEPTION_PARAM) == null)) {
        final DataProvider dataProvider = (DataProvider) result[1];
        final int taskCount = currentBundle.getTaskCount();
        for (int i=0; i<taskCount; i++) {
          final Task<?> task = (Task<?>) result[2 + i];
          task.setDataProvider(dataProvider).setInNode(true).setNode(node).setJob(currentBundle);
          taskList.add(task);
        }
        final TaskGraphInfo graphInfo = currentBundle.getParameter(BundleParameter.JOB_TASK_GRAPH_INFO, null);
        final int dependencyCount = (graphInfo == null) ? 0 : graphInfo.getNbDependencies();
        if (dependencyCount > 0) {
          final CollectionMap<Integer, Integer> dependencyMapping = graphInfo.getDependenciesMap();
          final Map<Integer, Task<?>> depsByPosition = new HashMap<>();
          for (int i=0; i<dependencyCount; i++) {
            final Task<?> task = (Task<?>) result[2 + taskCount + i];
            depsByPosition.put(task.getPosition(), task);
          }
          for (final Task<?> task: taskList) {
            if (!(task instanceof TaskNode)) continue;
            final TaskNode<?> taskNode = (TaskNode<?>) task;
            final Collection<Integer> depsPositions = dependencyMapping.getValues(task.getPosition());
            if (depsPositions != null) {
              for (final Integer pos: depsPositions) {
                final TaskNode<?> dep = (TaskNode<?>) depsByPosition.get(pos);
                if (dep != null) taskNode.dependsOn(dep);
              }
            }
          }
        }
      }
      return new BundleWithTasks(currentBundle, taskList);
    } catch (final Exception|Error e) {
      if (debugEnabled) log.debug("error in readJob():", e);
      throw e;
    }
  }

  @Override
  public void writeResults(final TaskBundle bundle, final List<Task<?>> tasks) throws Exception {
    try {
      bundle.setSLA(null);
      bundle.setMetadata(null);
      sendResults(bundle, tasks);
    } finally {
      postSendResults(bundle);
    }
  }

  /**
   * Performs the actions required if reloading the classes is necessary.
   * @throws Exception if any error occurs.
   */
  protected abstract void handleReload() throws Exception;

  /**
   * Perform the deserialization of the objects received through the socket connection.
   * @return an array of objects deserialized from the socket stream.
   * @throws Exception if an error occurs while deserializing.
   */
  protected abstract Object[] deserializeObjects() throws Exception;

  /**
   * Perform the deserialization of the objects received through the socket connection.
   * @param bundle the message header that contains information about the tasks and data provider.
   * @return an array of objects deserialized from the socket stream.
   * @throws Exception if an error occurs while deserializing.
   */
  protected abstract Object[] deserializeObjects(TaskBundle bundle) throws Exception;

  /**
   * Write the execution results to the socket stream.
   * @param bundle the task wrapper to send along.
   * @param tasks the list of tasks with their result field updated.
   * @throws Exception if an error occurs while writing to the socket stream.
   * @since 4.2
   */
  protected abstract void sendResults(TaskBundle bundle, List<Task<?>> tasks) throws Exception;

  /**
   * Deserialize the objects read from the socket, and reload the appropriate classes if any class change is detected.<br>
   * A class change is triggered when an <code>InvalidClassException</code> is caught. Upon catching this exception,
   * the class loader is reinitialized and the class are reloaded.
   * @return an array of objects deserialized from the socket stream.
   * @throws Exception if the classes could not be reloaded or an error occurred during deserialization.
   */
  protected Object[] readObjects() throws Exception {
    Object[] result = null;
    boolean reload = false;
    try {
      result = deserializeObjects();
    } catch(final IncompatibleClassChangeError err) {
      reload = true;
      if (debugEnabled) log.debug(err.getMessage() + "; reloading classes", err);
    } catch(final InvalidClassException e) {
      reload = true;
      if (debugEnabled) log.debug(e.getMessage() + "; reloading classes", e);
    }
    if (reload) {
      if (debugEnabled) log.debug("reloading classes");
      handleReload();
      result = deserializeObjects();
    }
    return result;
  }

  /**
   * Perform some cleanup after sending the results.
   * @param bundle the task wrapper that was sent.
   * @throws Exception if an error occurs while writing to the socket stream.
   * @since 4.2
   */
  protected void postSendResults(final TaskBundle bundle) throws Exception {
    if (!node.isOffline() && !bundle.isNotification()) {
      if (debugEnabled) log.debug("resetting remoteClassLoadingDisabled to false");
      final JPPFContainer cont = node.getContainer(bundle.getUuidPath().getList());
      cont.getClassLoader().setRemoteClassLoadingDisabled(false);
    }
  }

  /**
   * Prepare the task bundle's data that will be sent back to the server.
   * @param bundle the bundle to process.
   */
  protected void initializeBundleData(final TaskBundle bundle) {
    bundle.setNodeExecutionTime(System.nanoTime());
  }

  /**
   * Compute the task bundle's data before it is sent back to the server.
   * @param bundle the bundle to process.
   * @param tasks the list of tasks after they have been executed.
   */
  protected void finalizeBundleData(final TaskBundle bundle, final List<Task<?>> tasks) {
    if (bundle.isNotification()) return;
    final long elapsed = System.nanoTime() - bundle.getNodeExecutionTime();
    bundle.setNodeExecutionTime(elapsed);
    final Set<Integer> resubmitSet = new HashSet<>();
    for (final Task<?> task: tasks) {
      if ((task instanceof AbstractTask) && ((AbstractTask<?>) task).isResubmit()) resubmitSet.add(task.getPosition());
    }
    if (!resubmitSet.isEmpty()) {
      if (debugEnabled) log.debug("positions of task resubmit requests: {}", resubmitSet);
      final int[] resubmitPos = new int[resubmitSet.size()];
      int count = 0;
      for (int n: resubmitSet) resubmitPos[count++] = n;
      bundle.setParameter(BundleParameter.RESUBMIT_TASK_POSITIONS, resubmitPos);
    }
    if (!node.isOffline() && node.getConfiguration().containsProperty(JPPFProperties.NODE_MAX_JOBS)) {
      final int maxJobs = node.getConfiguration().get(JPPFProperties.NODE_MAX_JOBS);
      if (debugEnabled) log.debug("sending node max jobs = {}", maxJobs);
      bundle.setParameter(BundleParameter.NODE_MAX_JOBS, maxJobs);
    }
  }
}
