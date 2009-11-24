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

package org.jppf.demo.console;

import org.jppf.process.event.*;
import org.jppf.ui.options.TextAreaOption;

/**
 * Process event listener that redirects a process output to a text area component.
 * @author Laurent Cohen
 */
public class TextAreaProcessListener implements ProcessWrapperEventListener
{
	/**
	 * The text area to write the process output to.
	 */
	private TextAreaOption area = null;

	/**
	 * Initialize this event listener with the specified text area component.
	 * @param area the text area to write the process output to.
	 */
	public TextAreaProcessListener(TextAreaOption area)
	{
		this.area = area;
	}

	/**
	 * Notification that the process has written to its output stream.
	 * @param event encapsulate the output stream's content.
	 * @see org.jppf.process.event.ProcessWrapperEventListener#errorStreamAltered(org.jppf.process.event.ProcessWrapperEvent)
	 */
	public void errorStreamAltered(ProcessWrapperEvent event)
	{
		String txt = "[err] " + event.getContent() + "\n";
		area.append(txt);
	}

	/**
	 * Notification that the process has written to its error stream.
	 * @param event encapsulate the error stream's content.
	 * @see org.jppf.process.event.ProcessWrapperEventListener#outputStreamAltered(org.jppf.process.event.ProcessWrapperEvent)
	 */
	public void outputStreamAltered(ProcessWrapperEvent event)
	{
		String txt = "[out] " + event.getContent() + "\n";
		area.append(txt);
	}
}
