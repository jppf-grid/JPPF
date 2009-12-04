/*
 * JPPF.
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
