/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.ui.options.event;

import java.awt.event.*;
import java.util.List;
import java.util.prefs.*;

import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.options.xml.OptionsPageBuilder;

/**
 * This class performs cleanup and preferences stroign actions when the admin console is closed.
 * @author Laurent Cohen
 */
public class WindowClosingListener extends WindowAdapter
{
	/**
	 * Process the closing of the main frame.
	 * @param event the event we're interested in.
	 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
	 */
	@Override
	public void windowClosing(final WindowEvent event)
	{
		Preferences pref = OptionsHandler.getPreferences();
		List<OptionElement> list = OptionsHandler.getPageList();
		if (!list.isEmpty())
		{
			OptionElement elt = list.get(0);
			OptionsPageBuilder builder = new OptionsPageBuilder();
			builder.triggerFinalEvents(elt);
		}

		try
		{
			pref.flush();
		}
		catch(BackingStoreException e)
		{
		}

		System.exit(0);
	}
}
