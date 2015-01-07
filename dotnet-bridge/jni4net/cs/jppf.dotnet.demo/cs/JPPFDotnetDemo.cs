/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using net.sf.jni4net;
using org.jppf.client;
using org.jppf.client.concurrent;
using org.jppf.client.@event;
using org.jppf.client.monitoring.topology;
using org.jppf.dotnet;
using org.jppf.node.policy;
using org.jppf.node.protocol;
using org.jppf.dotnet;
using org.jppf.scheduling;
using java.util.concurrent;
using javax.management;
using org.jppf.job;
using org.jppf.management;
using org.jppf.management.generated;
using org.jppf.management.forwarding;
using org.jppf.node.provisioning;

namespace org.jppf.dotnet.demo {
  /// <summary>Demo application for the .Net bridge.
  /// <para>The following features are demonstrated:
  /// <ul>
  /// <li>creating and using .Net tasks</li>
  /// <li>creating and submitting jobs</li>
  /// <li>job listeners</li>
  /// <li>processing of execution results</li>
  /// <li>basic management and monitoring</li>
  /// <li>JMX notification listeners</li>
  /// </ul>
  /// </para>
  /// </summary>
  class JPPFDotnetDemo {
    /// <summary>Holds a JMX connection to the JPPF server
    private static JMXDriverConnectionWrapper jmx;

    static void Main(string[] args) {
      JPPFClient client = null;
      TopologyManager manager = null;
      try {
        // initialize the .Net bridge with verbose/quiet mode
        JPPFDotnet.Init(false);
        // initialize the JPPF client
        client = new JPPFClient();
        SubmitWithExecutor(client);
        // initialize a topology manager and register a listener for topology events
        manager = new TopologyManager(client);
        manager.AddTopologyListener(new MyTopologyListener());
        // print the number of nodes connected to the server
        PrintNbNodes(client);
        // provision a slave node for each .Net-capable master node
        ProvisionNodes(client, 1);
        // subscribe to job notifications emitted by the JPPF server
        RegisterJobNotificationListener(client);
        // subscribe to task completion notifications emitted by the JPPF nodes
        RegisterTaskNotificationListener(client);

        JPPFJob job = new JPPFJob();
        job.setName(".NET job");
        // execute the job only on nodes which successfully initialized the .Net bridge
        job.getSLA().setExecutionPolicy(new Equal("jppf.dotnet.bridge.initialized", true));
        int n = 5;
        for (int i=0; i<n; i++) job.add(new MyDotnetTask(1000)).setId("task " + (i + 1));
        MyDotnetTask myTask = new MyDotnetTask(3000);
        // this .Net task will time out after 1.5 second
        myTask.TimeoutSchedule = new JPPFSchedule(1500);
        job.add(myTask).setId("task " + (n + 1));
        // alternatively: job.add(new MyDotnetTask(3000)).setTimeoutSchedule(new JPPFSchedule(1500));
        // add a job listner that prints job events to the console
        job.addJobListener(new MyJobListener());
        Console.WriteLine("created job");
        // submit the job to the grid and get the execution results
        java.util.List results = client.submitJob(job);
        Console.WriteLine("got job results");
        for (int i=0; i<results.size(); i++) {
          Task task = (Task) results.get(i);
          //BaseDotnetTask dotnetTask = job.asBaseDotnetTask(task);
          BaseDotnetTask dotnetTask = task.AsBaseDotnetTask();
          if (dotnetTask != null) { // if .Net task
            if (dotnetTask.Exception != null) {
              Console.WriteLine("got exception for task " + dotnetTask + " : " + dotnetTask.Exception);
              Console.WriteLine(dotnetTask.Exception.StackTrace);
            }
            else if (dotnetTask.Result != null) Console.WriteLine("got result for task " + dotnetTask + " : " + dotnetTask.Result);
            else Console.WriteLine("no result or exception for task " + dotnetTask);
          }
        }
      } catch (Exception e) {
        Console.WriteLine("" + e);
      }

      Console.WriteLine("Please press ESC to terminate");
      do {
        while (!Console.KeyAvailable) {
        }
      } while (Console.ReadKey(true).Key != ConsoleKey.Escape);
      if (client != null) client.close();
    }

    /// <summary>Print the number of nodes connected to the server</summary>
    /// <param name="client">The JPPF client connected to the server</param>
    public static void PrintNbNodes(JPPFClient client) {
      JMXDriverConnectionWrapper jmx = GetJMXConnection(client);
      java.lang.Integer n = jmx.nbNodes();
      int nbNodes = (n == null) ? 0 : n.intValue();
      Console.WriteLine("there are " + nbNodes + " nodes");
    }

    /// <summary>Submit tasks using a JPPFExecutorService</summary>
    /// <param name="client">The JPPF client connected to the server</param>
    public static void SubmitWithExecutor(JPPFClient client) {
      JPPFExecutorService executor = new JPPFExecutorService(client);
      // send tasks 1 at a time
      executor.setBatchSize(1);
      IList<Future> futures = new List<Future>();
      for (int i = 0; i < 3; i++) {
        futures.Add(executor.Submit(new MyDotnetTask(100)));
      }
      // process the results in the order the tasks were submitted
      foreach (Future future in futures) {
        // future.get() returns the value of myTask.Result after execution
        // or throws an eventual exception that was raised
        try {
          object result = future.get();
          if (result != null) Console.WriteLine("[executor service] got result = " + result);
          else Console.WriteLine("[executor service] no result or exception");
        } catch (Exception e) {
          Console.WriteLine("[executor service] exception during execution: " + e);
        }
      }
      executor.shutdownNow();
    }

    /// <summary>Submit tasks using a JPPFCompletionService</summary>
    /// <param name="client">The JPPF client connected to the server</param>
    public static void SubmitWithCompletionService(JPPFClient client) {
      JPPFExecutorService executor = new JPPFExecutorService(client);
      JPPFCompletionService completionService = new JPPFCompletionService(executor);
      // send tasks 3 at a time
      executor.setBatchSize(3);
      for (int i = 0; i < 3; i++) completionService.Submit(new MyDotnetTask(100));
      // process the results in the order in which they arrive
      int count = 0;
      while (count < 3) {
        // get the next completed task
        Future future = completionService.poll();
        count++;
        // future.get() returns the value of myTask.Result after execution
        // or throws an eventual exception that was raised
        try {
          object result = future.get();
          if (result != null) Console.WriteLine("[executor service] got result = " + result);
          else Console.WriteLine("[executor service] no result or exception");
        } catch (Exception e) {
          Console.WriteLine("[executor service] exception during execution: " + e);
        }
      }
      executor.shutdownNow();
    }

    /// <summary>Provision the specified number of slave nodes.
    /// This method forwards the provisioning request to all relevant nodes via the driver</summary>
    /// <param name="client">The JPPF client connected to the server</param>
    /// <param name="nbNodes">The number of slave nodes to provision</param>
    public static void ProvisionNodes(JPPFClient client, int nbNodes) {
      // we are invoking the remote provisioning mbeans
      string mbeanName = JPPFNodeProvisioningMBeanStaticProxy.getMBeanName();
      // policy applied on master nodes with a .Net bridge initialized
      ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true).and(new Equal("jppf.dotnet.bridge.initialized", true));
      NodeSelector masterSelector = new ExecutionPolicySelector(masterPolicy);
      // parameters to the MBean method
      java.lang.Object[] parameters = { new java.lang.Integer(nbNodes), null };
      // Java signature of the remote MBean method to invoke
      java.lang.String[] signature = { "int", "org.jppf.utils.TypedProperties" };
      // send the request via the forwarding mbean
      JMXDriverConnectionWrapper jmx = GetJMXConnection(client);
      JPPFNodeForwardingMBeanStaticProxy proxy = new JPPFNodeForwardingMBeanStaticProxy(jmx);
      proxy.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", parameters, signature);
    }

    /// <summary>Register a listener for jobs life cycle notifications emitted by the server</summary>
    /// <param name="client">The JPPF client from which to get a JMX connection</param>
    public static void RegisterJobNotificationListener(JPPFClient client) {
      JMXDriverConnectionWrapper jmx = GetJMXConnection(client);
      AbstractMBeanStaticProxy jobProxy = new DriverJobManagementMBeanStaticProxy(jmx);
      jobProxy.AddNotificationListener(new MyJobNotificationListener(), "job notification");
    }

    /// <summary>Register a listener for tasks completion notifications emitted by all the nodes</summary>
    /// <param name="client">The JPPF client from which to get a JMX connection</param>
    /// <returns>An id string assigned to the registered listener</returns>
    public static string RegisterTaskNotificationListener(JPPFClient client) {
      // name of the node task monitor MBean
      string mbeanName = JPPFNodeTaskMonitorMBeanStaticProxy.getMBeanName();
      // only receive notifications from .Net-capable nodes
      ExecutionPolicy dotnetPolicy = new Equal("jppf.dotnet.bridge.initialized", true);
      NodeSelector selector = new ExecutionPolicySelector(dotnetPolicy);
      // register the forwarding listener with the driver
      JMXDriverConnectionWrapper jmx = GetJMXConnection(client);
      string listenerId = jmx.RegisterFowrwardingNotificationListener(selector, mbeanName, new MyTaskNotificationListener(), "task notification");
      Console.WriteLine("registered task notifications listener with listenerId = " + listenerId);
      // return the listener id so it can be eventually removed later
      return listenerId;
    }

    /// <summary>Obtain a JMX connection from a JPPF client</summary>
    /// <param name="client">The client to get the JMX connection from</param>
    /// <returns>An instance of <code>JMXDriverConnectionWrapper</code></returns>
    public static JMXDriverConnectionWrapper GetJMXConnection(JPPFClient client) {
      if (jmx == null) {
        // wait for a connection pool to be ready
        JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
        // wait until at least one JMX connection is available from the pool
        java.util.List list = pool.awaitJMXConnections(Operator.AT_LEAST, 1, true);
        // return the first available JMX connection
        jmx = (JMXDriverConnectionWrapper) list.get(0);
      }
      return jmx;
    }
  }

  /// <summary>This job listener implementation prints events to the console</summary>
  class MyJobListener : BaseDotnetJobListener {
    /// <summary>Job started notification</summary>
    /// <param name="jobEvent">The actual job event received from the Java side</param>
    public override void JobStarted(JobEvent jobEvent) {
      WriteEvent(jobEvent, "started");
    }

    /// <summary>Job ended notification</summary>
    /// <param name="jobEvent">The actual job event received from the Java side</param>
    public override void JobEnded(JobEvent jobEvent) {
      WriteEvent(jobEvent, "ended");
    }

    /// <summary>Job dispatched notification</summary>
    /// <param name="jobEvent">The actual job event received from the Java side</param>
    public override void JobDispatched(JobEvent jobEvent) {
      WriteEvent(jobEvent, "dispatched");
    }

    /// <summary>Job returned notification</summary>
    /// <param name="jobEvent">The actual job event received from the Java side</param>
    public override void JobReturned(JobEvent jobEvent) {
      WriteEvent(jobEvent, "returned");
    }

    /// <summary>Print the specified job event to the console.</summary>
    /// <param name="jobEvent">the job event to print</param>
    /// <param name="type">the type of event</param>
    public void WriteEvent(JobEvent jobEvent, string type) {
      JPPFJob job = jobEvent.getJob();
      java.util.List tasks = jobEvent.getJobTasks();
      Console.WriteLine("[.Net] Job '" + job.getName() + "' " + type + (tasks != null ? " with " + tasks.size() + " tasks" : ""));
    }
  }

  /// <summary>This notification listener prints jobs life cycle events events to the console</summary>
  class MyJobNotificationListener : BaseDotnetNotificationListener {
    /// <summary>Handle notifications</summary>
    /// <param name="notification">the job event notification to print</param>
    /// <param name="type">an arbotrary object passed when this listener is registered, may be null</param>
    public override void HandleNotification(Notification notification, object handback) {
      JobNotification notif = notification as JobNotification;
      if (notif != null) {
        JobEventType type = notif.getEventType();
        // skip job updated notifications
        if (type != JobEventType.JOB_UPDATED) {
          JobInformation jobInfo = notif.getJobInformation();
          int n = jobInfo.getTaskCount();
          Console.WriteLine("[MyJobNotificationListener] job '" + jobInfo.getJobName() + "' received " + type + " notification" +
            (n > 0 ? " for " + n + " tasks" : "") + ", handback = " + (handback != null ? "" + handback : "null"));
        }
      }
    }
  }

  /// <summary>This notification listener prints task completion events to the console</summary>
  class MyTaskNotificationListener : BaseDotnetNotificationListener {
    /// <summary>Handle notifications</summary>
    /// <param name="notification">the task event notification to print</param>
    /// <param name="type">an arbotrary object passed when this listener is registered, may be null</param>
    public override void HandleNotification(Notification notification, object handback) {
      JPPFNodeForwardingNotification fwdNotif = notification as JPPFNodeForwardingNotification;
      if (fwdNotif == null) return;
      TaskExecutionNotification notif = fwdNotif.getNotification() as TaskExecutionNotification;
      // only handle JPPF built-in task completion notifications
      if ((notif != null) && !notif.isUserNotification()) {
        TaskInformation taskInfo = notif.getTaskInformation();
        string id = taskInfo.getId();
        if (id == null) id = "null id";
        Console.WriteLine("[MyTaskNotificationListener] task '" + id + "' from job '" + taskInfo.getJobName() +
          "' has completed " + (taskInfo.hasError() ? "with error" : "successfully"));
      }
    }
  }

  /// <summary>A topology listener which prints topology events to the console</summary>
  /// <remarks>Note that here we dot not override the <see cref="BaseDotnetTopologyListener.DriverUpdated(TopologyEvent)"/> and
  /// <see cref="BaseDotnetTopologyListener.NodeUpdated(TopologyEvent)"/> methods, since they are called periodically
  /// (around every second by default) and would thus flood the console output with messages</remarks>
  class MyTopologyListener : BaseDotnetTopologyListener {

    /// <summary>Called when a new driver is added to the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public override void DriverAdded(TopologyEvent topologyEvent) {
      WriteEvent(topologyEvent, "driver added");
    }

    /// <summary>Called when a driver is removed from the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public override void DriverRemoved(TopologyEvent topologyEvent) {
      WriteEvent(topologyEvent, "driver removed");
    }

    /// <summary>Called when a new node is added to the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public override void NodeAdded(TopologyEvent topologyEvent) {
      WriteEvent(topologyEvent, "node added");
    }

    /// <summary>Called when a node is removed from the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public override void NodeRemoved(TopologyEvent topologyEvent) {
      WriteEvent(topologyEvent, "node removed");
    }

    /// <summary>Print a console message describing a topology event notification</summary>
    /// <param name="topologyEvent">the received event</param>
    /// <param name="type">a string describing the type of event notification</param>
    private void WriteEvent(TopologyEvent topologyEvent, string type) {
      TopologyDriver driver = topologyEvent.getDriver();
      string s1 = "none";
      if (driver != null) s1 = driver.getDisplayName();
      TopologyNode node = topologyEvent.getNodeOrPeer();
      string s2 = "none";
      if (node != null) s2 = node.getDisplayName();
      Console.WriteLine("[.Net] topology: " + type + " for driver=" + s1 + " and node=" + s2);
    }
  }
}
