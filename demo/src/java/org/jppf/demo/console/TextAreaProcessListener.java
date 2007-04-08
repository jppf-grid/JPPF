/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
