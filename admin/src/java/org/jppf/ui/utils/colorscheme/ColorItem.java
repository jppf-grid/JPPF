/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.ui.utils.colorscheme;

import java.awt.Color;

/**
 * 
 * @author Laurent Cohen
 */
public class ColorItem
{
	/**
	 * Display name of this item.
	 */
	public String name = null;
	/**
	 * Color to use for this item.
	 */
	public Color color = null;

	/**
	 * Initialize this item with the specified name and color.
	 * @param name the display name of this item.
	 * @param color the color to use for this item.
	 */
	public ColorItem(String name, Color color)
	{
		this.name = name;
		this.color = color;
	}

	/**
	 * Get a string representation of this item.
	 * @return this item's display name.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name == null ? "[no name]" : name;
	}

	/**
	 * Return a string representation of this item's color.
	 * @return a space-separated list of rgb values. 
	 */
	public String colorValue()
	{
		if (color == null) return "255 255 255";
		StringBuffer sb = new StringBuffer();
		sb.append(color.getRed()).append(" ").append(color.getGreen()).append(" ").append(color.getBlue());
		return sb.toString();
	}
}
