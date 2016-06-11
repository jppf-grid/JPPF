/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package test.loadbalancer;

import java.util.*;
import java.util.concurrent.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.concurrent.*;
import org.jppf.utils.*;

/**
 * @author Tomas
 *
 */
public class JPPF_LoadBalancingApp {
  /** number of tasks */
  private static final int JOB_SIZE = 100;
  /** */
  private static final int TOTAL_TASKS = 500;
  /** */
  private static final int SIZE_PER_EXECUTOR = 10;

  /**
   * @param args .
   */
  public static void main(final String[] args) {
    System.out.println("Starting...");
    boolean remoteExec = true;

    TypedProperties props = JPPFConfiguration.getProperties();
    props.setBoolean("jppf.discovery.enabled", false);

    props.setString("jppf.drivers", "driver1");
    //props.setString("driver1.jppf.server.host", "52.23.207.140");
    props.setString("driver1.jppf.server.host", "192.168.1.24");
    props.setInt("driver1.jppf.server.port", 11111);
    props.setInt("jppf.management.port", 11198);

    props.setBoolean("jppf.local.execution.enabled", true); //first channel
    props.setBoolean("jppf.remote.execution.enabled", remoteExec); //second channel

    /*
    props.setString("jppf.load.balancing.algorithm", "MyLoadBalancer");
    props.setString("jppf.load.balancing.profile", "MyLoadBalancer");
    // maximum number of tasks which will client distribute at once to each node
    props.setInt("jppf.load.balancing.profile.MyLoadBalancer.sizePerExecutor", SIZE_PER_EXECUTOR);
    */
    props.setString("jppf.load.balancing.algorithm", "proportional");
    props.setString("jppf.load.balancing.profile", "proportional");
    props.setInt("jppf.load.balancing.profile.proportional.initialSize", SIZE_PER_EXECUTOR);
    props.setInt("jppf.load.balancing.profile.proportional.performanceCacheSize", 500);

    try (JPPFClient jppfClient = new JPPFClient()) {
      if (remoteExec) jppfClient.awaitWorkingConnectionPool();
      // We must provide the load-balancer access to the JPPF client
      MyLoadBalancer.setClient(jppfClient);

      JPPFExecutorService executor = new JPPFExecutorService(jppfClient);
      executor.setBatchSize(JOB_SIZE);
      executor.setBatchTimeout(1000L);

      //compute tasks
      JPPF_LoadBalancingApp computer = new JPPF_LoadBalancingApp();
      int count = 1;
      //while (true) {
      long start = System.nanoTime();
      while (count <= TOTAL_TASKS) {
        computer.compute(executor, count);
        count++;
      }
      long elapsed = (System.nanoTime() - start) / 1_000_000L;
      System.out.printf("exec done in %s (%,d ms)%n", StringUtils.toStringDuration(elapsed), elapsed);
      executor.shutdownNow();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Finished.");
    
  }

  /**  */
  private ArrayList<Integer> taskList = new ArrayList<>();

  /**
   * Submit a task and perform the computation if there are enough tasks.
   * @param executor the JPPF executor.
   * @param count index of the task.
   * @throws Exception if any error occurs
   */
  private void compute(final ExecutorService executor, final int count) throws Exception {
    taskList.add(count);
    if (taskList.size() == JOB_SIZE) {
      _compute(executor);
    }
  }

  /**
   * Submit the tasks.
   * @param executor the JPPF executor.
   * @throws Exception if any error occurs
   */
  private void _compute(final ExecutorService executor) throws Exception {
    List<Future<?>> results = new ArrayList<>();
    for (Integer task : taskList) {
      results.add(executor.submit(new JPPF_LoadBalancingTask(task)));
    }

    for (Future<?> future : results) {
      String result = (String) future.get();
      System.out.println(result);
    }
    taskList.clear();
  }
}
