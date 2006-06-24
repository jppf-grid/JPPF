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
	 * Determines whether firing events is enabled or not.
	 */
	protected boolean eventsEnabled = true;

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
			c.insets = new Insets(2, 2, 2, 2);
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
			else  c.weighty = 1.0;
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
	 * Enable or disable the events firing in this otpion and/or its children.
	 * @param enabled true to enable the events, false to disable them.
	 * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
	 */
	public void setEventsEnabled(boolean enabled)
	{
		eventsEnabled = enabled;
	}
}
