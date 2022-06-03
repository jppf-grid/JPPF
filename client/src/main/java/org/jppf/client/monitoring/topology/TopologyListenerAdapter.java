/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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
package org.jppf.client.monitoring.topology;

/**
 * A convenience class for subclasses that wish to receive topology events
 * without having to implement all the methods of the {@link TopologyListener} interface.
 * @author Laurent Cohen
 * @since 5.0
 */
public class TopologyListenerAdapter implements TopologyListener {
  @Override
  public void driverAdded(final TopologyEvent event) {
  }

  @Override
  public void driverRemoved(final TopologyEvent event) {
  }

  @Override
  public void driverUpdated(final TopologyEvent event) {
  }

  @Override
  public void nodeAdded(final TopologyEvent event) {
  }

  @Override
  public void nodeRemoved(final TopologyEvent event) {
  }

  @Override
  public void nodeUpdated(final TopologyEvent event) {
  }
}
