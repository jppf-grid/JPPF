using System;
using org.jppf.client;
using org.jppf.client.@event;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JPPF job listeners.</summary>
  public class BaseDotnetJobListener {

    public BaseDotnetJobListener() {
    }

    /// <summary>Job started notification</summary>
    public virtual void JobStarted(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "started");
    }

    /// <summary>Job started notification</summary>
    public virtual void JobEnded(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "ended");
    }

    /// <summary>Job started notification</summary>
    public virtual void JobDispatched(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "dispatched");
    }

    /// <summary>Job started notification</summary>
    public virtual void JobReturned(DotnetJobEvent jobEvent) {
      WriteEvent(jobEvent, "returned");
    }

    /// <summary>Job started notification</summary>
    private void WriteEvent(DotnetJobEvent jobEvent, string type) {
      //Console.WriteLine("[.Net] Job '" + job.getName() + "' " + type + (positions != null ? " with " + positions.Length + " tasks" : ""));
    }
  }
}
