/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
package org.jppf.ui.options;

import static org.jppf.ui.utils.GuiUtils.addLayoutComp;
import java.awt.*;
import java.util.*;
import javax.swing.*;
import org.jppf.ui.options.event.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * Default abstract implementation of the <code>Option</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOption extends AbstractOptionElement implements Option
{
	/**
	 * The value of this option.
	 */
	protected Object value = null;
	/**
	 * List of listeners that are notified when the value of this option changes.
	 */
	protected java.util.List<ValueChangeListener> listeners = new ArrayList<ValueChangeListener>();
	/**
	 * Determines whether the value of this option should be saved in the user preferences.
	 */
	protected boolean persistent = false;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public AbstractOption()
	{
	}

	/**
	 * The value of this option.
	 * @return the value as an <code>Object</code> instance.
	 * @see org.jppf.ui.options.Option#getValue()
	 */
	public Object getValue()
	{
		return value;
	}

	/**
	 * Set the value of this option.
	 * @param value the value as an <code>Object</code> instance.
	 */
	public void setValue(Object value)
	{
		this.value = value;
	}

	/**
	 * Add a value change listener to this option's list of listeners.
	 * @param listener the listener to add to the list.
	 */
	public void addValueChangeListener(ValueChangeListener listener)
	{
		listeners.add(listener);
	}
	
	/**
	 * Remove a value change listener from this option's list of listeners.
	 * @param listener the listener to remove from the list.
	 */
	public void removeValueChangeListener(ValueChangeListener listener)
	{
		listeners.remove(listener);
	}

	/**
	 * Notify all registered listeners that the value of this option has changed.
	 */
	public void fireValueChanged()
	{
		if (!eventsEnabled) return;
		for (ValueChangeListener listener: listeners)
		{
			listener.valueChanged(new ValueChangeEvent(this));
		}
	}

	/**
	 * Concrete subclasses must implement this method, so they can properly forward
	 * changes in the value edited by the underlying UI component/editor.
	 */
	protected abstract void setupValueChangeNotifications();

	/**
	 * Layout 2 components according to this option's orientation.
	 * This method adds a filler (invisible) component between the 2 components,
	 * which will grow or decrease whenever the enclosing panel is resized.
	 * @param comp1 the first component to layout.
	 * @param comp2 the second component to layout.
	 * @return a <code>JPanel</code> instance, enclosing the 2 components plus the filler.
	 */
	protected JPanel layoutComponents(JComponent comp1, JComponent comp2)
	{
		return layoutComponents(comp1, comp2, this.orientation);
	}

	/**
	 * Layout 2 components according to the specified orientation.
	 * This method adds a filler (invisible) component between the 2 components,
	 * which will grow or decrease whenever the enclosing panel is resized.
	 * @param comp1 the first component to layout.
	 * @param comp2 the second component to layout.
	 * @param orientation the orientation to use to compute the layout.
	 * @return a <code>JPanel</code> instance, enclosing the 2 components plus the filler.
	 */
	protected JPanel layoutComponents(JComponent comp1, JComponent comp2, int orientation)
	{
		JPanel panel = new JPanel();
		if ((comp1 == null) && (comp2 == null)) return panel;
		if ((comp1 != null) && (comp2 != null))
		{
			GridBagLayout g = new GridBagLayout();
			panel.setLayout(g);
	    GridBagConstraints c = new GridBagConstraints();
			c.insets = insets;
			if (orientation == HORIZONTAL)
			{
				c.gridy = 0;
				c.anchor = GridBagConstraints.LINE_START;
			}
			else
			{
				c.gridx  = 0;
				c.anchor = GridBagConstraints.WEST;
				//c.anchor = GridBagConstraints.NORTHWEST;
			}
			addLayoutComp(panel, g, c, comp1);
			c.anchor = GridBagConstraints.CENTER;
			if (orientation == HORIZONTAL) c.weightx = 1.0;
			else c.weighty = 1.0;
			JComponent filler = GuiUtils.createFiller(1, 1);
			addLayoutComp(panel, g, c, filler);
			if (orientation == HORIZONTAL)
			{
				c.anchor = GridBagConstraints.LINE_END;
				c.weightx = 0.0;
			}
			else
			{
				c.anchor = GridBagConstraints.WEST;
				//c.anchor = GridBagConstraints.SOUTHWEST;
				c.weighty = 0.0;
			}
			addLayoutComp(panel, g, c, comp2);
		}
		else
		{
			int or = (orientation == HORIZONTAL) ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS;
			panel.setLayout(new BoxLayout(panel, or));
			panel.add(comp1 != null ? comp1 : comp2);
		}

		return panel;
	}

	/**
	 * Determine whether the value of this option should be saved in the user preferences.
	 * @return true if the value should be saved, false otherwise.
	 * @see org.jppf.ui.options.Option#isPersistent()
	 */
	public boolean isPersistent()
	{
		return persistent;
	}

	/**
	 * Set whether the value of this option should be saved in the user preferences.
	 * @param persistent true if the value should be saved, false otherwise.
	 */
	public void setPersistent(boolean persistent)
	{
		this.persistent = persistent;
	}
}
