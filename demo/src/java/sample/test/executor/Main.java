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

package sample.test.executor;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.JPPFExecutorService;
import org.jppf.node.protocol.*;
import org.jppf.scheduling.JPPFSchedule;
import org.slf4j.*;

/**
 * Test of the executor service.
 */
public class Main {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Main.class);

  /**
   * Entry point.
   * @param args not used.
   */
  public static void main(final String[] args) {
    log.info("Starting test");
    final JPPFClient client = new JPPFClient();
    final JPPFExecutorService executor = new JPPFExecutorService(client);
    try {
      final DataProvider dp = new MemoryMapDataProvider();
      dp.setParameter("testKey", "testValue");
      executor.getConfiguration().getJobConfiguration().setDataProvider(dp);
      executor.setBatchSize(5);
      executor.setBatchTimeout(100L);
      final List<Future<Integer>> futures = new ArrayList<>(20);
      final int nbTasks = 20;
      log.info("Adding tasks");
      for (int i = 0; i < nbTasks; i++) {
        futures.add(executor.submit(new SimpleCountTask(i)));
        //Thread.sleep(1);
      }
      log.info("Waiting for pending tasks to complete");
      /* executor.shutdown();
       * while (!executor.isTerminated())
       * {
       * Thread.sleep(1000);
       * } */
      log.info("Pending tasks completed");
      for (int i = 0; i < nbTasks; i++) {
        log.info("Checking task {}", i);
        if (futures.get(i).get() != i) {
          throw new Exception("Invalid future response");
        }
      }
      log.info("All completed tasks checked");

      final MyTask myTask = new MyTask();
      myTask.setTimeoutSchedule(new JPPFSchedule(5000L));
      final Future<String> future = executor.submit((Callable<String>) myTask);
      System.out.println("result: " + future.get());
    } catch (final Exception e) {
      log.error("Error", e);
    } finally {
      executor.shutdownNow();
      client.close();
    }
  }

  /**
   * Simple task.
   */
  private static class SimpleCountTask implements Callable<Integer>, Serializable {
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(SimpleCountTask.class);
    /**
     * Explicit serialVersionUID.
     */
    private static final long serialVersionUID = 3044260680117586115L;
    /**
     * This task's number.
     */
    private int number;

    /**
     * Initialize this task with its number.
     * @param number the task number.
     */
    public SimpleCountTask(final int number) {
      this.number = number;
    }

    /**
     * {@inheritDoc}
     */
    // @Override
    @Override
    public Integer call() throws Exception {
      logger.info("From logger {}", number);
      logger.info("From stdout " + number);
      return number;
    }
  }
}
