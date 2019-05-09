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

package test.org.jppf.test.setup.common;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import javax.management.*;

import org.jppf.client.event.*;
import org.jppf.job.*;
import org.jppf.node.protocol.graph.*;
import org.jppf.node.protocol.graph.TaskGraph.Node;
import org.jppf.serialization.JPPFSerialization;
import org.jppf.utils.ExceptionThrowingRunnable;

import test.org.jppf.test.setup.BaseTest;

/**
 * Test the internal APIs for building and using a graph of tasks within a job.
 * @author Laurent Cohen
 */
public class TaskDependenciesHelper {
  /**
   * Copy the specified graph by serialization.
   * @param graph the graph to copy.
   * @param ser the serialization scheme implementation to use.
   * @return a copy of the input graph.
   * @throws Exception if any error occurs.
   */
  public static TaskGraph copyGraph(final TaskGraph graph, final JPPFSerialization ser) throws Exception {
    BaseTest.print(false, false, "1: graph = %s", graph);
    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      ser.serialize(graph, os);
      final byte[] bytes = os.toByteArray();
      try (final ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
        final TaskGraph graph2 = (TaskGraph) ser.deserialize(is);
        BaseTest.print(false, false, "2: graph = %s; seriallzed size = %,d bytes, string = %s", graph2, bytes.length, new String(bytes));
        return graph2;
      }
    }
  }

  /**
   * @return a graph with diamond dependencies.
   * The graph is as follows:
   * <pre>
   *      T1 
   *    /    \
   * T0       T3
   *    \    /
   *      T2
   * </pre>
   * @throws Exception if any error occurs.
   */
  public static TaskGraph createDiamondGraph() throws Exception {
    final MyTask[] tasks = createDiamondTasks();
    final TaskGraph graph = TaskGraphHelper.graphOf(Arrays.asList(tasks));
    BaseTest.print(false, false, "graph topological sort = %s", graph.topologicalSortDFS());
    return graph;
  }

  /**
   * @return an array of tasks with diamond dependencies.
   * The graph is as follows:
   * <pre>
   *      T1 
   *    /    \
   * T0       T3
   *    \    /
   *      T2
   * </pre>
   * @throws Exception if any error occurs.
   */
  public static MyTask[] createDiamondTasks() throws Exception {
    final MyTask[] tasks = new MyTask[4];
    for (int i=0; i<tasks.length; i++) tasks[i] = new MyTask("T" + i, i);
    tasks[0].dependsOn(tasks[1].dependsOn(tasks[3]), tasks[2].dependsOn(tasks[3]));
    return tasks;
  }

  /**
   * Create a dependency graph of tasks organized in layers, where tasks in each layer depend on all the tasks in the next layer.
   * @param nbLayers the number of layers.
   * @param tasksPerLayer the number of tasks in each layer.
   * @return an array of tasks with layered dependencies.
   * @throws Exception if any error occurs.
   */
  public static MyTask[] createLayeredTasks(final int nbLayers, final int tasksPerLayer) throws Exception {
    final MyTask[] tasks = new MyTask[nbLayers * tasksPerLayer];
    final int layerDigits = Integer.toString(nbLayers - 1).length(), taskDigits = Integer.toString(tasksPerLayer - 1).length();
    final String idFormat = String.format("L%%%ddT%%%dd", layerDigits, taskDigits);
    for (int i=0; i<nbLayers; i++) {
      for (int j=0; j<tasksPerLayer; j++) {
        final int pos = i * tasksPerLayer + j;
        final MyTask task = new MyTask(String.format(idFormat, i, j), pos);
        tasks[pos] = task;
        if (i > 0) {
          for (int k=0; k<tasksPerLayer; k++) tasks[(i - 1) * tasksPerLayer + k].dependsOn(task);
        }
      }
    }
    return tasks;
  }

  /**
   * @return a graph with diamonfd dependencies.
   * @throws Exception if any error occurs.
   */
  public static TaskGraph createGraph() throws Exception {
    final MyTask[] tasks = new MyTask[6];
    for (int i=0; i<tasks.length; i++) tasks[i] = new MyTask("" + (char) ('A' + i), i);
    tasks[5].dependsOn(tasks[0], tasks[1], tasks[4]);
    tasks[4].dependsOn(tasks[2], tasks[3]);
    tasks[3].dependsOn(tasks[1], tasks[2]);
    tasks[2].dependsOn(tasks[0]);
    tasks[1].dependsOn(tasks[0]);
    
    final TaskGraph graph = TaskGraphHelper.graphOf(Arrays.asList(tasks));
    //print(false, false, "graph topological sort = %s", graph.topologicalSortDFS());
    return graph;
  }

  /**
   * Simulate the execution of a task in a graph and check the resulting state of the graph.
   * @param graph the graph to check.
   * @param taskPosition the position of the task whose execution is simulated, if < 0 then no simulated execution, just check he graph.
   * @param expectedDoneCount the expected number of nodes that are done.
   * @param expectGraphDone whether to expect the graph execution to be complete.
   * @param expectedPositions the expected positions of the task that no longer have pending dependencies.
   */
  public static void checkExecution(final TaskGraph graph, final int taskPosition, final int expectedDoneCount, final boolean expectGraphDone, final int...expectedPositions) {
    final int expectedSize = ((expectedPositions == null) || (expectedPositions.length <= 0)) ? 0 : expectedPositions.length;
    if (taskPosition >= 0) graph.nodeDone(taskPosition);
    final Set<Integer> positions = graph.getAvailableNodes();
    assertEquals(expectedDoneCount, graph.getDoneCount());
    assertEquals(expectedSize, positions.size());
    if (expectedSize > 0) {
      for (final int n: expectedPositions) {
        if (!positions.contains(n)) fail(String.format("expected position '%d' not found, expected %s but got %st ", n, Arrays.toString(expectedPositions), positions));
      }
    }
    assertTrue(expectGraphDone ? graph.isDone() : !graph.isDone());
  }

  /**
   * Execute the specified action and verify that it raises a JPPFDependencyCycleException.
   * @param action the action to execute.
   * @throws Exception if any error occurs.
   */
  public static void testDependencyCycle(final ExceptionThrowingRunnable action) throws Exception {
    try {
      action.run();
      fail("action did not raise an exception");
    } catch(final JPPFDependencyCycleException e) {
      BaseTest.print(false, false, "got exception: %s", e);
    } catch(final Exception e) {
      fail("got unexpected exception: " + e);
    }
  }

  /**
   * A simple task implementation for testing.
   */
  public static class MyTask extends AbstractTaskNode<String> {
    /** */
    private long duration;
    /** */
    private String startNotif;
    /** */
    private String nodeUuid;

    /**
     * @param id the task id.
     */
    public MyTask(final String id) {
      this(id, 0, -1L);
    }

    /**
     * @param id the task id.
     * @param position the task position.
     */
    public MyTask(final String id, final int position) {
      this(id, position, -1L);
    }

    /**
     * @param id the task id.
     * @param position the task position.
     * @param duration how long the task wil sleep.
     */
    public MyTask(final String id, final int position, final long duration) {
      setId(id);
      setPosition(position);
      this.duration = duration;
    }

    @Override
    public String toString() {
      return getId();
    }

    @Override
    public void run() {
      try {
        //System.out.println("executing " + this);
        nodeUuid = getNode().getUuid();
        if (startNotif != null) fireNotification(startNotif, true);
        if (duration > 0L) Thread.sleep(duration);
        final String result = "executed " + getId();
        System.out.println(result);
        setResult(result);
      } catch (final Exception e) {
        setThrowable(e);
      }
    }

    @Override
    public void onCancel() {
      System.out.println("task " + this + " cancelled");
    }

    /**
     * @return how this task will sleep.
     */
    public long getDuration() {
      return duration;
    }

    /**
     * @param duration how this task will sleep.
     * @return this task, for methof call chaining.
     */
    public MyTask setDuration(final long duration) {
      this.duration = duration;
      return this;
    }

    /**
     * @return the start notification to send.
     */
    public String getStartNotif() {
      return startNotif;
    }

    /**
     * @param startNotif the start notification to send.
     * @return this task, for methof call chaining.
     */
    public MyTask setStartNotif(final String startNotif) {
      this.startNotif = startNotif;
      return this;
    }

    /**
     * @return the uuid of the node on which this task is executed.
     */
    public String getNodeUuid() {
      return nodeUuid;
    }
  }

  /** */
  @FunctionalInterface
  public interface NodeVisitor extends TaskNodeVisitor {
    @Override
    default TaskNodeVisitResult visitTaskNode(Node node) {
      BaseTest.print(false, false, "visiting %s", node);
      doVisit(node);
      return TaskNodeVisitResult.CONTINUE;
    }

    /**
     * Visit the specified node.
     * @param node the node to visit.
     */
    void doVisit(Node node);
  }

  /** */
  public static class DispatchListener extends JobListenerAdapter {
    /** */
    public final List<Integer> dispatches = new ArrayList<>();

    @Override
    public void jobDispatched(final JobEvent event) {
      final int size = event.getJobTasks().size();
      BaseTest.print(false, false, "dispatching %d tasks %s", size, event.getJobTasks());
      synchronized (dispatches) {
        dispatches.add(size);
      }
    }
  }

  /** */
  public static class ServerDispatchListener implements NotificationListener {
    /** */
    public final List<Integer> dispatches = new ArrayList<>();
    /**
     * Whether the job has ended.
     */
    boolean jobEnded;

    @Override
    public synchronized void handleNotification(final Notification notification, final Object handback) {
      final JobNotification jobNotif = (JobNotification) notification;
      final JobEventType eventType = jobNotif.getEventType();
      switch(eventType) {
        case JOB_DISPATCHED:
          final JobInformation info = jobNotif.getJobInformation();
          final int n = info.getTaskCount();
          dispatches.add(n);
          BaseTest.print(false, false, "server job %s dispatched %d tasks to node %s", info.getJobName(), n, jobNotif.getNodeInfo().getUuid());
          break;

        case JOB_ENDED:
          jobEnded = true;
          break;
      }
    }

    /**
     * @return whether the job has ended.
     */
    public synchronized boolean isJobEnded() {
      return jobEnded;
    }

    /**
     * @return the current number of dispatches.
     */
    public synchronized int getNbDispatches() {
      return dispatches.size();
    }
  }
}
