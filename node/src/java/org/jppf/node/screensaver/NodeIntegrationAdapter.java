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

package org.jppf.node.screensaver;

import org.jppf.node.event.*;

/**
 * An abstract adapter class for receiving node events. The methods in this class are empty.
 * This class exists as convenience, to be overriden for creating listener objects, instead of
 * implemnting the {@link NodeLifeCycleListener} and {@link TaskExecutionListener} interfaces.
 * @author Laurent Cohen
 */
public abstract class NodeIntegrationAdapter extends NodeLifeCycleListenerAdapter implements NodeIntegration
{
  @Override
  public void taskExecuted(final TaskExecutionEvent event)
  {
  }
}
