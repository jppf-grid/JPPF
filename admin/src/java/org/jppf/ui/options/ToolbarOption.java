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

import java.util.*;

import javax.swing.JToolBar;

/**
 * This option class encapsulates a JToolBar.
 * @author Laurent Cohen
 */
public class ToolbarOption extends AbstractOptionElement implements OptionsPage
{
	/**
	 * The list of children of this options page.
	 */
	protected List<OptionElement> children = new ArrayList<OptionElement>();

	/**
	 * Initialize the split pane with 2 fillers as left (or top) and right (or bottom) components.
	 */
	public ToolbarOption()
	{
	}

	/**
	 * Initialize the panel used to display this options page.
	 */
	public void createUI()
	{
		JToolBar toolbar = new JToolBar(JToolBar.HORIZONTAL);
		toolbar.setFloatable(false);
		UIComponent = toolbar;
		toolbar.setOpaque(false);
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		if (UIComponent != null) UIComponent.setEnabled(enabled);
		for (OptionElement elt: children) elt.setEnabled(enabled);
	}

	/**
	 * Enable or disable the events firing in this option and/or its children.
	 * @param enabled true to enable the events, false to disable them.
	 * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
	 */
	public void setEventsEnabled(boolean enabled)
	{
		for (OptionElement elt: children) elt.setEventsEnabled(enabled);
	}

	/**
	 * Add an element to this options page. The element can be either an option, or another page.
	 * @param element the element to add.
	 * @see org.jppf.ui.options.OptionsPage#add(org.jppf.ui.options.OptionElement)
	 */
	public void add(OptionElement element)
	{
		JToolBar toolbar = (JToolBar) UIComponent;
		children.add(element);
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(this);
		toolbar.add(element.getUIComponent());
	}

	/**
	 * Remove an element from this options page.
	 * @param element the element to remove.
	 * @see org.jppf.ui.options.OptionsPage#remove(org.jppf.ui.options.OptionElement)
	 */
	public void remove(OptionElement element)
	{
		JToolBar toolbar = (JToolBar) UIComponent;
		children.remove(element);
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(null);
		toolbar.remove(element.getUIComponent());
	}

	/**
	 * Get the options in this page.
	 * @return a list of <code>Option</code> instances.
	 * @see org.jppf.ui.options.OptionsPage#getChildren()
	 */
	public List<OptionElement> getChildren()
	{
		return Collections.unmodifiableList(children);
	}
}
