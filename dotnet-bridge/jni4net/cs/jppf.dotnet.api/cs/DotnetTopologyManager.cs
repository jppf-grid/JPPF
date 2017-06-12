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
using org.jppf.client;
using org.jppf.client.monitoring;
using org.jppf.client.monitoring.topology;
using org.jppf.utils;

namespace org.jppf.client.monitoring.topology {

  /// <summary>///an extension of TopologyManager which handles varargs for the listeners in the constructors</summary>
  public class DotnetTopologyManager: TopologyManager {
    /// <summary>Refrence to the JPPF configuration</summary>
    private static TypedProperties Config = JPPFConfiguration.getProperties();

    /// <summary>Initialize this topology manager with the specified listeners.
    /// The refresh intervals are determined from the configuration, or take a default value of 1000L if they are not configured</summary>
    /// <param name="listeners">the listeners to register</param>
    public DotnetTopologyManager(params BaseDotnetTopologyListener[] listeners)
      : this(new JPPFClient(), listeners) {
    }

    /// <summary>Initialize this topology manager with the specified intervals and listeners</summary>
    /// <param name="topologyRefreshInterval">the interval in millis between refreshes of the topology</param>
    /// <param name="jvmHealthRefreshInterval">the interval in millis between refreshes of the JVM health data</param>
    /// <param name="listeners">the listeners to register</param>
    public DotnetTopologyManager(long topologyRefreshInterval, long jvmHealthRefreshInterval, params BaseDotnetTopologyListener[] listeners)
      : this(topologyRefreshInterval, jvmHealthRefreshInterval, new JPPFClient(), listeners) {
    }

    /// <summary>Initialize this topology manager with the specified client and listeners.
    /// The refresh intervals are determined from the configuration, or take a default value of 1000L if they are not configured</summary>
    /// <param name="client">the JPPF client used internally</param>
    /// <param name="listeners">the listeners to register</param>
    public DotnetTopologyManager(JPPFClient client, params BaseDotnetTopologyListener[] listeners)
      : this(Config.getLong("jppf.admin.refresh.interval.topology", 1000L), Config.getLong("jppf.admin.refresh.interval.health", 1000L), client, listeners) {
    }

    /// <summary>Initialize this topology manager with the specified intervals, client and listeners</summary>
    /// <param name="topologyRefreshInterval">the interval in millis between refreshes of the topology</param>
    /// <param name="jvmHealthRefreshInterval">the interval in millis between refreshes of the JVM health data</param>
    /// <param name="client">the JPPF client used internally</param>
    /// <param name="listeners">the listeners to register</param>
    public DotnetTopologyManager(long topologyRefreshInterval, long jvmHealthRefreshInterval, JPPFClient client, params BaseDotnetTopologyListener[] listeners)
      : base(topologyRefreshInterval, jvmHealthRefreshInterval, client, ToJavaListeners(listeners)) {
    }

    private static TopologyListener[] ToJavaListeners(params BaseDotnetTopologyListener[] listeners) {
      if (listeners == null) return null;
      TopologyListener[] javaListeners = new TopologyListener[listeners.Length];
      for (int i = 0; i < listeners.Length; i++) {
        javaListeners[i] = new DotnetTopologyListenerWrapper(new DotnetTopologyEventDispatcher(listeners[i]));
      }
      return javaListeners;
    }
  }
}
