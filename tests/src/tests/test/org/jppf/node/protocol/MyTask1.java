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

package test.org.jppf.node.protocol;

import static org.jppf.utils.configuration.JPPFProperties.*;

import java.util.List;

import org.jppf.*;
import org.jppf.client.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.TypedProperties;

import test.org.jppf.test.setup.common.AwaitTaskNotificationListener;

/**
 * A task that submits a job while executing in a node.
 */
public class MyTask1 extends AbstractTask<String> {
  /**
   * The JPPF client instance to use.
   */
  private static JPPFClient clientFromNode;
  /**
   * Lock to synchronize retrieval and creation of the client.
   */
  private static final Object staticLock = new Object();
  /**
   * How long to sleep.
   */
  private final long duration;
  /**
   * Whether and when to cancel the submitted job.
   */
  private final long cancelAfter;
  /**
   * The job submittd by this task.
   */
  private transient JPPFJob job;
  /**
   * Whether this task executed to completion.
   */
  private boolean completed;
  /**
   * Whether to submit the job and leave immediately.
   */
  private final boolean submitAndLeave;

  /**
   * @param duration how long to sleep.
   * @param cancelAfter whether and when to cancel the submitted job.
   */
  public MyTask1(final long duration, final long cancelAfter) {
    this(duration, cancelAfter, false);
  }

  /**
   * @param duration how long to sleep.
   * @param cancelAfter whether and when to cancel the submitted job.
   * @param submitAndLeave whether to submit the job and leave immediately.
   */
  public MyTask1(final long duration, final long cancelAfter, final boolean submitAndLeave) {
    this.duration = duration;
    this.cancelAfter = cancelAfter;
    this.submitAndLeave = submitAndLeave;
  }

  @Override
  public void run() {
    System.out.printf("MyTask1: duration = %,d,  cancelAfter = %,d, submitAndLeave = %b\n", duration, cancelAfter, submitAndLeave);
    try {
      AwaitTaskNotificationListener listener = null;
      job = new JPPFJob().setName(getJob().getName() + "-2");
      job.add(new MyTask2(duration, submitAndLeave));
      final JPPFClient client = getClient();
      if (submitAndLeave) {
        job.getSLA().setCancelUponClientDisconnect(false);
        listener = new AwaitTaskNotificationListener(client, MyTask2.START_NOTIF);
      }
      System.out.println("MyTask1: submitting job " + job.getName());
      client.submitAsync(job);
      if (!submitAndLeave) {
        if (cancelAfter > 0L) {
          Thread.sleep(cancelAfter);
          System.out.println("MyTask1: cancelling job " + job.getName());
          job.cancel();
        }
        final List<Task<?>> results = job.awaitResults(5000L);
        if (results == null) throw new JPPFException("did not receive results before timeout");
        System.out.println("MyTask1: got job results");
        final MyTask2 task = (MyTask2) results.get(0);
        setResult(task.getResult());
        setThrowable(task.getThrowable());
      } else {
        System.out.println("MyTask1: waiting for start notification");
        final boolean received = listener.await(5000L);
        System.out.printf("MyTask1: leaving after submission, received notif = %b\n", received);
      }
      completed = true;
      System.out.println("MyTask1 done");
    } catch (final InterruptedException e) {
      setThrowable(e);
    } catch (final Exception e) {
      setThrowable(e);
    }
  }

  /**
   * Get or create the JPPF client.
   * @return a {@link JPPFClient} instance.
   */
  JPPFClient getClient() {
    synchronized(staticLock) {
      if (clientFromNode == null) {
        System.out.println("getting client from node " + getNode().getUuid());
        final TypedProperties nodeConfig = getNode().getConfiguration();
        final String host = nodeConfig.getString("jppf.server.host");
        final int port = nodeConfig.getInt("jppf.server.port");
        final String driver = "driver1";
        final TypedProperties clientConfig = new TypedProperties()
          .set(DISCOVERY_ENABLED, false).set(DRIVERS, new String[] { driver })
          .set(PARAM_SERVER_HOST, host, driver).set(PARAM_SERVER_PORT, port, driver);
        clientFromNode = new JPPFClient(clientConfig);
        if (clientFromNode.awaitConnectionPool(5000L, JPPFClientConnectionStatus.workingStatuses()) == null)
          throw new JPPFRuntimeException("could not obtain a client connection to the driver from the node");
      }
      return clientFromNode;
    }
  }

  @Override
  public void onCancel() {
    System.out.println("MyTask1.onCancel(): task was cancelled on node " + getNode().getUuid());
    if (job != null) job.cancel();
  }

  /**
   * @return whether this task executed to completion.
   */
  public boolean isCompleted() {
    return completed;
  }
}