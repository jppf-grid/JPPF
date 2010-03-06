/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.server.protocol;

import java.util.EventListener;

/**
 * Listener for events occurring with {@link Location} objects.
 * @author Laurent Cohen
 */
public interface LocationEventListener extends EventListener
{
	/**
	 * Invoked when a data transfer has occurred between the location pointed to by the event
	 * and another location.
	 * @param event - a representation of the event that occurred.
	 */
	void dataTransferred(LocationEvent event);
}
