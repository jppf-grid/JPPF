/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package test.org.jppf.test.setup.common;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.concurrent.Callable;

import org.jppf.JPPFError;
import org.jppf.client.*;
import org.jppf.management.*;
import org.jppf.management.forwarding.JPPFNodeForwardingMBean;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.*;

import test.org.jppf.test.setup.BaseTest;

/**
 * Helper methods for setting up and cleaning the environment before and after testing.
 * @author Laurent Cohen
 */
public class BaseTestHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger("TEST");
  /**
   * Message used for successful task execution.
   */
  public static final String EXECUTION_SUCCESSFUL_MESSAGE = "execution successful";
  /**
   * Prefix and suffix of messages to send to the serevr log.
   */
  public static final String STARS = "*****";
  /**
   * Parameter types for the {@code log()} method of the drivers and nodes debug MBean.
   */
  private final static String[] LOG_METHOD_SIGNATURE = { String[].class.getName() };

  /**
   * Find a constructor with the specfied number of parameters for the specified class.
   * @param taskClass the class of the tasks to add to the job.
   * @param nbParams the number of parameters for the tasks constructor.
   * @return a <code>constructor</code> instance.
   * @throws Exception if any error occurs if a construcotr could not be found.
   */
  public static Constructor<?> findConstructor(final Class<?> taskClass, final int nbParams) throws Exception {
    Constructor<?>[] constructors = taskClass.getConstructors();
    Constructor<?> constructor = null;
    for (Constructor<?> c: constructors) {
      if (c.getParameterTypes().length == nbParams) {
        constructor = c;
        break;
      }
    }
    if (constructor == null) throw new IllegalArgumentException("couldn't find a constructor for class " + taskClass.getName() + " with " + nbParams + " arguments");
    return constructor;
  }

  /**
   * Create a task with the specified parameters.
   * The type of the task is specified via its class, and the constructor to
   * use is specified based on the number of parameters.
   * @param id the task id.
   * @param taskClass the class of the tasks to add to the job.
   * @param params the parameters for the tasks constructor.
   * @return an <code>Object</code> representing a task.
   * @throws Exception if any error occurs.
   */
  public static Object createTask(final String id, final Class<?> taskClass, final Object...params) throws Exception {
    int nbArgs = (params == null) ? 0 : params.length;
    Constructor<?> constructor = findConstructor(taskClass, nbArgs);
    Object o = constructor.newInstance(params);
    if (o instanceof Task) ((Task<?>) o).setId(id);
    return o;
  }

  /**
   * Create a job with the specified parameters.
   * The type of the tasks is specified via their class, and the constructor to
   * use is specified based on the number of parameters.
   * @param name the job's name.
   * @param blocking specifies whether the job is blocking.
   * @param broadcast specifies whether the job is a broadcast job.
   * @param nbTasks the number of tasks to add to the job.
   * @param taskClass the class of the tasks to add to the job.
   * @param params the parameters for the tasks constructor.
   * @return a <code>JPPFJob</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFJob createJob(final String name, final boolean blocking, final boolean broadcast, final int nbTasks, final Class<?> taskClass, final Object...params) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    int nbArgs = (params == null) ? 0 : params.length;
    Constructor<?> constructor = findConstructor(taskClass, nbArgs);
    // 0 padding of task number
    int nbDigits = Integer.toString(nbTasks).length();
    String format = "%s-task %0" + nbDigits + "d";
    for (int i=1; i<=nbTasks; i++) {
      Object o = constructor.newInstance(params);
      job.add(o).setId(String.format(format, job.getName(), i));
    }
    job.setBlocking(blocking);
    job.getSLA().setBroadcastJob(broadcast);
    return job;
  }

  /**
   * Create a job with the specified parameters.
   * The type of the tasks is specified via their class, and the constructor to
   * use is specified based on the number of parameters.
   * @param name the job's name.
   * @param blocking specifies whether the job is blocking.
   * @param broadcast specifies whether the job is a broadcast job.
   * @param tasks the tasks to put in the job.
   * @return a <code>JPPFJob</code> instance.
   * @throws Exception if any error occurs.
   */
  public static JPPFJob createJob2(final String name, final boolean blocking, final boolean broadcast, final Object...tasks) throws Exception {
    JPPFJob job = new JPPFJob();
    job.setName(name);
    for (int i=1; i<=tasks.length; i++) job.add(tasks[i-1]).setId(job.getName() + " - task " + i);
    job.setBlocking(blocking);
    job.getSLA().setBroadcastJob(broadcast);
    return job;
  }

  /**
   * Wait untul the specified test succeeds, or the specified timeout expires, whichever happens first.
   * @param test the test to run.
   * @param timeout the timeout in milliseconds.
   * @throws Exception if any error occurs.
   */
  public static void waitForTest(final Callable<? super Object> test, final long timeout) throws Exception {
    Throwable throwable = null;
    long start = System.nanoTime();
    while (true) {
      try {
        test.call();
      } catch (Exception|Error e) {
        throwable = e;
      }
      if (throwable == null) return;
      long elapsed = DateTimeUtils.elapsedFrom(start);
      if (elapsed >= timeout) {
        if (throwable instanceof Exception) throw (Exception) throwable;
        else if (throwable instanceof Error) throw (Error) throwable;
        else throw new JPPFError(throwable);
      } else {
        throwable = null;
        Thread.sleep(10L);
      }
    }
  }

  /**
   * Print a formatted to the server log via the server debug mbean on all connected servers.
   * @param client JPPF client holding the server connections.
   * @param format the parameterized format.
   * @param params the parameters of the message.
   */
  public static void printToServers(final JPPFClient client, final String format, final Object...params) {
    printToServersAndNodes(client, true, false, format, params);
  }

  /**
   * Print a formatted message to the server log via the server debug mbean on all connected servers.
   * @param client JPPF client holding the server connections.
   * @param toServers whether to log to the discovered servers.
   * @param toNodes whether to log to the nodes attached to the discovered servers.
   * @param format the parameterized format.
   * @param params the parameters of the message.
   */
  public static void printToServersAndNodes(final JPPFClient client, final boolean toServers, final boolean toNodes, final String format, final Object...params) {
    printToAll(client, false, toServers, toNodes, true, format, params);
  }

  /**
   * Print a formatted message to the server log via the server debug mbean on all connected servers.
   * @param client JPPF client holding the server connections.
   * @param decorate whether to decorate the message in a very visible fashion.
   * @param format the parameterized format.
   * @param params the parameters of the message.
   */
  public static void printToAll(final JPPFClient client, final boolean decorate, final String format, final Object...params) {
    printToAll(client, true, true, true, decorate, format, params);
  }

  /**
   * Print a formatted message to the server log via the server debug mbean on all connected servers.
   * @param client JPPF client holding the server connections.
   * @param toClient whether to log to the client log.
   * @param toServers whether to log to the discovered servers.
   * @param toNodes whether to log to the nodes attached to the discovered servers.
   * @param decorate whether to decorate the message in a very visible fashion.
   * @param format the parameterized format.
   * @param params the parameters of the message.
   */
  public static void printToAll(final JPPFClient client, final boolean toClient, final boolean toServers, final boolean toNodes,
    final boolean decorate, final String format, final Object...params) {
    if (!toServers && !toNodes) return;
    List<JPPFConnectionPool> pools = client.findConnectionPools(JPPFClientConnectionStatus.workingStatuses());
    if ((pools == null) || pools.isEmpty()) return;
    String fmt = String.format("%s %s %s", STARS, format, STARS);
    String msg = String.format(fmt, params);
    StringBuilder sb = new StringBuilder(msg.length()).append(STARS).append(' ');
    for (int i=0; i<msg.length() - 2 * (STARS.length() + 1); i++) sb.append('-');
    String[] messages = { msg };
    if (decorate) {
      String s = sb.append(' ').append(STARS).toString();
      messages = new String[] { s, msg, s };
    }
    if (toClient) {
      for (String s: messages) log.info(s);
    }
    for (JPPFConnectionPool pool: pools) {
      List<JMXDriverConnectionWrapper> jmxConnections = pool.awaitJMXConnections(Operator.AT_LEAST, 1, 1000L, true);
      if (!jmxConnections.isEmpty()) {
        JMXDriverConnectionWrapper jmx = jmxConnections.get(0);
        if (toServers) {
          try {
            jmx.invoke("org.jppf:name=debug,type=driver", "log", new Object[] { messages }, LOG_METHOD_SIGNATURE);
          } catch (Exception e) {
            System.err.printf("[%s][%s] error invoking remote logging on %s:%n%s%n",
              BaseTest.getFormattedTimestamp(), ReflectionUtils.getCurrentClassAndMethod(), jmx, ExceptionUtils.getStackTrace(e));
          }
        }
        if (toNodes) {
          try {
            final JPPFNodeForwardingMBean forwarder = jmx.getNodeForwarder();
            if (forwarder != null) forwarder.forwardInvoke(NodeSelector.ALL_NODES, "org.jppf:name=debug,type=node", "log", new Object[] { messages }, LOG_METHOD_SIGNATURE);
          } catch (Exception e) {
            System.err.printf("[%s][%s] error invoking remote logging on the nodes of %s:%n%s%n",
              BaseTest.getFormattedTimestamp(), ReflectionUtils.getCurrentClassAndMethod(), jmx, ExceptionUtils.getStackTrace(e));
          }
        }
      }
    }
  }
}
