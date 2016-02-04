/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.dotnet;

import org.jppf.client.monitoring.topology.*;

/**
 * This class wraps a .Net topology listener to which toplogy event notifications are delegated.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class DotnetTopologyListenerWrapper extends AbstractDotnetListenerWrapper implements TopologyListener {
  /**
   * Initialize this wrapper with the specified proxy to a .Net job listener.
   * @param dotnetListener a proxy to a .Net job listener.
   */
  public DotnetTopologyListenerWrapper(final system.Object dotnetListener) {
    super(false, dotnetListener, "DriverAdded", "DriverRemoved", "DriverUpdated", "NodeAdded", "NodeRemoved", "NodeUpdated");
  }

  @Override
  public void driverAdded(TopologyEvent event) {
    delegate(event, "DriverAdded");
  }

  @Override
  public void driverRemoved(TopologyEvent event) {
    delegate(event, "DriverRemoved");
  }

  @Override
  public void driverUpdated(TopologyEvent event) {
    delegate(event, "DriverUpdated");
  }

  @Override
  public void nodeAdded(TopologyEvent event) {
    delegate(event, "NodeAdded");
  }

  @Override
  public void nodeRemoved(TopologyEvent event) {
    delegate(event, "NodeRemoved");
  }

  @Override
  public void nodeUpdated(TopologyEvent event) {
    delegate(event, "NodeUpdated");
  }
}
