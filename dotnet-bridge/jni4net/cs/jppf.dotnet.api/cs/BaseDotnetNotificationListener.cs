using System;
using javax.management;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JMX notification listeners.</summary>
  public class BaseDotnetNotificationListener {

    public BaseDotnetNotificationListener() {
    }

    /// <summary>handle a notification</summary>
    /// <param name="notification">the notification object received from the Java side</param>
    /// <param name="handback">an arbitrary object provided when the listerner was registered</param>
    public virtual void HandleNotification(Notification notification, object handback) {
    }
  }

  /// <summary>This class wraps a <see cref="BaseDotnetJobListener"/> and dispatches job events received from the Java side.</summary>
  /// <remarks>This class is only for internal use on the .Net side</remarks>
  public class DotnetNotificationDispatcher {
    private bool verbose = false;
    private BaseDotnetNotificationListener listener;
    private object handback = null;

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    /// <param name="handback">an arbitrary object provided when the listerner was registered</param>
    public DotnetNotificationDispatcher(BaseDotnetNotificationListener listener, object handback) {
      this.listener = listener;
      this.handback = handback;
    }

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    /// <param name="handback">an arbitrary object provided when the listerner was registered</param>
    /// <param name="verbose">if <code>true</code>, then this instance will print a console message upon each event notification.
    /// This is intended as a debugging help.</param>
    public DotnetNotificationDispatcher(BaseDotnetNotificationListener listener, object handback, bool verbose) {
      this.listener = listener;
      this.handback = handback;
      this.verbose = verbose;
    }

    /// <summary>Handle a JMX notification</summary>
    /// <param name="notification">the notification object received from the Java side</param>
    public virtual void HandleNotification(java.lang.Object notification) {
      if (verbose) WriteEvent(notification as Notification, handback);
      listener.HandleNotification(notification as Notification, handback);
    }

    /// <summary>Print a console message describing a JMX notification</summary>
    /// <param name="notification">the notification object received from the Java side</param>
    /// <param name="handback">an arbitrary object provided when the listerner was registered</param>
    private void WriteEvent(Notification notification, object handback) {
      Console.WriteLine("[.Net] received notification " + notification + ", handback = " + (handback != null ? "" + handback : "null"));
    }
  }
}
