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

package org.jppf.node.event;

/**
 * Extension of the {@link NodeLifeCycleListener} insterface which receives additional
 * notifications right after the header of a job has been received, and before the 
 * data provider and tasks are received.
 * <p>In this case, the event object also provides a  reference to the class loader used
 * to deserialize the tasks and load the class they need from the client.
 * @author Laurent Cohen
 */
public interface NodeLifeCycleListenerEx extends NodeLifeCycleListener
{
  /**
   * Called when the node has loaded a job header and before the <code>DataProvider</code> or any of the tasks has been loaded.
   * <br>Note that <code>event.getTasks()</code> will return <code>null</code> at this point.
   * @param event encapsulates information about the job.
   */
  void jobHeaderLoaded(NodeLifeCycleEvent event);
}
