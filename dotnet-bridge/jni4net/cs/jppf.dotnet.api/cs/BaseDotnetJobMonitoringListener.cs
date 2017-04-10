/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
using org.jppf.client;
using org.jppf.client.monitoring.jobs;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JPPF job monitoring listeners.</summary>
  public class BaseDotnetJobMonitoringListener {

    public BaseDotnetJobMonitoringListener() {
    }

    /// <summary>Called when a new driver is added to the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void DriverAdded(JobMonitoringEvent jobEvent) {
    }

    /// <summary>Called when a driver is removed from the grid</summary>
    /// <param name="jobEvent">encapsulates the job monitoring event</param>
    public virtual void DriverRemoved(JobMonitoringEvent jobEvent) {
    }

    /// <summary>Called when a new job is added to a driver</summary>
    /// <param name="jobEvent">encapsulates the job monitoring event</param>
    public virtual void JobUpdated(JobMonitoringEvent jobEvent) {
    }

    /// <summary>Called when a job is removed from a driver</summary>
    /// <param name="jobEvent">encapsulates the job monitoring event</param>
    public virtual void JobAdded(JobMonitoringEvent jobEvent) {
    }

    /// <summary>Called when a job is updated in a driver</summary>
    /// <param name="jobEvent">encapsulates the job monitoring event</param>
    public virtual void JobRemoved(JobMonitoringEvent jobEvent) {
    }

    /// <summary>Called when a job is dispatched to a node</summary>
    /// <param name="jobEvent">encapsulates the job monitoring event</param>
    public virtual void JobDispatchAdded(JobMonitoringEvent jobEvent) {
    }

    /// <summary>Called when a job returns from a node</summary>
    /// <param name="jobEvent">encapsulates the job monitoring event</param>
    public virtual void JobDispatchRemoved(JobMonitoringEvent jobEvent) {
    }
  }

  /// <summary>This class wraps a <see cref="BaseDotnetTopologyListener"/> and dispatches topology events received from the Java side.</summary>
  /// <remarks>This class is only for internal use on the .Net side</remarks>
  public class DotnetJobMonitoringEventDispatcher {
    private bool verbose = false;
    private BaseDotnetJobMonitoringListener listener;

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    public DotnetJobMonitoringEventDispatcher(BaseDotnetJobMonitoringListener listener) {
      this.listener = listener;
    }

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    /// <param name="verbose">if <code>true</code>, then this instance will print a console message upon each event notification.
    /// This is intended as a debugging help.</param>
    public DotnetJobMonitoringEventDispatcher(BaseDotnetJobMonitoringListener listener, bool verbose) {
      this.listener = listener;
      this.verbose = verbose;
    }

    /// <summary>Driver added notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void DriverAdded(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "driver added");
      listener.DriverAdded(e);
    }

    /// <summary>Driver removed notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void DriverRemoved(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "driver removed");
      listener.DriverRemoved(e);
    }

    /// <summary>Job added notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void JobAdded(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "job added");
      listener.JobAdded(e);
    }

    /// <summary>Job removed notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void JobRemoved(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "job removed");
      listener.JobRemoved(e);
    }

    /// <summary>Job updated notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void JobUpdated(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "job updated");
      listener.JobUpdated(e);
    }

    /// <summary>Job dispatch added notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void JobDispatchAdded(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "job dispatch added");
      listener.JobDispatchAdded(e);
    }

    /// <summary>Job dispatch removed notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related job components</param>
    public virtual void JobDispatchRemoved(java.lang.Object jobEvent) {
      JobMonitoringEvent e = jobEvent as JobMonitoringEvent;
      if (verbose) WriteEvent(e, "job dispatch removed");
      listener.JobDispatchRemoved(e);
    }

    /// <summary>Print a console message describing a received event notification</summary>
    /// <param name="jobEvent">the received event</param>
    /// <param name="type">a string describing the type of event notification</param>
    private void WriteEvent(JobMonitoringEvent jobEvent, string type) {
      Console.WriteLine("[.Net] job dispatcher: job monitoring event '" + type + "' for driver=" + GetName(jobEvent.getJobDriver()) +
        ", job=" + GetName(jobEvent.getJob()) + " and dispatch=" + GetName(jobEvent.getJobDispatch()));
    }

    private string GetName(AbstractJobComponent comp) {
      if (comp == null) return "none";
      return comp.getDisplayName();
    }
  }
}
