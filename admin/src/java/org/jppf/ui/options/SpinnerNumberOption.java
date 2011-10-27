/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
	protected Integer min = Integer.valueOf(0);
	/**
	 * Maximum value for the spinner control.
	 */
	protected Integer max = Integer.valueOf(0);

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
	public SpinnerNumberOption(final String name, final String label, final String tooltip, final Integer value, final Integer min, final Integer max)
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
	@Override
	public void createUI()
	{
		SpinnerNumberModel model =
			new SpinnerNumberModel(((Integer) value).intValue(), min.intValue(), max.intValue(), 1);
		spinner = new JSpinner(model);
		JSpinner.DefaultEditor editor = (JSpinner.DefaultEditor) spinner.getEditor();
		if (editor.getTextField().getColumns() < 5) editor.getTextField().setColumns(5);
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
	@Override
	public Object getValue()
	{
		value = spinner.getModel().getValue();
		return value;
	}

	/**
	 * Set the current value for this option.
	 * @param value a <code>Number</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(final Object value)
	{
		if (value instanceof String)
		{
			try
			{
				this.value = Integer.valueOf((String) value);
			}
			catch(NumberFormatException e)
			{
				this.value = Integer.valueOf(min);
			}
		}
		else if (!(value instanceof Number)) this.value = Integer.valueOf(min);
		else this.value = Integer.valueOf(((Number) value).intValue());
		if (spinner != null)
		{
			spinner.getModel().setValue(this.value);
		}
	}

	/**
	 * Propagate the selection changes in the underlying combo box to the listeners to this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	@Override
	protected void setupValueChangeNotifications()
	{
		SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
		model.addChangeListener(new ChangeListener()
		{
			@Override
			public void stateChanged(final ChangeEvent e)
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
	@Override
	public void setEnabled(final boolean enabled)
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
	public void setMax(final Integer max)
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
	public void setMin(final Integer min)
	{
		this.min = min;
	}
}
