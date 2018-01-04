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

import static org.jppf.node.protocol.BundleParameter.NODE_EXCEPTION_PARAM;

import java.io.InvalidClassException;
import java.util.*;
import java.util.concurrent.Callable;

import org.jppf.io.*;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * This class performs the I/O operations requested by the JPPFNode, for reading the task bundles and sending the results back.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class AbstractNodeIO implements NodeIO {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractNodeIO.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The node who owns this TaskIO.
   */
  protected final JPPFNode node;
  /**
   * The task bundle currently being processed.
   */
  protected TaskBundle currentBundle = null;
  /**
   * Used to serialize/deserialize tasks and data providers.
   */
  protected ObjectSerializer serializer = null;

  /**
   * Initialize this TaskIO with the specified node.
   * @param node - the node who owns this TaskIO.
   */
  public AbstractNodeIO(final JPPFNode node) {
    this.node = node;
    HookFactory.registerConfigSingleHook(JPPFProperties.SERIALIZATION_EXCEPTION_HOOK, SerializationExceptionHook.class, new DefaultSerializationExceptionHook(), getClass().getClassLoader());
  }

  /**
   * Read a task from the socket connection, along with its header information.
   * @return a pair of <code>JPPFTaskBundle</code> and a <code>List</code> of <code>JPPFTask</code> instances.
   * @throws Exception if an error is raised while reading the task data.
   */
  @Override
  public Pair<TaskBundle, List<Task<?>>> readTask() throws Exception {
    final Object[] result = readObjects();
    currentBundle = (TaskBundle) result[0];
    final List<Task<?>> taskList = new ArrayList<>(result.length - 2);
    if (!currentBundle.isHandshake() && (currentBundle.getParameter(NODE_EXCEPTION_PARAM) == null)) {
      final DataProvider dataProvider = (DataProvider) result[1];
      for (int i=0; i<currentBundle.getTaskCount(); i++) {
        final Task<?> task = (Task<?>) result[2 + i];
        task.setDataProvider(dataProvider).setInNode(true).setNode(node);
        taskList.add(task);
      }
    }
    return new Pair<>(currentBundle, taskList);
  }

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
   */
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
   * Write the execution results to the socket stream.
   * @param bundle the task wrapper to send along.
   * @param tasks the list of tasks with their result field updated.
   * @throws Exception if an error occurs while writing to the socket stream.
   * @since 4.2
   */
  protected abstract void sendResults(TaskBundle bundle, List<Task<?>> tasks) throws Exception;

  /**
   * Perform some cleanup after sending the results.
   * @param bundle the task wrapper that was sent.
   * @throws Exception if an error occurs while writing to the socket stream.
   * @since 4.2
   */
  protected void postSendResults(final TaskBundle bundle) throws Exception {
    if (!node.isOffline()) {
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
  }

  /**
   * A pairing of a list of buffers and the total length of their usable data.
   * @exclude
   */
  protected static class BufferList extends Pair<List<JPPFBuffer>, Integer> {
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Initialize this pairing with the specified list of buffers and length.
     * @param first the list of buffers.
     * @param second the total data length.
     */
    public BufferList(final List<JPPFBuffer> first, final Integer second) {
      super(first, second);
    }
  }

  /**
   * The goal of this class is to serialize an object before sending it back to the server,
   * and catch an eventual exception.
   */
  protected class ObjectSerializationTask implements Callable<DataLocation> {
    /**
     * The data to send over the network connection.
     */
    private final Object object;
    /**
     * Used to serialize the object.
     */
    private final ObjectSerializer ser;
    /**
     * The context class loader to use.
     */
    private final ClassLoader contextCL;

    /**
     * Initialize this task with the specified data buffer.
     * @param object the object to serialize.
     * @param serializer used to serialize the object.
     * @param contextCL the context class loader to use.
     */
    public ObjectSerializationTask(final Object object, final ObjectSerializer serializer, final ClassLoader contextCL) {
      this.object = object;
      this.ser = serializer;
      this.contextCL = contextCL;
    }

    @Override
    public DataLocation call() {
      DataLocation dl = null;
      final int p = (object instanceof Task) ? ((Task<?>) object).getPosition() : -1;
      try {
        Thread.currentThread().setContextClassLoader(contextCL);
        if (traceEnabled) log.trace("before serialization of object at position " + p);
        dl = IOHelper.serializeData(object, ser);
        final int size = dl.getSize();
        if (traceEnabled) log.trace("serialized object at position " + p + ", size = " + size);
      } catch(final Throwable t) {
        log.error(t.getMessage(), t);
        try {
          final JPPFExceptionResult result = (JPPFExceptionResult) HookFactory.invokeSingleHook(SerializationExceptionHook.class, "buildExceptionResult", object, t);
          result.setPosition(p);
          dl = IOHelper.serializeData(result, ser);
        } catch(final Exception e2) {
          log.error(e2.getMessage(), e2);
        }
      } finally {
        Thread.currentThread().setContextClassLoader(contextCL);
      }
      return dl;
    }
  }
}
