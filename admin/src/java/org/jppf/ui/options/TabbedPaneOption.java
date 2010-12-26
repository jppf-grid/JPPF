/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import javax.swing.*;

import org.jppf.ui.utils.GuiUtils;

/**
 * This option class encapsulates a tabbed pane, as the one present in the Swing api.
 * @author Laurent Cohen
 */
public class TabbedPaneOption extends AbstractOptionElement implements OptionsPage
{
	/**
	 * The list of children of this options page.
	 */
	protected List<OptionElement> children = new ArrayList<OptionElement>();

	/**
	 * Initialize the split pane with 2 fillers as left (or top) and right (or bottom) components.
	 */
	public TabbedPaneOption()
	{
	}

	/**
	 * Initialize the panel used to display this options page.
	 */
	public void createUI()
	{
		JTabbedPane pane = new JTabbedPane();
		pane.setDoubleBuffered(true);
		if (!bordered) pane.setBorder(BorderFactory.createEmptyBorder());
		UIComponent = pane;
		//pane.setOpaque(false);
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
		children.add(element);
		JTabbedPane pane = (JTabbedPane) UIComponent;
		ImageIcon icon = null;
		if (element.getIconPath() != null) icon = GuiUtils.loadIcon(element.getIconPath());
		try
		{
			pane.addTab(element.getLabel(), icon, element.getUIComponent(), element.getToolTipText());
		}
		catch(Throwable t)
		{
			t.printStackTrace();
		}
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(this);
	}

	/**
	 * Remove an element from this options page.
	 * @param element the element to remove.
	 * @see org.jppf.ui.options.OptionsPage#remove(org.jppf.ui.options.OptionElement)
	 */
	public void remove(OptionElement element)
	{
		children.remove(element);
		UIComponent.remove(element.getUIComponent());
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(null);
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
