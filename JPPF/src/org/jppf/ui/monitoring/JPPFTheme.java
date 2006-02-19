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
package org.jppf.ui.monitoring;

import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.theme.SubstanceTheme;

/**
 * JPPF Theme for Substance L&F.
 * @author Laurent Cohen
 */
public class JPPFTheme extends SubstanceTheme
{
	/**
	 * Default initialization.
	 */
	public JPPFTheme()
	{
		super(new JPPFColorScheme(), "JPPF Theme", ThemeKind.DARK);
	}

	/**
	 * Default initialization.
	 * @param scheme the color schema to apply.
	 * @param name the name of this theme.
	 * @param dark true if this them is dark, false otherwise.
	 */
	public JPPFTheme(ColorScheme scheme, String name, boolean dark)
	{
		super(scheme, name, dark ? ThemeKind.DARK : ThemeKind.BRIGHT);
	}
}
