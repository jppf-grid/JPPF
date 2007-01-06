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
import org.jvnet.substance.color.ColorScheme;

/**
 * Implementation of the JPPF color scheme.
 * @author Laurent Cohen
 */
public class ColorSchemeData implements ColorScheme
{
	/**
	 * The foreground color for this theme.
	 */
	private Color foregroundColor = null;
	/**
	 * The ultra light color for this theme.
	 */
	private Color ultraLightColor = null;
	/**
	 * The extra light color for this theme.
	 */
	private Color extraLightColor = null;
	/**
	 * The light color for this theme.
	 */
	private Color lightColor = null;
	/**
	 * The mid color for this theme.
	 */
	private Color midColor = null;
	/**
	 * The dark color for this theme.
	 */
	private Color darkColor = null;
	/**
	 * The ultra dark color for this theme.
	 */
	private Color ultraDarkColor = null;

	/**
	 * Default constructor.
	 */
	public ColorSchemeData()
	{
	}

	/**
	 * Initialize this color scheme data object from an exisiting color scheme.
	 * @param scheme the color scheme to initiialize from.
	 */
	public ColorSchemeData(ColorScheme scheme)
	{
		foregroundColor = scheme.getForegroundColor();
		ultraLightColor = scheme.getUltraLightColor();
		extraLightColor = scheme.getExtraLightColor();
		lightColor = scheme.getLightColor();
		midColor = scheme.getMidColor();
		darkColor = scheme.getDarkColor();
		ultraDarkColor = scheme.getUltraDarkColor();
	}

	/**
	 * Get the foreground color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getForegroundColor()
	 */
	public Color getForegroundColor()
	{
		return foregroundColor;
	}

	/**
	 * Set the foreground color for this theme.
	 * @param foregroundColor a <code>Color</code> instance. 
	 */
	public void setForegroundColor(Color foregroundColor)
	{
		this.foregroundColor = foregroundColor;
	}

	/**
	 * Get the ultra light color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getUltraLightColor()
	 */
	public Color getUltraLightColor()
	{
		return ultraLightColor;
	}

	/**
	 * Set the ultra light color for this theme.
	 * @param ultraLightColor a <code>Color</code> instance. 
	 */
	public void setUltraLightColor(Color ultraLightColor)
	{
		this.ultraLightColor = ultraLightColor;
	}
	/**
	 * Get the extra light color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getExtraLightColor()
	 */
	public Color getExtraLightColor()
	{
		return new Color(64, 64, 160);
	}

	/**
	 * Set the extra light color for this theme.
	 * @param extraLightColor a <code>Color</code> instance. 
	 */
	public void setExtraLightColor(Color extraLightColor)
	{
		this.extraLightColor = extraLightColor;
	}

	/**
	 * Get the light color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getLightColor()
	 */
	public Color getLightColor()
	{
		return lightColor;
	}

	/**
	 * Set the light color for this theme.
	 * @param lightColor a <code>Color</code> instance. 
	 */
	public void setLightColor(Color lightColor)
	{
		this.lightColor = lightColor;
	}

	/**
	 * Get the mid color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getMidColor()
	 */
	public Color getMidColor()
	{
		return midColor;
	}

	/**
	 * Set the mid color for this theme.
	 * @param midColor a <code>Color</code> instance.
	 */
	public void setMidColor(Color midColor)
	{
		this.midColor = midColor;
	}

	/**
	 * Get the dark color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getDarkColor()
	 */
	public Color getDarkColor()
	{
		return darkColor;
	}

	/**
	 * Set the dark color for this theme.
	 * @param darkColor a <code>Color</code> instance.
	 */
	public void setDarkColor(Color darkColor)
	{
		this.darkColor = darkColor;
	}

	/**
	 * Get the ultra dark color for this theme.
	 * @return a <code>Color</code> instance.
	 * @see org.jvnet.substance.color.ColorScheme#getUltraDarkColor()
	 */
	public Color getUltraDarkColor()
	{
		return ultraDarkColor;
	}

	/**
	 * Set the ultra dark color for this theme.
	 * @param ultraDarkColor a <code>Color</code> instance. 
	 */
	public void setUltraDarkColor(Color ultraDarkColor)
	{
		this.ultraDarkColor = ultraDarkColor;
	}
}
