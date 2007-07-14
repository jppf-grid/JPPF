/*
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
package org.jppf.ui.options;

import java.awt.event.*;
import javax.swing.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * Option implementation with a JButton as the underlying component.
 * @author Laurent Cohen
 */
public class ButtonOption extends AbstractOption
{
	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public ButtonOption()
	{
	}

	/**
	 * Initialize this text option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the checkbox.
	 */
	public ButtonOption(String name, String label, String tooltip)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		JButton button = new JButton();
		if (label != null) button.setText(label);
		if (iconPath != null)
		{
			ImageIcon icon = GuiUtils.loadIcon(iconPath);
			if (icon != null) button.setIcon(icon);
		}
		if (toolTipText != null) button.setToolTipText(toolTipText);
		UIComponent = button;
		setupValueChangeNotifications();
	}

	/**
	 * This method does nothing.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
		((JButton) UIComponent).addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fireValueChanged();
			}
		});
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		((JButton) UIComponent).setEnabled(enabled);
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
