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

package org.jppf.ui.options.event;

import java.awt.*;
import java.awt.event.*;
import java.util.prefs.*;

import javax.swing.JFrame;

import org.jppf.ui.monitoring.charts.config.JPPFChartBuilder;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.treetable.AbstractTreeTableOption;

/**
 * 
 * @author Laurent Cohen
 */
public class WindowClosingListener extends WindowAdapter
{
	/**
	 * Process the closing of the main gframe.
	 * @param event - the event we're interested in.
	 * @see java.awt.event.WindowAdapter#windowClosing(java.awt.event.WindowEvent)
	 */
	public void windowClosing(WindowEvent event)
	{
		JFrame frame = (JFrame) event.getWindow();
		//frame.setVisible(false);
		OptionElement elt = OptionsHandler.getPage("JPPFAdminTool").findFirstWithName("/ChartsBuilder");
		if (elt != null)
		{
			JPPFChartBuilder builder = (JPPFChartBuilder) elt.getUIComponent();
			builder.getStorage().saveAll();
		}
		int state = frame.getExtendedState();
		boolean maximized = (state & Frame.MAXIMIZED_BOTH) > 0;
		if (maximized)
		{
			frame.setExtendedState(Frame.NORMAL);
		}
		Preferences pref = OptionsHandler.getPreferences().node("JPPFAdminTool");
		Point p = frame.getLocation();
		pref.putInt("locationx", p.x);
		pref.putInt("locationy", p.y);
		Dimension d = frame.getSize();
		pref.putInt("width", d.width);
		pref.putInt("height", d.height);
		pref.putBoolean("maximized", maximized);

		AbstractTreeTableOption opt = (AbstractTreeTableOption) OptionsHandler.getPage("JPPFAdminTool").findFirstWithName("/NodeTreeTable");
		opt.saveTableColumnsWidth();
		opt = (AbstractTreeTableOption) OptionsHandler.getPage("JPPFAdminTool").findFirstWithName("/JobTreetable");
		opt.saveTableColumnsWidth();
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
