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

package org.jppf.ui.options.xml;

import java.awt.Component;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.options.*;
import org.slf4j.*;

/**
 * Mouse listener for debug use. Shows a popup menu on the top container of options loaded through an "import" tag in the XML descriptor.
 * The menu provides one option to reload the page.
 * @author laurentcohen
 */
public class DebugMouseListener extends MouseAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(DebugMouseListener.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The option to debug.
	 */
	private OptionElement option = null;
	/**
	 * Determines whether the XML is loaded from a url or file location.
	 */
	private String source = null;
	/**
	 * Where to load the xml descriptor from.
	 */
	private String location = null;

	/**
	 * 
	 * @param option - the option to debug.
	 * @param source - determines whether the XML is loaded from a url or file location.
	 * @param location - where to load the xml descriptor from.
	 */
	public DebugMouseListener(OptionElement option, String source, String location)
	{
		this.option = option;
		this.source = source;
		this.location = location;
	}
	/**
	 * Processes right-click events to display popup menus.
	 * @param event - the mouse event to process.
	 * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
	 */
	@Override
    public void mousePressed(MouseEvent event)
	{
		if (event.getButton() != MouseEvent.BUTTON3) return;
		Component comp = event.getComponent();
		int x = event.getX();
		int y = event.getY();
		
		JPopupMenu menu = new JPopupMenu();
		JMenuItem item = new JMenuItem("Reload");
		item.addActionListener(new ActionListener()
		{
			@Override
            public void actionPerformed(ActionEvent ev)
			{
				doReloadPage();
			}
		});
		menu.add(item);
		menu.show(comp, x, y);
	}

	/**
	 * Reload the page.
	 */
	private void doReloadPage()
	{
		try
		{
			OptionsPage parent = (OptionsPage) option.getParent();
			parent.remove(option);
			OptionsPageBuilder builder = new OptionsPageBuilder(true);
			OptionElement elt;
			if ("url".equalsIgnoreCase(source)) elt = builder.buildPageFromURL(location, builder.getBaseName());
			else elt = builder.buildPage(location, null);
			builder.getFactory().addDebugComp(elt, source, location);
			parent.add(elt);
			builder.triggerInitialEvents(elt);
			parent.getUIComponent().updateUI();
		}
		catch(Exception  e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
