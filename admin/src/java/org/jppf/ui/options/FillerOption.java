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
package org.jppf.ui.options;

import org.jppf.ui.utils.GuiUtils;

/**
 * An option for boolean values, represented as a checkbox.
 * @author Laurent Cohen
 */
public class FillerOption extends AbstractOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public FillerOption()
	{
		UIComponent = GuiUtils.createFiller(1, 1);
	}

	/**
	 * Initialize this boolean option with the specified parameters.
	 * @param width the filler's width
	 * @param height the filler's height.
	 */
	public FillerOption(int width, int height)
	{
		UIComponent = GuiUtils.createFiller(width, height);
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
	}

	/**
	 * Get the current value for this option.
	 * @return null.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		return null;
	}

	/**
	 * Propagate the state changes of the underlying checkbox to the listeners to this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
	}

	/**
	 * This method always returns false, since buttons have no value to persist.
	 * @return false.
	 * @see org.jppf.ui.options.AbstractOption#isPersistent()
	 */
	public boolean isPersistent()
	{
		return false;
	}
}
