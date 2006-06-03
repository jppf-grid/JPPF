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

import java.util.*;
import org.jppf.ui.options.event.*;

/**
 * Default abstract implementation of the <code>Option</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOption extends AbstractOptionElement implements Option
{
	/**
	 * The tooltip text displayed with the UI component.
	 */
	protected String toolTipText = null;
	/**
	 * The value of this option.
	 */
	protected Object value = null;
	/**
	 * List of listeners that are notified when the value of this option changes.
	 */
	protected List<ValueChangeListener> listeners = new ArrayList<ValueChangeListener>();

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public AbstractOption()
	{
	}

	/**
	 * Get the tooltip text displayed with the UI component.
	 * @return the tooltip as a string.
	 * @see org.jppf.ui.options.Option#getToolTipText()
	 */
	public String getToolTipText()
	{
		return toolTipText;
	}

	/**
	 * Set the tooltip text displayed with the UI component.
	 * @param toolTipText the tooltip as a string.
	 */
	public void setToolTipText(String toolTipText)
	{
		this.toolTipText = toolTipText;
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
	 * The value of this option.
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
	protected void fireValueChanged()
	{
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
}
