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
package test.deserialization.error;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class DeserializationErrorRunner {
  /**
   * The JPPF client, handles all communications with the server.
   * It is recommended to only use one JPPF client per JVM, so it
   * should generally be created and used as a singleton.
   */
  private static JPPFClient jppfClient =  null;
  /**
   * 
   */
  private static AtomicInteger jobSeq = new AtomicInteger(0);

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments,
   * however nothing prevents us from using them if need be.
   * @throws Exception if any error occurs
   */
  public static void main(final String[] args) throws Exception {
    try {
      jppfClient = new JPPFClient();
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);
      long time = System.nanoTime();
      // warmup job
      jppfClient.submit(createJob(32, true));
      JPPFJob job = createJob(8 * 4 * 3, false);
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      jppfClient.submit(job);
      Thread.sleep(DeserializationErrorTask.EXEC_SLEEP_TIME + 5L);
      System.out.println("closing the connection");
      closeConnection();
      //jppfClient.close();
      List<JPPFTask> results = collector.waitForResults();
      JPPFTask task = results.get(0);
      Exception e = task.getException();
      System.out.println("result task = " + ReflectionUtils.dumpObject(task));
      System.out.println("exception = " + (e == null ? "null" : ExceptionUtils.getStackTrace(e)));
      System.out.println("result = " + task.getResult());
      time = System.nanoTime() - time;
      System.out.println("execution time = " + StringUtils.toStringDuration(time/1000000L));
    } finally {
      if ((jppfClient != null) && !jppfClient.isClosed()) jppfClient.close();
    }
  }

  /**
   * Create a job.
   * @param nbTasks number of tasks in the job.
   * @param blocking true if job is blocking, false otherwise.
   * @return a JPPFJob instance.
   * @throws Exception if any error occurs.
   */
  private static JPPFJob createJob(final int nbTasks, final boolean blocking) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(DeserializationErrorRunner.class.getSimpleName() + '-' + jobSeq.incrementAndGet());
    job.setBlocking(blocking);
    for (int i=1; i<=nbTasks; i++) job.addTask(new DeserializationErrorTask(i));
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    return job;
  }

  /**
   * Cancel the specified job.
   * @param uuid uuid of the job to cancel.
   * @throws Exception if any error occurs.
   */
  private static void cancelJobWithJMX(final String uuid) throws Exception {
    AbstractJPPFClientConnection c = (AbstractJPPFClientConnection) jppfClient.getClientConnection();
    c.getJmxConnection().cancelJob(uuid);
  }

  /**
   * 
   * @throws Exception if any error occurs
   */
  private static void closeConnection() throws Exception {
    final JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
    List<Thread> threads = new ArrayList<Thread>();
    threads.add(new Thread() {
      @Override
      public void run() {
        try {
          ClassServerDelegateImpl delegate = (ClassServerDelegateImpl) c.getDelegate();
          delegate.getSocketClient().getSocket().close();
        } catch (Exception e) {
        }
      }
    });
    /*
    threads.add(new Thread() {
      @Override
      public void run() {
        try {
          TaskServerConnectionHandler taskHandler = c.getTaskServerConnection();
          taskHandler.getSocketClient().getSocket().close();
        } catch (Exception e) {
        }
      }
    });
    */
    for (Thread t: threads) t.start();
    for (Thread t: threads) t.join();
  }
}
