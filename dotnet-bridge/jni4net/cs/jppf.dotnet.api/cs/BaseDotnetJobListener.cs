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
using org.jppf.client.@event;
using org.jppf.node.protocol;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JPPF job listeners.</summary>
  public class BaseDotnetJobListener {

    public BaseDotnetJobListener() {
    }

    /// <summary>Called when the job is submitted or re-submitted</summary>
    /// <param name="jobEvent">encapsulates the event</param>
    public virtual void JobStarted(JobEvent jobEvent) {
    }

    /// <summary>Called when the job has completed</summary>
    /// <param name="jobEvent">encapsulates the event</param>
    public virtual void JobEnded(JobEvent jobEvent) {
    }

    /// <summary>Called when a set of tasks is dispatched to the server</summary>
    /// <param name="jobEvent">encapsulates the event</param>
    public virtual void JobDispatched(JobEvent jobEvent) {
    }

    /// <summary>Called when a set of tasks returns from the server</summary>
    /// <param name="jobEvent">encapsulates the event</param>
    public virtual void JobReturned(JobEvent jobEvent) {
    }
  }

  /// <summary>This class wraps a <see cref="BaseDotnetJobListener"/> and dispatches job events received from the Java side.</summary>
  /// <remarks>This class is only for internal use on the .Net side</remarks>
  public class DotnetJobEventDispatcher {
    private bool verbose = false;
    private BaseDotnetJobListener listener;

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    public DotnetJobEventDispatcher(BaseDotnetJobListener listener) {
      this.listener = listener;
    }

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    /// <param name="verbose">if <code>true</code>, then this instance will print a console message upon each event notification.
    /// This is intended as a debugging help.</param>
    public DotnetJobEventDispatcher(BaseDotnetJobListener listener, bool verbose) {
      this.listener = listener;
      this.verbose = verbose;
    }

    /// <summary>Job started notification</summary>
    public virtual void JobStarted(java.lang.Object jobEvent) {
      JobEvent e = jobEvent as JobEvent;
      if (verbose) WriteEvent(e, "started");
      listener.JobStarted(e);
    }

    /// <summary>Job started notification</summary>
    /// <param name="jobEvent">encapsulates the job and the tasks that were disptached of whose results have been received</param>
    public virtual void JobEnded(java.lang.Object jobEvent) {
      JobEvent e = jobEvent as JobEvent;
      if (verbose) WriteEvent(e, "ended");
      listener.JobEnded(e);
    }

    /// <summary>Job dispatched notification</summary>
    /// <param name="jobEvent">encapsulates the job and the tasks that were disptached of whose results have been received</param>
    public virtual void JobDispatched(java.lang.Object jobEvent) {
      JobEvent e = jobEvent as JobEvent;
      if (verbose) WriteEvent(e, "dispatched");
      listener.JobDispatched(e as JobEvent);
    }

    /// <summary>Job returned notification</summary>
    /// <param name="jobEvent">encapsulates the job and the tasks that were disptached of whose results have been received</param>
    public virtual void JobReturned(java.lang.Object jobEvent) {
      JobEvent e = jobEvent as JobEvent;
      if (verbose) WriteEvent(e, "returned");
      listener.JobReturned(e);
    }

    /// <summary>Print a console message describing a received event notification</summary>
    /// <param name="positions">the positions in the job of the tasks that were disptached of whose reuslts have been received</param>
    /// <param name="type">a string describing the type of even notification</param>
    private void WriteEvent(JobEvent jobEvent, string type) {
      Console.WriteLine("[.Net] dispatcher: Job '" + jobEvent.getJob().getName() + "' " + type + " with " + jobEvent.getJobTasks().size() + " tasks");
    }
  }
}
