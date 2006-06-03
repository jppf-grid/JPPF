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
package org.jppf.ui.options.xml;

import java.util.*;
import org.jppf.utils.TypedProperties;

/**
 * Instances of this class are used to represented an XML document describing an options page.
 * @author Laurent Cohen
 */
public class OptionDescriptor extends TypedProperties
{
	/**
	 * Type of option component.
	 * The possible values are:
	 * <ul>
	 * <li>&quot;page&quot; for an options page</li>
	 * <li>&quot;FormattedNumber&quot; for a formatted number text field</li>
	 * <li>&quot;SpinnerNumber&quot; for numbers edited with a spinner control</li>
	 * <li>&quot;TextArea&quot; for a text area option</li>
	 * <li>&quot;Boolean&quot; for a checkbox option</li>
	 * <li>&quot;ComboBox&quot; for dropdown lists</li>
	 * <li>&quot;PlainText&quot; for a simple, sligle-line text field</li>
	 * <li>&quot;Button&quot; for a button</li>
	 * <li>&quot;Password&quot; for a password field</li>
	 * </ul>
	 */
	public String type = null;
	/**
	 * Name of the option element.
	 */
	public String name = null;
	/**
	 * Children of this option element.
	 */
	public List<OptionDescriptor> children = new ArrayList<OptionDescriptor>();
	/**
	 * Listeners of this option element.
	 */
	public List<ListenerDescriptor> listeners = new ArrayList<ListenerDescriptor>();
	/**
	 * Items used in list boxes or combo boxes.
	 */
	public List<ItemDescriptor> items = new ArrayList<ItemDescriptor>();

	/**
	 * Descriptor for listeners set on option elements.
	 */
	public static class ListenerDescriptor
	{
		/**
		 * Type of listener.
		 * The possible values are:
		 * <ul>
		 * <li>&quot;action&quot; for an action listener set on a button</li>
		 * <li>&quot;value&quot; for a <code>ValueChangeListener</code></li>
		 * </ul>
		 */
		public String type = null;
		/**
		 * Name of the listener class to instantiate to set the listener on an option element.
		 */
		public String className = null;
	}

	/**
	 * Descriptor for listeners set on option elements.
	 */
	public static class ItemDescriptor
	{
		/**
		 * The name of this item.
		 */
		public String name = null;
		/**
		 * Name of the listener class to instantiate to set the listener on an option element.
		 */
		public String selected = null;
	}
}
