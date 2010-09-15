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

package org.jppf.comm.recovery;

/**
 * Event filter for ReaperListener objects.
 * @author Laurent Cohen
 */
public interface ReaperEventFilter
{
	/**
	 * Determine whether the specified event is enabled.
	 * @param event the event to check.
	 * @return <code>true</code> if the event is enabled, <code>false</code> if it is filtered out.
	 */
	boolean isEventEnabled(ReaperEvent event);
}
