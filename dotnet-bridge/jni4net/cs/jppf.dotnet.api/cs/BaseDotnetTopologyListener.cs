/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
using org.jppf.client.monitoring.topology;

namespace org.jppf.dotnet {
  /// <summary>Base .Net class for JPPF topology listeners.</summary>
  public class BaseDotnetTopologyListener {

    public BaseDotnetTopologyListener() {
    }

    /// <summary>Called when a new driver is added to the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void DriverAdded(TopologyEvent topologyEvent) {
    }

    /// <summary>Called when a driver is removed from the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void DriverRemoved(TopologyEvent topologyEvent) {
    }

    /// <summary>Called when information on a driver is updated</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void DriverUpdated(TopologyEvent topologyEvent) {
    }

    /// <summary>Called when a new node is added to the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void NodeAdded(TopologyEvent topologyEvent) {
    }

    /// <summary>Called when a node is removed from the grid</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void NodeRemoved(TopologyEvent topologyEvent) {
    }

    /// <summary>Called when information on a node is updated</summary>
    /// <param name="jobEvent">encapsulates the topology event</param>
    public virtual void NodeUpdated(TopologyEvent topologyEvent) {
    }
  }

  /// <summary>This class wraps a <see cref="BaseDotnetTopologyListener"/> and dispatches topology events received from the Java side.</summary>
  /// <remarks>This class is only for internal use on the .Net side</remarks>
  public class DotnetTopologyEventDispatcher {
    private bool verbose = false;
    private BaseDotnetTopologyListener listener;

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    public DotnetTopologyEventDispatcher(BaseDotnetTopologyListener listener) {
      this.listener = listener;
    }

    /// <summary>Initialize this job event disptacher</summary>
    /// <param name="listener">the <see cref="BaseDotnetJobListener"/> to which the events are dispatched</param>
    /// <param name="verbose">if <code>true</code>, then this instance will print a console message upon each event notification.
    /// This is intended as a debugging help.</param>
    public DotnetTopologyEventDispatcher(BaseDotnetTopologyListener listener, bool verbose) {
      this.listener = listener;
      this.verbose = verbose;
    }

    /// <summary>Driver added notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related topology components</param>
    public virtual void DriverAdded(java.lang.Object topologyEvent) {
      TopologyEvent e = topologyEvent as TopologyEvent;
      if (verbose) WriteEvent(e, "driver added");
      listener.DriverAdded(e);
    }

    /// <summary>Driver removed notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related topology components</param>
    public virtual void DriverRemoved(java.lang.Object topologyEvent) {
      TopologyEvent e = topologyEvent as TopologyEvent;
      if (verbose) WriteEvent(e, "driver removed");
      listener.DriverRemoved(e);
    }

    /// <summary>Driver updated notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related topology components</param>
    public virtual void DriverUpdated(java.lang.Object topologyEvent) {
      TopologyEvent e = topologyEvent as TopologyEvent;
      if (verbose) WriteEvent(e, "driver updated");
      listener.DriverUpdated(e);
    }

    /// <summary>Node added notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related topology components</param>
    public virtual void NodeAdded(java.lang.Object topologyEvent) {
      TopologyEvent e = topologyEvent as TopologyEvent;
      if (verbose) WriteEvent(e, "node added");
      listener.NodeAdded(e);
    }

    /// <summary>Node removed notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related topology components</param>
    public virtual void NodeRemoved(java.lang.Object topologyEvent) {
      TopologyEvent e = topologyEvent as TopologyEvent;
      if (verbose) WriteEvent(e, "node removed");
      listener.NodeRemoved(e);
    }

    /// <summary>Node updated notification</summary>
    /// <param name="jobEvent">encapsulates information about the event and related topology components</param>
    public virtual void NodeUpdated(java.lang.Object topologyEvent) {
      TopologyEvent e = topologyEvent as TopologyEvent;
      if (verbose) WriteEvent(e, "node updated");
      listener.NodeUpdated(e);
    }

    /// <summary>Print a console message describing a received event notification</summary>
    /// <param name="topologyEvent">the received event</param>
    /// <param name="type">a string describing the type of event notification</param>
    private void WriteEvent(TopologyEvent topologyEvent, string type) {
      TopologyDriver driver = topologyEvent.getDriver();
      string s1 = "none";
      if (driver != null) s1 = driver.getDisplayName();
      TopologyNode node = topologyEvent.getNodeOrPeer();
      string s2 = "none";
      if (node != null) s2 = node.getDisplayName();
      Console.WriteLine("[.Net] dispatcher: topology event '" + type + "' for driver=" + s1 + " and node=" + s2);
    }
  }
}
