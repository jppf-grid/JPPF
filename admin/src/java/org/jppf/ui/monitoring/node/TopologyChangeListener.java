/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.ui.monitoring.node;

/**
 * Listener interface for changes in the grid topology emitted by the {@link NodeDataPanel}. 
 * @author Laurent Cohen
 */
public interface TopologyChangeListener
{
  /**
   * Called when a driver is added.
   * @param event the event encapsulating the change information.
   */
  void driverAdded(TopologyChangeEvent event);

  /**
   * Called when a driver is removed.
   * @param event the event encapsulating the change information.
   */
  void driverRemoved(TopologyChangeEvent event);

  /**
   * Called when a node is added.
   * @param event the event encapsulating the change information.
   */
  void nodeAdded(TopologyChangeEvent event);

  /**
   * Called when a node is removed.
   * @param event the event encapsulating the change information.
   */
  void nodeRemoved(TopologyChangeEvent event);

  /**
   * Called when the data for a node or driver changed.
   * @param event the event encapsulating the change information.
   */
  void dataUpdated(TopologyChangeEvent event);

}
