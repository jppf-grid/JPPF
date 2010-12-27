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

package org.jppf.process.event;

import java.util.EventObject;

/**
 * Instances of this class encapsulate an event occurring when a process writes to
 * its output or error stream. 
 * @author Laurent Cohen
 */
public class ProcessWrapperEvent extends EventObject
{
	/**
	 * Initialize this event with the specified source.
	 * @param content the source of this event, in effect the content of the corresponding process stream.
	 */
	public ProcessWrapperEvent(String content)
	{
		super(content);
	}

	/**
	 * Get the content of this event.
	 * @return the content as a string.
	 */
	public String getContent()
	{
		return (String) getSource();
	}
}
