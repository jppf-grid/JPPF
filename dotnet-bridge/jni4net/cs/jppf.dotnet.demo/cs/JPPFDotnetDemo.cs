using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using net.sf.jni4net;
using org.jppf.client;
using org.jppf.client.@event;
using org.jppf.dotnet;
using org.jppf.node.policy;
using org.jppf.node.protocol;
using org.jppf.dotnet;
using org.jppf.scheduling;
using javax.management;
using org.jppf.job;
using org.jppf.management;
using org.jppf.management.generated;
using org.jppf.management.forwarding;
using org.jppf.node.provisioning;

namespace org.jppf.dotnet.demo {
  class JPPFDotnetDemo {
    static void Main(string[] args) {
      JPPFClient client = null;
      try {
        JPPFDotnet.Init(false); // init the bridge with verbose/quiet mode
        client = new JPPFClient();
        PrintNbNodes(client);
        ProvisionNodes(client, 3);
        JPPFJob job = new JPPFJob();
        job.setName(".NET job");
        // execute only on nodes which successfully initialized the .Net bridge
        job.getSLA().setExecutionPolicy(new Equal("jppf.dotnet.bridge.initialized", true));
        job.add(new MyDotnetTask(1000), true); // .Net task
        // this .Net task will time out after 1.5 second
        job.add(new MyDotnetTask(3000), true).setTimeoutSchedule(new JPPFSchedule(1500));
        // add a job listner that prints job events to the console
        job.addJobListener(new MyJobListener());
        Console.WriteLine("created job");
        java.util.List results = client.submitJob(job);
        Console.WriteLine("got job results");
        for (int i=0; i<results.size(); i++) {
          Task task = (Task) results.get(i);
          BaseDotnetTask dotnetTask = job.asBaseDotnetTask(task);
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
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      Console.WriteLine("connection pool = " + pool);
      java.util.List list = pool.awaitJMXConnections(Operator.AT_LEAST, 1, true);
      JMXDriverConnectionWrapper jmx = (JMXDriverConnectionWrapper) list.get(0);
      Console.WriteLine("jmx connection = " + jmx);
      java.lang.Integer n = jmx.nbNodes();
      Console.WriteLine("there are " + n + " nodes");
    }

    /// <summary>Provision the specified number of slave nodes</summary>
    /// <param name="client">The JPPF client connected to the server</param>
    /// <param name="nbNodes">The number of slave nodes to provision</param>
    public static void ProvisionNodes(JPPFClient client, int nbNodes) {
      JPPFConnectionPool pool = client.awaitWorkingConnectionPool();
      java.util.List list = pool.awaitJMXConnections(Operator.AT_LEAST, 1, true);
      JMXDriverConnectionWrapper jmx = (JMXDriverConnectionWrapper) list.get(0);
      // policy applied on master nodes with a .Net bridge initialized
      ExecutionPolicy masterPolicy = new Equal("jppf.node.provisioning.master", true).and(new Equal("jppf.dotnet.bridge.initialized", true));
      NodeSelector masterSelector = new ExecutionPolicySelector(masterPolicy);
      java.lang.Object[] parameters = { new java.lang.Integer(nbNodes), null };
      java.lang.String[] sig = { new java.lang.String("int"), new java.lang.String("org.jppf.utils.TypedProperties") };
      string mbeanName = "org.jppf:name=provisioning,type=node";
      JPPFNodeForwardingMBeanStaticProxy proxy = new JPPFNodeForwardingMBeanStaticProxy(jmx);
      proxy.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", parameters, sig);
      java.util.Map map = proxy.forwardInvoke(masterSelector, "org.jppf:name=admin,type=node", "state");
      int n = map.size();
      Console.WriteLine("there are " + n + " node states");
      java.util.Iterator it = map.keySet().iterator();
      while (it.hasNext()) {
        java.lang.String uuid = (java.lang.String) it.next();
        object value = map.get(uuid);
        JPPFNodeState state = value as JPPFNodeState;
        if (state != null) {
          Console.WriteLine("node " + uuid + " has " + state.getThreadPoolSize() + " processing threads, full state = " + state);
        } else {
          Console.WriteLine("node " + uuid + " raised an exception:" + value);
          //e.printStackTrace();
        }
      }
      //proxy.AddNotificationListener(new MyNotificationListener(), "handback for MyNotificationListener");
      AbstractMBeanStaticProxy jobProxy = new DriverJobManagementMBeanStaticProxy(jmx);
      jobProxy.AddNotificationListener(new MyNotificationListener(), "handback for MyNotificationListener");
    }
  }

  /// <summary>This job listener implementation prints events to the console</summary>
  class MyJobListener : BaseDotnetJobListener {
    public MyJobListener() {
    }

    /// <summary>Job started notification</summary>
    public override void JobStarted(JobEvent jobEvent) {
      WriteEvent(jobEvent, "started");
    }

    /// <summary>Job end notification</summary>
    public override void JobEnded(JobEvent jobEvent) {
      WriteEvent(jobEvent, "ended");
    }

    /// <summary>Job dispatched notification</summary>
    public override void JobDispatched(JobEvent jobEvent) {
      WriteEvent(jobEvent, "dispatched");
    }

    /// <summary>Job returned notification</summary>
    public override void JobReturned(JobEvent jobEvent) {
      WriteEvent(jobEvent, "returned");
    }

    /// <summary>Print the psecified job event ot the console.</summary>
    /// <param name="jobEvent">the job event to print</param>
    /// <param name="type">the type of event</param>
    public void WriteEvent(JobEvent jobEvent, string type) {
      JPPFJob job = jobEvent.getJob();
      java.util.List tasks = jobEvent.getJobTasks();
      Console.WriteLine("[.Net] Job '" + job.getName() + "' " + type + (tasks != null ? " with " + tasks.size() + " tasks" : ""));
    }
  }

  /// <summary>This job listener implementation prints events to the console</summary>
  class MyNotificationListener : BaseDotnetNotificationListener {
    public MyNotificationListener() {
    }

    /// <summary>handle notification</summary>
    public override void HandleNotification(Notification notification, object handback) {
      JobNotification notif = notification as JobNotification;
      if (notif != null) {
        JobEventType type = notif.getEventType();
        int n = notif.getJobInformation().getTaskCount();
        if (type != JobEventType.JOB_UPDATED) {
          Console.WriteLine("[.Net] MyNotificationListener job '" + notif.getJobInformation().getJobName() + "' received " +  type + " notification" +
            (n > 0 ? " for " + n + " tasks" : "") + ", handback = " + (handback != null ? "" + handback : "null"));
        }
      }
    }
  }
}
