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

package org.jppf.ui.utils.colorscheme;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.theme.SubstanceTheme;
import org.jvnet.substance.theme.SubstanceTheme.ThemeKind;

/**
 * 
 * @author Laurent Cohen
 */
public class ColorItemListMouseAdapter extends MouseAdapter
{
	/**
	 * The list on which to listen for double-clicks.
	 */
	private JList list = null;
	/**
	 * Mapping of items to their name.
	 */
	private Map<String, Color> colorMap = new HashMap<String, Color>();
	/**
	 * The current color scheme.
	 */
	private ColorSchemeData scheme = new ColorSchemeData();

	/**
	 * Initialize this mouse adapter with the specified JList.
	 * @param list the list on which to listen for double-clicks.
	 */
	public ColorItemListMouseAdapter(JList list)
	{
		this.list = list;
		resetItemMap();
	}

	/**
	 * Invoked when a mouse event occurs.
	 * @param e The mouse event that call this method to be called.
	 * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
	 */
	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() == 2)
		{
			int index = list.locationToIndex(e.getPoint());
			ColorItem item = (ColorItem) list.getModel().getElementAt(index);
			if (item != null) chooseColor(item);
		}
	}

	/**
	 * Choose the color for the specified color item.
	 * @param item the item for which to choose the color.
	 */
	private void chooseColor(ColorItem item)
	{
		Color c = JColorChooser.showDialog(list, "Select a color", item.color);
		if (c != null)
		{
			item.color = c;
			resetItemMap();
			SubstanceLookAndFeel.setCurrentTheme(new SubstanceTheme(scheme, "Current Theme", ThemeKind.DARK));
		}
	}

	/**
	 * Recompute the colors map.
	 */
	private void resetItemMap()
	{
		if ((list == null) || (list.getModel().getSize() <= 0)) return;
		colorMap.clear();
		for (int i=0; i<list.getModel().getSize(); i++)
		{
			ColorItem item = (ColorItem) list.getModel().getElementAt(i);
			colorMap.put(item.name, item.color);
		}
		scheme = new ColorSchemeData();
		scheme.setForegroundColor(colorMap.get("foreground"));
		scheme.setUltraLightColor(colorMap.get("ultra light"));
		scheme.setExtraLightColor(colorMap.get("extra light"));
		scheme.setLightColor(colorMap.get("light"));
		scheme.setMidColor(colorMap.get("mid"));
		scheme.setDarkColor(colorMap.get("dark"));
		scheme.setUltraDarkColor(colorMap.get("ultra dark"));
	}
}
