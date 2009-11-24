/*
 * Java Parallel Processing Framework.
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
		//return new Color(0, 0, 96);
		return new Color(0, 0, 60);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getUltraLightColor()
	 */
	public Color getUltraLightColor()
	{
		//return new Color(64, 64, 128);
		return new Color(109, 120, 182);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getExtraLightColor()
	 */
	public Color getExtraLightColor()
	{
		//return new Color(64, 64, 160);
		return new Color(181, 192, 222);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getLightColor()
	 */
	public Color getLightColor()
	{
		//return new Color(96, 96, 160);
		return new Color(216, 218, 237);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getMidColor()
	 */
	public Color getMidColor()
	{
		//return new Color(128, 128, 255);
		return new Color(216, 218, 237);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getDarkColor()
	 */
	public Color getDarkColor()
	{
		//return new Color(51, 0, 153);
		return new Color(181, 192, 222);
	}

	/**
	 * .
	 * @return .
	 * @see org.jvnet.substance.color.ColorScheme#getUltraDarkColor()
	 */
	public Color getUltraDarkColor()
	{
		//return new Color(0, 0, 96);
		return new Color(109, 120, 182);
	}
}
