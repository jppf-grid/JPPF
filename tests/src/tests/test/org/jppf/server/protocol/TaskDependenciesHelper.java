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

package test.org.jppf.server.protocol;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.jppf.client.event.*;
import org.jppf.node.protocol.graph.*;
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
  static JobTaskGraph copyGraph(final JobTaskGraph graph, final JPPFSerialization ser) throws Exception {
    BaseTest.print(false, false, "1: graph = %s", graph);
    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      ser.serialize(graph, os);
      final byte[] bytes = os.toByteArray();
      try (final ByteArrayInputStream is = new ByteArrayInputStream(bytes)) {
        final JobTaskGraph graph2 = (JobTaskGraph) ser.deserialize(is);
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
  static JobTaskGraph createDiamondGraph() throws Exception {
    final MyTask[] tasks = createDiamondTasks();
    final JobTaskGraph graph = JobGraphHelper.graphOf(Arrays.asList(tasks));
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
  static MyTask[] createDiamondTasks() throws Exception {
    final MyTask[] tasks = new MyTask[4];
    for (int i=0; i<tasks.length; i++) tasks[i] = new MyTask("T" + i, i);
    tasks[0].dependsOn(tasks[1].dependsOn(tasks[3]), tasks[2].dependsOn(tasks[3]));
    return tasks;
  }

  /**
   * @return  agraph with diamonfd dependencies.
   * @throws Exception if any error occurs.
   */
  static JobTaskGraph createGraph() throws Exception {
    final MyTask[] tasks = new MyTask[6];
    for (int i=0; i<tasks.length; i++) tasks[i] = new MyTask("" + (char) ('A' + i), i);
    tasks[5].dependsOn(tasks[0], tasks[1], tasks[4]);
    tasks[4].dependsOn(tasks[2], tasks[3]);
    tasks[3].dependsOn(tasks[1], tasks[2]);
    tasks[2].dependsOn(tasks[0]);
    tasks[1].dependsOn(tasks[0]);
    
    final JobTaskGraph graph = JobGraphHelper.graphOf(Arrays.asList(tasks));
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
  static void checkExecution(final JobTaskGraph graph, final int taskPosition, final int expectedDoneCount, final boolean expectGraphDone, final int...expectedPositions) {
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
  static void testDependencyCycle(final ExceptionThrowingRunnable action) throws Exception {
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
     */
    public void setDuration(final long duration) {
      this.duration = duration;
    }
  }

  /** */
  @FunctionalInterface
  public interface NodeVisitor extends TaskNodeVisitor {
    @Override
    default TaskNodeVisitResult visitTaskNode(JobTaskNode node) {
      BaseTest.print(false, false, "visiting %s", node);
      doVisit(node);
      return TaskNodeVisitResult.CONTINUE;
    }

    /**
     * Visit the specified node.
     * @param node the node to visit.
     */
    void doVisit(JobTaskNode node);
  }

  /** */
  public static class DispatchListener extends JobListenerAdapter {
    /** */
    final List<Integer> dispatches = new ArrayList<>();

    @Override
    public void jobDispatched(final JobEvent event) {
      BaseTest.print(false, false, "dispatching tasks %s", event.getJobTasks());
      synchronized (dispatches) {
        dispatches.add(event.getJobTasks().size());
      }
    }
  }
}
