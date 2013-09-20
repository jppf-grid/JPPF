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
package org.jppf.test.scenario.jppf_171;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.*;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.test.scenario.AbstractScenarioRunner;
import org.jppf.test.setup.Setup;

/**
 * This is a template JPPF application runner.
 * It is fully commented and is designed to be used as a starting point
 * to write an application using JPPF.
 * @author Laurent Cohen
 */
public class DeadlockRunner extends AbstractScenarioRunner {
  /**
   * 
   */
  private static AtomicInteger jobSeq = new AtomicInteger(0);

  @Override
  public void run()
  {
    JPPFClient jppfClient = null;
    Setup setup = getSetup();
    try
    {
      jppfClient = setup.getClient();
      while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);
      int nbNodes = setup.getNodes().length;
      long time = System.nanoTime();
      jppfClient.submit(createJob(32, true, 1)); // warmup job
      JPPFJob job = createJob(8 * 1 * 20, false, 50);
      JPPFResultCollector collector = (JPPFResultCollector) job.getResultListener();
      System.out.println("submitting job with  " + job.getTasks().size() + " tasks");
      jppfClient.submit(job);
      int closeCount = 0;
      List<JPPFTask> results = null;
      while (results == null)
      {
        results = collector.waitForResults(50 + 5L);
        System.out.println("closing the connection " + ++closeCount);
        if (results != null) closeConnection(jppfClient);
        while (!jppfClient.hasAvailableConnection()) Thread.sleep(1L);
      }
      time = System.nanoTime() - time;
      HealthSnapshot snp = setup.getDriverDiagnosticsMBean().healthSnapshot();
      System.out.println("diagnostics: " + snp.toFormattedString());
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Create a job.
   * @param nbTasks number of tasks in the job.
   * @param blocking true if job is blocking, false otherwise.
   * @param minSleepTime the minimum sleep time for each task.
   * @return a JPPFJob instance.
   * @throws Exception if any error occurs.
   */
  private JPPFJob createJob(final int nbTasks, final boolean blocking, final long minSleepTime) throws Exception
  {
    JPPFJob job = new JPPFJob();
    job.setName(DeadlockRunner.class.getSimpleName() + '-' + jobSeq.incrementAndGet());
    job.setBlocking(blocking);
    for (int i=1; i<=nbTasks; i++) job.addTask(new DeadlockTask(i, minSleepTime + (i%10)));
    JPPFResultCollector collector = new JPPFResultCollector(job);
    job.setResultListener(collector);
    return job;
  }

  /**
   * 
   * @param jppfClient .
   * @throws Exception if any error occurs
   */
  private void closeConnection(final JPPFClient jppfClient) throws Exception {
    final JPPFClientConnectionImpl c = (JPPFClientConnectionImpl) jppfClient.getClientConnection();
    List<Thread> threads = new ArrayList<Thread>();
    /*
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
    */
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
    for (Thread t: threads) t.start();
    for (Thread t: threads) t.join();
  }
}
