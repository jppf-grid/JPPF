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

import java.util.*;

import org.jppf.node.protocol.graph.*;
import org.jppf.utils.ExceptionThrowingRunnable;
import org.junit.Test;

import test.org.jppf.test.setup.BaseTest;

/**
 * Test the internal APIs for building and using a graph of tasks within a job.
 * @author Laurent Cohen
 */
public class TestTaskDependencies extends BaseTest {
  /**
   * Test that cycles in the tasks dependencies are detected.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testCycle() throws Exception {
    testDependencyCycle(() -> {
      final MyTask t1 = new MyTask("T1");
      final MyTask t2 = new MyTask("T2");
      final MyTask t3 = new MyTask("T3");
      t2.dependsOn(t3);
      t3.dependsOn(t1);
      t1.dependsOn(t2, t3);
    });

    testDependencyCycle(() -> {
      final MyTask t1 = new MyTask("T1");
      t1.dependsOn(new MyTask("T2").dependsOn(t1));
    });
  }

  /**
   * Test that a task cannot depend on itself.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testDirectCycle() throws Exception {
    testDependencyCycle(() -> {
      final MyTask t1 = new MyTask("T1");
      t1.dependsOn(t1);
    });
  }
  
  /**
   * Test building and visiting a graph of tasks.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testGraph() throws Exception {
    final MyTask t0 = new MyTask("T0", 0), t1 = new MyTask("T1", 1), t2 = new MyTask("T2", 2), t3 = new MyTask("T3", 3);
    t0.dependsOn(t1.dependsOn(t3), t2.dependsOn(t3));
    final JobTaskGraph graph = JobGraphHelper.graphOf(Arrays.asList(t0, t1, t2, t3));

    final List<JobTaskNode> nodes = new ArrayList<>();
    print(false, false, "getting all nodes");
    graph.startVisit(node -> {
      print(false, false, "visiting %s", node);
      nodes.add(node);
      return TaskNodeVisitResult.CONTINUE;
    });
    print(false, false, "all nodes: %s", nodes);
    assertEquals(4, nodes.size());
    assertEquals(0, nodes.get(0).getPosition());

    nodes.clear();
    print(false, false, "getting nodes without dependencies");
    graph.startVisit(node -> {
      print(false, false, "visiting %s", node);
      if (node.getDependencies().isEmpty()) nodes.add(node);
      return TaskNodeVisitResult.CONTINUE;
    });
    print(false, false, "nodes without dependencies: %s", nodes);
    assertEquals(1, nodes.size());
    assertEquals(3, nodes.get(0).getPosition());
  }

  /**
   * Test the simulated execution of a task graph.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testGraphExecution() throws Exception {
    final MyTask t0 = new MyTask("T0", 0), t1 = new MyTask("T1", 1), t2 = new MyTask("T2", 2), t3 = new MyTask("T3", 3);
    t0.dependsOn(t1.dependsOn(t3), t2.dependsOn(t3));
    final JobTaskGraph graph = JobGraphHelper.graphOf(Arrays.asList(t0, t1, t2, t3));
    checkExecution(graph, -1, false, 3);
    checkExecution(graph,  3, false, 1, 2);
    checkExecution(graph,  2, false, 1);
    checkExecution(graph,  1, false, 0);
    checkExecution(graph,  0, true);
  }

  /**
   * Simulate the execution of a task in a grpah and chech thze resulting state of the graph.
   * @param graph the graph to check.
   * @param taskPosition the position of the task whose execution is simulated, if < 0 then no simulated execution, just check he graph.
   * @param expectGraphDone whether to expect the graph execution to be complete.
   * @param expectedPositions the expected positions of the task that no longer have pending dependencies.
   */
  private static void checkExecution(final JobTaskGraph graph, final int taskPosition, final boolean expectGraphDone, final int...expectedPositions) {
    final int expectedSize = ((expectedPositions == null) || (expectedPositions.length <= 0)) ? 0 : expectedPositions.length;
    if (taskPosition >= 0) graph.nodeDone(taskPosition);
    final Set<Integer> positions = graph.getAvailableNodes();
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
  private static void testDependencyCycle(final ExceptionThrowingRunnable action) throws Exception {
    try {
      action.run();
      fail("action did not raise an exception");
    } catch(final JPPFDependencyCycleException e) {
      print(false, false, "got exception: %s", e);
    } catch(final Exception e) {
      fail("got unexpected exception: " + e);
    }
  }

  /** */
  public static class MyTask extends AbstractTaskNode<String> {
    /**
     * @param id the task id.
     */
    public MyTask(final String id) {
      setId(id);
    }

    /**
     * @param id the task id.
     * @param position the task position.
     */
    public MyTask(final String id, final int position) {
      setId(id);
      setPosition(position);
    }

    @Override
    public String toString() {
      return getId();
    }
  }
}
