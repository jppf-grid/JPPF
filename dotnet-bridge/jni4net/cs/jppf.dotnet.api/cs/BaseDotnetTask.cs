using System;
using System.Runtime.Serialization;

namespace org.jppf.dotnet {
  /// <summary>This is the base class for .Net JPPF tasks. It is intended solely for subclassing.
  /// The <see cref="BaseDotnetTask.Execute()"/> method is run on remote JPPF nodes</summary>
  [Serializable()]
  public class BaseDotnetTask {
    private object cancelledLock = new System.Object();
    private object timeoutLock = new System.Object();
    private bool cancelledFlag = false;
    private bool timeoutFlag = false;

    public BaseDotnetTask() {
    }

    /// <summary>Execute this task. This method should be overriden in subclasses</summary>
    public virtual void Execute() {
    }

    /// <summary>Called when the cancellation of this task is requested</summary>
    public virtual void OnCancel() {
    }

    /// <summary>The cancelled state of this task. It is set by the Java side and can be used during
    /// the computation to determine whether the task should terminate immediately</summary>
    public bool Cancelled {
      get {
        lock (cancelledLock) { return cancelledFlag; }
      }
      internal set {
        lock (cancelledLock) { cancelledFlag = value; }
      }
    }
    
    /// <summary>Called when this task times out.
    /// The timeout is set with a call to <code>org.jppf.node.protocol.Task.setTimeout(org.jppf.scheduling.JPPFSchedule)</code></summary>
    public virtual void OnTimeout() {
    }

    /// <summary>The cancelled state of this task. It is set by the Java side and can be used during
    /// the computation to determine whether the task should terminate immediately</summary>
    public bool TimedOut {
      get {
        lock (timeoutLock) { return timeoutFlag; }
      }
      internal set {
        lock (timeoutLock) { timeoutFlag = value; }
      }
    }
    
    /// <summary>The exception, if any, raised by the execution of this task</summary>
    public Exception Exception { get; internal set; }

    /// <summary>The result, if any, of this task's execution</summary>
    public object Result { get; set; }
  }
}
