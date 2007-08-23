/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.server.protocol;

import java.util.EventListener;

/**
 * Listener interface for receiving notifications of tasks events.
 * @author Laurent Cohen
 */
public interface JPPFTaskListener extends EventListener
{
	/**
	 * Notify this listerer that an event has occurred during a task's life cycle.
	 * @param event the event this listener is notified of.
	 */
	void eventOccurred(JPPFTaskEvent event);
}
