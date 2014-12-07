using System;
using System.Runtime.Serialization;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JPPF tasks.</summary>
  [Serializable()]
  public class BaseDotnetTask {
    private object cancelledLock = new System.Object();
    private object timeoutLock = new System.Object();
    private bool cancelledFlag = false;
    private bool timeoutFlag = false;

    public BaseDotnetTask() {
    }

    /// <summary>Execute this task</summary>
    public virtual void Execute() {
    }

    /// <summary>Called when the cancellation of this task is requested</summary>
    public virtual void OnCancel() {
    }

    /// <value>The cancelled state of this task</value>
    public bool Cancelled {
      get {
        lock (cancelledLock) { return cancelledFlag; }
      }
      internal set {
        lock (cancelledLock) { cancelledFlag = value; }
      }
    }
    
    /// <summary>Called when the cancellation of this task is requested</summary>
    public virtual void OnTimeout() {
    }

    /// <value>The cancelled state of this task</value>
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
