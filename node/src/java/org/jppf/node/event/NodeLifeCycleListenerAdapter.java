/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.node.event;

/**
 * Convenience class that can be used when not all methods of {@link NodeLifeCycleListener} need to be implemented.
 * @author Laurent Cohen
 */
public class NodeLifeCycleListenerAdapter implements NodeLifeCycleListener {
  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event) {
  }

  @Override
  public void beforeNextJob(final NodeLifeCycleEvent event) {
  }
}
