/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.client;

import static org.jppf.client.JPPFClientConnectionStatus.CREATED;

import java.io.NotSerializableException;
import java.nio.channels.AsynchronousCloseException;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

import org.jppf.JPPFException;
import org.jppf.comm.socket.*;
import org.jppf.io.IOHelper;
import org.jppf.node.protocol.*;
import org.jppf.serialization.*;
import org.jppf.server.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class provides an API to submit execution requests and administration
 * commands, and request server information data.<br>
 * It has its own unique identifier, used by the nodes, to determine whether
 * classes from the submitting application should be dynamically reloaded or not
 * depending on whether the uuid has changed or not.
 * @author Laurent Cohen
 * @exclude
 */
public abstract class BaseJPPFClientConnection implements JPPFClientConnection {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BaseJPPFClientConnection.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Used to prevent parallel deserialization.
   */
  private static Lock lock = new ReentrantLock();
  /**
   * Determines whether tasks deserialization should be sequential rather than parallel.
   */
  private static final boolean SEQUENTIAL_DESERIALIZATION = JPPFConfiguration.getProperties().getBoolean("jppf.sequential.deserialization", false);
  /**
   * 
   */
  protected static AtomicInteger connectionCount = new AtomicInteger(0);
  /**
   * Handler for the connection to the task server.
   */
  protected TaskServerConnectionHandler taskServerConnection = null;
  /**
   * Enables loading local classes onto remote nodes.
   */
  protected ClassServerDelegate delegate = null;
  /**
   * Unique identifier of the remote driver.
   */
  protected String driverUuid = null;
  /**
   * The name or IP address of the host the JPPF driver is running on.
   */
  protected String host = null;
  /**
   * The TCP port the JPPF driver listening to for submitted tasks.
   */
  protected int port = -1;
  /**
   * Configuration name for this local client.
   */
  protected String name = null;
  /**
   * The JPPF client that owns this connection.
   */
  protected AbstractGenericClient client = null;
  /**
   * Unique ID for this connection and its two channels.
   */
  protected String connectionUuid = null;
  /**
   * Status of the connection.
   */
  protected AtomicReference<JPPFClientConnectionStatus> status = new AtomicReference<>(CREATED);

  /**
   * Initialize this client connection.
   * @see org.jppf.client.JPPFClientConnection#init()
   */
  @Override
  public abstract void init();

  /**
   * Send tasks to the server for execution.
   * @param cl classloader used for serialization.
   * @param header the task bundle to send to the driver.
   * @param job the job to execute remotely.
   * @throws Exception if an error occurs while sending the request.
   */
  public void sendTasks(final ClassLoader cl, final JPPFTaskBundle header, final JPPFJob job) throws Exception {
    ObjectSerializer ser = makeHelper(cl, client.getSerializationHelperClassName()).getSerializer();
    int count = job.getJobTasks().size() - job.getResults().size();
    TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(client.getUuid());
    header.setUuidPath(uuidPath);
    header.setTaskCount(count);
    header.setName(job.getName());
    header.setUuid(job.getUuid());
    header.setSLA(job.getSLA());
    header.setMetadata(job.getMetadata());

    int[] positions = new int[count];
    Task<?>[] tasks = new Task<?>[count];
    int i = 0;
    for (Task task : job.getJobTasks()) {
      int pos = task.getPosition();
      if (!job.getResults().hasResult(pos)) {
        tasks[i] = task;
        positions[i] = pos;
        i++;
      }
    }
    header.setParameter(BundleParameter.TASK_POSITIONS, positions);
    if (debugEnabled) log.debug(this.toDebugString() + " sending job " + header + ", positions=" + StringUtils.buildString(positions));

    SocketWrapper socketClient = taskServerConnection.getSocketClient();
    boolean hasNotSerializableException = false;
    IOHelper.sendData(socketClient, header, ser);
    try {
      IOHelper.sendData(socketClient, job.getDataProvider(), ser);
    } catch(NotSerializableException e) {
      hasNotSerializableException = true;
      log.error("error serializing data provider for {} : {}\nthe job will be cancelled", job, ExceptionUtils.getStackTrace(e));
      IOHelper.sendData(socketClient, null, ser);
    }
    for (Task<?> task : tasks) {
      try {
        IOHelper.sendData(socketClient, task, ser);
      } catch(NotSerializableException e) {
        hasNotSerializableException = true;
        log.error("error serializing task {} for {} : {}\nthe job will be cancelled", new Object[] { task, job, ExceptionUtils.getStackTrace(e) });
        Task<?> t = new JPPFExceptionResult(e, task);
        t.setPosition(task.getPosition());
        IOHelper.sendData(socketClient, t, ser);
      }
    }
    socketClient.flush();
    if (hasNotSerializableException) client.cancelJob(job.getUuid());
  }

  /**
   * Send a handshake job to the server.
   * @return a JPPFTaskBundlesent by the server in response to the handshake job.
   * @throws Exception if an error occurs while sending the request.
   */
  public TaskBundle sendHandshakeJob() throws Exception {
    TaskBundle header = new JPPFTaskBundle();
    ObjectSerializer ser = new ObjectSerializerImpl();
    TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(client.getUuid());
    header.setUuidPath(uuidPath);
    if (debugEnabled) log.debug(this.toDebugString() + " sending handshake job, uuidPath=" + uuidPath);
    header.setUuid(new JPPFUuid().toString());
    header.setName("handshake job");
    header.setUuid("handshake job");
    header.setParameter("connection.uuid", connectionUuid);
    SocketWrapper socketClient = taskServerConnection.getSocketClient();
    IOHelper.sendData(socketClient, header, ser);
    IOHelper.sendData(socketClient, null, ser); // null data provider
    socketClient.flush();
    return receiveBundleAndResults(getClass().getClassLoader(), AbstractJPPFClient.SERIALIZATION_HELPER_IMPL).first();
  }

  /**
   * Send a close command job to the server.
   * @throws Exception if an error occurs while sending the request.
   */
  public void sendCloseConnectionCommand() throws Exception {
    TaskBundle header = new JPPFTaskBundle();
    ObjectSerializer ser = new ObjectSerializerImpl();
    TraversalList<String> uuidPath = new TraversalList<>();
    uuidPath.add(client.getUuid());
    header.setUuidPath(uuidPath);
    if (debugEnabled) log.debug(this.toDebugString() + " sending close command job, uuidPath=" + uuidPath);
    header.setName("close command job");
    header.setUuid("close command job");
    header.setParameter("connection.uuid", connectionUuid);
    header.setParameter(BundleParameter.CLOSE_COMMAND, true);
    SocketWrapper socketClient = taskServerConnection.getSocketClient();
    if (socketClient != null) {
      IOHelper.sendData(socketClient, header, ser);
      IOHelper.sendData(socketClient, null, ser); // null data provider
      socketClient.flush();
    }
  }

  /**
   * Receive results of tasks execution.
   * @param cl the class loader to use for deserializing the tasks.
   * @param helperClassName the fully qualified class name of the serialization helper to use.
   * @return a pair of objects representing the executed tasks results, and the index
   * of the first result within the initial task execution request.
   * @throws Exception if an error is raised while reading the results from the server.
   */
  @SuppressWarnings("unchecked")
  protected Pair<TaskBundle, List<Task<?>>> receiveBundleAndResults(final ClassLoader cl, final String helperClassName) throws Exception {
    List<Task<?>> taskList = new LinkedList<>();
    TaskBundle bundle = null;
    ClassLoader ctxCl = Thread.currentThread().getContextClassLoader();
    try {
      ClassLoader loader = cl == null ? getClass().getClassLoader() : cl;
      Thread.currentThread().setContextClassLoader(loader);
      SocketWrapper socketClient = taskServerConnection.getSocketClient();
      ObjectSerializer ser = makeHelper(loader, helperClassName).getSerializer();
      bundle = (TaskBundle) IOHelper.unwrappedData(socketClient, ser);
      int count = bundle.getTaskCount();
      int[] positions = bundle.getParameter(BundleParameter.TASK_POSITIONS);
      if (debugEnabled) {
        log.debug(this.toDebugString() + " : received bundle " + bundle + ", positions=" + StringUtils.buildString(positions));
      }
      if (SEQUENTIAL_DESERIALIZATION) lock.lock();
      try {
        for (int i = 0; i < count; i++) {
          Task<?> task = (Task<?>) IOHelper.unwrappedData(socketClient, ser);
          if ((positions != null) && (i < positions.length)) task.setPosition(positions[i]);
          taskList.add(task);
        }
      } finally {
        if (SEQUENTIAL_DESERIALIZATION) lock.unlock();
      }

      // if an exception prevented the node from executing the tasks
      Throwable t = bundle.getParameter(BundleParameter.NODE_EXCEPTION_PARAM);
      if (t != null) {
        if (debugEnabled) log.debug(this.toDebugString() + " : server returned exception parameter in the header for job '" + bundle.getName() + "' : " + t);
        Exception e = (t instanceof Exception) ? (Exception) t : new JPPFException(t);
        for (Task<?> task : taskList) task.setThrowable(e);
      }
      return new Pair(bundle, taskList);
    } catch (AsynchronousCloseException e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      throw e;
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.error(ExceptionUtils.getMessage(e));
      throw e;
    } catch (Error e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.error(ExceptionUtils.getMessage(e));
      throw e;
    } finally {
      Thread.currentThread().setContextClassLoader(ctxCl);
    }
  }

  /**
   * Receive results of tasks execution.
   * @param cl the context classloader to use to deserialize the results.
   * @return a pair of objects representing the executed tasks results, and the index
   * of the first result within the initial task execution request.
   * @throws Exception if an error is raised while reading the results from the server.
   */
  public List<Task<?>> receiveResults(final ClassLoader cl) throws Exception {
    return receiveBundleAndResults(cl, client.getSerializationHelperClassName()).second();
  }

  /**
   * Instantiate a <code>SerializationHelper</code> using the current context class loader.
   * @param helperClassName the fully qualified class name of the serialization helper to use.
   * @return a <code>SerializationHelper</code> instance.
   * @throws Exception if the serialization helper could not be instantiated.
   */
  protected SerializationHelper makeHelper(final String helperClassName) throws Exception {
    return makeHelper(null, helperClassName);
  }

  /**
   * Instantiate a <code>SerializationHelper</code> using the current context class loader.
   * @param classLoader the class loader to use to load the serialization helper class.
   * @param helperClassName the fully qualified class name of the serialization helper to use.
   * @return a <code>SerializationHelper</code> instance.
   * @throws Exception if the serialization helper could not be instantiated.
   */
  protected SerializationHelper makeHelper(final ClassLoader classLoader, final String helperClassName) throws Exception {
    ClassLoader[] clArray = { classLoader, Thread.currentThread().getContextClassLoader(), getClass().getClassLoader() };
    Class clazz = null;
    for (ClassLoader cl: clArray) {
      try {
        if (cl == null) continue;
        clazz = Class.forName(helperClassName, true, cl);
        break;
      } catch (Exception e) {
      }
      if (clazz == null) throw new IllegalStateException("could not load class " + helperClassName + " from any of these class loaders: " + Arrays.asList(clArray));
    }
    return (SerializationHelper) clazz.newInstance();
  }

  /**
   * Shutdown this client and retrieve all pending executions for resubmission.
   * @return a list of <code>JPPFJob</code> instances to resubmit.
   * @see org.jppf.client.JPPFClientConnection#close()
   */
  @Override
  public abstract List<JPPFJob> close();

  /**
   * Get the name assigned to this client connection.
   * @return the name as a string.
   * @see org.jppf.client.JPPFClientConnection#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Create a socket initializer.
   * @return an instance of a class implementing <code>SocketInitializer</code>.
   */
  protected abstract SocketInitializer createSocketInitializer();

  /**
   * Get the handler for the connection to the task server.
   * @return a <code>TaskServerConnectionHandler</code> instance.
   */
  public TaskServerConnectionHandler getTaskServerConnection() {
    return taskServerConnection;
  }

  /**
   * Get the class server delegate that loads local classes onto remote nodes
   * @return a {@link ClassServerDelegate} instance.
   */
  public ClassServerDelegate getDelegate() {
    return delegate;
  }

  /**
   * Get the JPPF client that owns this connection.
   * @return an <code>AbstractGenericClient</code> instance.
   */
  public AbstractGenericClient getClient() {
    return client;
  }

  /**
   * Get the unique identifier of the remote driver.
   * @return the uuid as a string.
   * @deprecated use {@link #getDriverUuid()} instead.
   */
  public String getUuid() {
    return driverUuid;
  }

  @Override
  public String getDriverUuid() {
    return driverUuid;
  }

  @Override
  public String getConnectionUuid() {
    return connectionUuid;
  }

  /**
   * The name or IP address of the host the JPPF driver is running on.
   * @return the host as a string.
   */
  @Override
  public String getHost() {
    return host;
  }

  /**
   * Get the TCP port the JPPF driver listening to for submitted tasks.
   * @return the port as an int value.
   */
  @Override
  public int getPort() {
    return port;
  }

  /**
   * Get a string representing this connection for debugging purposes.
   * @return a string representing this connection.
   */
  protected String toDebugString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("connectionUuid=").append(connectionUuid);
    sb.append(", status=").append(status);
    sb.append(']');
    return sb.toString();
  }
}
