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

import java.util.EventListener;

/**
 * Listener interface for receiving veent notifications from a process wrapper.
 * @author Laurent Cohen
 */
public interface ProcessWrapperEventListener extends EventListener
{
	/**
	 * Notification that the process has written to its output stream.
	 * @param event encapsulate the output stream's content.
	 */
	void outputStreamAltered(ProcessWrapperEvent event);
	/**
	 * Notification that the process has written to its error stream.
	 * @param event encapsulate the error stream's content.
	 */
	void errorStreamAltered(ProcessWrapperEvent event);
}
