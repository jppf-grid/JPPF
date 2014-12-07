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
using org.jppf.management;
using org.jppf.management.forwarding;
using org.jppf.node.provisioning;

namespace org.jppf.dotnet.demo {
  class JPPFDotnetDemo {
    static void Main(string[] args) {
      JPPFClient client = null;
      try {
        JPPFDotnet.Init(false); // init the bridge with verbose/quiet mode
        Console.WriteLine("Init() done");
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
        job.add(new JavaDemoTask(), null); // proxy to Java task
        //job.addJobListener(new MyJobListener());
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
          } else { // if Java task
            if (task.getException() != null) Console.WriteLine("got throwable for task " + task + " : " + task.getException());
            else if (task.getResult() != null) Console.WriteLine("got result for task " + task + " : " + task.getResult());
            else Console.WriteLine("no result or exception for task " + task);
          }
        }
        client.close();
      } catch (Exception e) {
        Console.WriteLine("" + e);
        //Console.WriteLine(e.StackTrace);
      } finally {
        if (client != null) client.close();
      }

      Console.WriteLine("Please press ESC to terminate");
      do {
        while (!Console.KeyAvailable) {
        }
      } while (Console.ReadKey(true).Key != ConsoleKey.Escape);
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
      string[] sig = { "int", "org.jppf.utils.TypedProperties" };
      string mbeanName = "org.jppf:name=provisioning,type=node";
      jmx.forwardInvoke(masterSelector, mbeanName, "provisionSlaveNodes", parameters, sig);
      //Console.WriteLine("there are " + n + " nodes");
    }
  }

  class MyJobListener : BaseDotnetJobListener {
    public MyJobListener() {
    }

    /// <summary>Job started notification</summary>
    public override void JobStarted(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "started");
    }

    /// <summary>Job started notification</summary>
    public override void JobEnded(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "ended");
    }

    /// <summary>Job started notification</summary>
    public override void JobDispatched(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "dispatched");
    }

    /// <summary>Job started notification</summary>
    public override void JobReturned(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "returned");
    }

    /// <summary>Job started notification</summary>
    public void WriteEvent(DotnetJobEvent jobEvent, string type) {
      JPPFJob job = jobEvent.Job;
      IList<Task> tasks = jobEvent.Tasks;
      Console.WriteLine("[.Net] Job '" + job.getName() + "' " + type + (tasks != null ? " with " + tasks.Count + " tasks" : ""));
    }
  }
}
