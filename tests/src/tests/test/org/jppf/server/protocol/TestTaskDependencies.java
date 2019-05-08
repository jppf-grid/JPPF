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
import static test.org.jppf.test.setup.common.TaskDependenciesHelper.*;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.node.protocol.graph.*;
import org.jppf.serialization.*;
import org.jppf.serialization.kryo.KryoSerialization;
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
    graph.startVisit((NodeVisitor) node -> nodes.add(node));
    print(false, false, "all nodes: %s", nodes);
    assertEquals(4, nodes.size());
    assertEquals(0, nodes.get(0).getPosition());

    nodes.clear();
    print(false, false, "getting nodes without dependencies");
    graph.startVisit((NodeVisitor) node -> {
      if (node.getDependencies().isEmpty()) nodes.add(node);
    });
    print(false, false, "nodes without dependencies: %s", nodes);
    assertEquals(1, nodes.size());
    assertEquals(3, nodes.get(0).getPosition());
  }

  /**
   * Test adding tasks with dependenices to a real {@link JPPFJob} instance.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testGraphInJob() throws Exception {
    final MyTask t0 = new MyTask("T0", 0), t1 = new MyTask("T1", 1), t2 = new MyTask("T2", 2), t3 = new MyTask("T3", 3);
    t0.dependsOn(t1.dependsOn(t3), t2.dependsOn(t3));
    final JPPFJob job = new JPPFJob();
    job.addWithDpendencies(t0);
    assertTrue(job.hasTaskGraph());
    assertEquals(4, job.unexecutedTaskCount());
    final List<Task<?>> tasks = job.getJobTasks();
    assertEquals(4, tasks.size());
    for (int i=0; i<tasks.size(); i++) assertEquals(i, tasks.get(i).getPosition());
  }

  /**
   * Test the simulated execution of a task graph.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testGraphExecution() throws Exception {
    final JobTaskGraph graph = createDiamondGraph();
    print(false, false, "graph = %s", graph);
    checkExecution(graph, -1, 0, false, 3);
    checkExecution(graph,  3, 1, false, 1, 2);
    checkExecution(graph,  2, 2, false, 1);
    checkExecution(graph,  1, 3, false, 0);
    checkExecution(graph,  0, 4, true);
  }

  /**
   * Test the simulated execution of a task graph.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testGraphSerialization() throws Exception {
    final JPPFSerialization[] serializations = { new DefaultJavaSerialization(), new DefaultJPPFSerialization(), new KryoSerialization()/*, new XstreamSerialization()*/ };
    for (final JPPFSerialization ser: serializations) {
      print(false, false, ">> testing with %s <<", ser.getClass().getSimpleName());
      JobTaskGraph graph = createDiamondGraph();
      checkExecution(graph = copyGraph(graph, ser), -1, 0, false, 3);
      checkExecution(graph = copyGraph(graph, ser),  3, 1, false, 1, 2);
      checkExecution(graph = copyGraph(graph, ser),  2, 2, false, 1);
      checkExecution(graph = copyGraph(graph, ser),  1, 3, false, 0);
      checkExecution(graph = copyGraph(graph, ser),  0, 4, true);
    }
  }

  /**
   * Test the computation of a <a href="https://en.wikipedia.org/wiki/Topological_sorting">topological order</a> among the tasks in a dependency graph.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testTopologicalSort() throws Exception {
    final JobTaskGraph graph = createGraph();
    final List<Integer> to = graph.topologicalSortDFS();
    print(false, false, "b: graph = %s, topological order = %s", graph, to);
    for (final Integer n: to) {
      graph.nodeDone(n);
      print(false, false, "%d: graph = %s, topological order = %s", n, graph, graph.topologicalSortDFS());
    }
  }

  /**
   * Test what happens with a very deep graph (e.g a linked list with a large number of nodes).
   * It is expected that if the thread stack size (-Xss) is too low, then a {@link StackOverflowError} will be raised.
   * @throws Exception if any error occurs.
   */
  @Test
  public void testDeepGraph() throws Exception {
    final int nbTasks = 1000;
    final MyTask[] tasks = new MyTask[nbTasks];
    for (int i=0; i<nbTasks; i++) {
      tasks[i] = new MyTask("T" + i, i);
      if (i > 0) tasks[i].dependsOn(tasks[i - 1]);
    }
    final JobTaskGraph graph = JobGraphHelper.graphOf(Arrays.asList(tasks));
    print(false, false, "graph = %s", graph);
    final List<Integer> sorted = graph.topologicalSortDFS();
    print(false, false, "topoplogical sort -> %s", sorted);
  }
}
