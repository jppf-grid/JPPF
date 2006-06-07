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
package org.jppf.ui.options;

import javax.swing.*;
import javax.swing.event.*;

/**
 * An option that uses a JSpinner control to edit its value.
 * @author Laurent Cohen
 */
public class SpinnerNumberOption extends AbstractOption
{
	/**
	 * The combo box used to select one among several.
	 */
	private JSpinner spinner = null;
	/**
	 * Label associated with the text field.
	 */
	protected JLabel spinnerLabel = null;
	/**
	 * Minimum value for the spinner control.
	 */
	protected Integer min = new Integer(0);
	/**
	 * Maximum value for the spinner control.
	 */
	protected Integer max = new Integer(0);

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public SpinnerNumberOption()
	{
	}

	/**
	 * Initialize this spinner option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the combobox.
	 * @param value the initial value of this component.
	 * @param min the minimum value that can be set in the spinner.
	 * @param max the maximum value that can be set in the spinner.
	 */
	public SpinnerNumberOption(String name, String label, String tooltip, Integer value, Integer min, Integer max)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		this.min = min;
		this.max = max;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		SpinnerNumberModel model =
			new SpinnerNumberModel(((Integer) value).intValue(), min.intValue(), max.intValue(), 1);
		spinner = new JSpinner(model);
		spinnerLabel = new JLabel(label);
		if (toolTipText != null)
		{
			spinner.setToolTipText(toolTipText);
			spinnerLabel.setToolTipText(toolTipText);
		}
		UIComponent = layoutComponents(spinnerLabel, spinner);
		setupValueChangeNotifications();
	}

	/**
	 * Get the current value for this option.
	 * @return a <code>Number</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#getValue()
	 */
	public Object getValue()
	{
		value = ((SpinnerNumberModel) spinner.getModel()).getValue();
		return value;
	}

	/**
	 * Set the current value for this option.
	 * @param value a <code>Number</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
	{
		this.value = value;
		if (spinner != null)
		{
			((SpinnerNumberModel) spinner.getModel()).setValue(value);
		}
	}

	/**
	 * Propagate the selection changes in the underlying combo box to the listeners to this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
		SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
		model.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				getValue();
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
		spinner.setEnabled(enabled);
		spinnerLabel.setEnabled(enabled);
	}

	/**
	 * Get the maximum value for the spinner control.
	 * @return the value as an Integer value.
	 */
	public Integer getMax()
	{
		return max;
	}

	/**
	 * Set the maximum value for the spinner control.
	 * @param max the value as an Integer value.
	 */
	public void setMax(Integer max)
	{
		this.max = max;
	}

	/**
	 * Get the minimum value for the spinner control.
	 * @return the value as an Integer value.
	 */
	public Integer getMin()
	{
		return min;
	}

	/**
	 * Set the minimum value for the spinner control.
	 * @param min the value as an Integer value.
	 */
	public void setMin(Integer min)
	{
		this.min = min;
	}
}
