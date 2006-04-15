/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.ui.monitoring;

import java.awt.Color;
import org.jvnet.substance.color.ColorScheme;

/**
 * Implementation of the JPPF color scheme.
 * @author Laurent Cohen
 */
public class JPPFColorScheme implements ColorScheme
{
	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getForegroundColor()
	 */
	public Color getForegroundColor()
	{
		return new Color(0, 0, 128);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getUltraLightColor()
	 */
	public Color getUltraLightColor()
	{
		return new Color(255, 255, 255);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getExtraLightColor()
	 */
	public Color getExtraLightColor()
	{
		return new Color(255, 255, 192);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getLightColor()
	 */
	public Color getLightColor()
	{
		return new Color(255, 255, 128);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getMidColor()
	 */
	public Color getMidColor()
	{
		return new Color(128, 128, 255);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getDarkColor()
	 */
	public Color getDarkColor()
	{
		return new Color(128, 128, 255);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getUltraDarkColor()
	 */
	public Color getUltraDarkColor()
	{
		return new Color(128, 128, 255);
	}
}
