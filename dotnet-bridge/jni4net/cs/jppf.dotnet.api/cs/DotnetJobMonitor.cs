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
using System.Threading;
using org.jppf.dotnet;
using org.jppf.client.monitoring;
using org.jppf.client.monitoring.topology;
using org.jppf.client.monitoring.jobs;

namespace org.jppf.client.monitoring.jobs {

  /// <summary>///an extension of JobMonitor which handles varargs for the listeners in the constructors</summary>
  public class DotnetJobMonitor : JobMonitor {

    /// <summary>Initialize this job monitor with the specified topology manager and listeners</summary>
    /// <param name="topologyManager">topologyManager</param>
    /// <param name="listeners">the listeners to register</param>
    public DotnetJobMonitor(TopologyManager topologyManager, params BaseDotnetJobMonitoringListener[] listeners)
      : this(JobMonitorUpdateMode.IMMEDIATE_NOTIFICATIONS, 0L, topologyManager, listeners) {
    }

    /// <summary>Initialize this job monitor with the specified update mode, refresh interval, topology manager and listeners</summary>
    /// <param name="topologyRefreshInterval">the interval in millis between refreshes of the topology</param>
    /// <param name="jvmHealthRefreshInterval">the interval in millis between refreshes of the JVM health data</param>
    /// <param name="client">the JPPF client used internally</param>
    /// <param name="listeners">the listeners to register</param>
    public DotnetJobMonitor(JobMonitorUpdateMode mode, long period, TopologyManager topologyManager, params BaseDotnetJobMonitoringListener[] listeners)
      : base(mode, period, topologyManager, ToJavaListeners(listeners)) {
    }

    private static JobMonitoringListener[] ToJavaListeners(params BaseDotnetJobMonitoringListener[] listeners) {
      if (listeners == null) return null;
      JobMonitoringListener[] javaListeners = new JobMonitoringListener[listeners.Length];
      for (int i=0; i<listeners.Length; i++) {
        javaListeners[i] = new DotnetJobMonitoringListenerWrapper(new DotnetJobMonitoringEventDispatcher(listeners[i]));
      }
      return javaListeners;
    }
  }
}
