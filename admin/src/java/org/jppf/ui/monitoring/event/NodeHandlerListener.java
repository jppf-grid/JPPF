/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
package org.jppf.ui.monitoring.event;

import java.util.EventListener;

/**
 * Event listener for handling changes to the nodes data.
 * @author Laurent Cohen
 */
public interface NodeHandlerListener extends EventListener
{
	/**
	 * Called to notify that the node handler data has changed.
	 * @param event the object that encapsulates the data change event.
	 */
	void nodeDataUpdated(NodeHandlerEvent event);
	/**
	 * Called to notify that a node was added to a driver.
	 * @param event the object that encapsulates the node addition.
	 */
	void nodeAdded(NodeHandlerEvent event);
	/**
	 * Called to notify that a node was removed from a driver.
	 * @param event the object that encapsulates the node removal.
	 */
	void nodeRemoved(NodeHandlerEvent event);
	/**
	 * Called to notify a driver was added.
	 * @param event the object that encapsulates the driver addition.
	 */
	void driverAdded(NodeHandlerEvent event);
	/**
	 * Called to notify a driver was removed.
	 * @param event the object that encapsulates the driver removal.
	 */
	void driverRemoved(NodeHandlerEvent event);
}
