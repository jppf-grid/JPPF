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
		return extraLightColor;
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
