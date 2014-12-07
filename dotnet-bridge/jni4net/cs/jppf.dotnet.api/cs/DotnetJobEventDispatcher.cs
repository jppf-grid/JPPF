using System;
using System.Collections.Generic;
using org.jppf.client;
using org.jppf.client.@event;
using org.jppf.node.protocol;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JPPF job listeners.</summary>
  public class DotnetJobEventDispatcher {
    private bool verbose = false;
    private JPPFJob job;
    private BaseDotnetJobListener listener;

    public DotnetJobEventDispatcher(JPPFJob job, BaseDotnetJobListener listener) {
      this.job = job;
      this.listener = listener;
    }

    public DotnetJobEventDispatcher(JPPFJob job, BaseDotnetJobListener listener, bool verbose) {
      this.job = job;
      this.listener = listener;
      this.verbose = verbose;
    }

    /// <summary>Job started notification</summary>
    public virtual void JobStarted(int[] positions) {
      if (verbose) WriteEvent(positions, "started");
      listener.JobStarted(CreateEvent(positions));
    }

    /// <summary>Job started notification</summary>
    public virtual void JobEnded(int[] positions) {
      if (verbose) WriteEvent(positions, "ended");
      listener.JobEnded(CreateEvent(positions));
    }

    /// <summary>Job started notification</summary>
    public virtual void JobDispatched(int[] positions) {
      if (verbose) WriteEvent(positions, "dispatched");
      listener.JobDispatched(CreateEvent(positions));
    }

    /// <summary>Job started notification</summary>
    public virtual void JobReturned(int[] positions) {
      if (verbose) WriteEvent(positions, "returned");
      listener.JobReturned(CreateEvent(positions));
    }

    private DotnetJobEvent CreateEvent(int[] positions) {
      IList<Task> tasks = null;
      if (positions != null) {
        tasks = new List<Task>();
        foreach (int pos in positions) tasks.Add(job.getResults().getResultTask(pos));
      }
      return new DotnetJobEvent(job, tasks);
    }

    /// <summary>Job started notification</summary>
    private void WriteEvent(int[] positions, string type) {
      Console.WriteLine("[.Net] dispatcher: Job '" + job.getName() + "' " + type + (positions != null ? " with " + positions.Length + " tasks" : ""));
    }
  }

  /// <summary>Base .Net class for JPPF job listeners.</summary>
  public class DotnetJobEvent {
    private JPPFJob job = null;
    private IList<Task> tasks = null;
    public JPPFJob Job { get { return job; } }
    public IList<Task> Tasks { get { return tasks; } }

    public DotnetJobEvent(JPPFJob job, IList<Task> tasks) {
      this.job = job;
      this.tasks = tasks;
    }
  }
}
