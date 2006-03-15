/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring.charts.config;

import java.util.*;
import javax.swing.JPanel;

/**
 * 
 * @author Laurent Cohen
 */
public class TabConfiguration
{
	/**
	 * The name of the tab.
	 */
	String name = null;
	/**
	 * The panel that contains the configured charts.
	 */
	public JPanel panel = null;
	/**
	 * The list of chart configurations contained in this tab.
	 */
	public List<ChartConfiguration> configs = new ArrayList<ChartConfiguration>();
	/**
	 * The position of this tab in the list of tabs.
	 */
	public int position = -1;

	/**
	 * Create a tab configuration with uninitialized parameters.
	 */
	public TabConfiguration()
	{
	}

	/**
	 * Create a tab configuration with a specified name and position.
	 * @param name the name of the tab to create.
	 * @param position the position of the tab in the list of tabs.
	 */
	public TabConfiguration(String name, int position)
	{
		this.name = name;
		this.position = position;
	}

	/**
	 * Get a string representation of this TabConfiguration
	 * @return a string with the tab name.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name == null ? "unnamed tab" : name;
	}
}
