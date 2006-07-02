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

import java.awt.Dimension;
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
	 * Determines whether this page is an outermost page.
	 */
	protected boolean mainPage = false;

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
		if ((width > 0) && (height > 0)) pane.setPreferredSize(new Dimension(width, height));
		UIComponent = pane;
		pane.setOpaque(false);
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

	/**
	 * Determines whether this page is part of another.
	 * @return true if this page is an outermost page, false if it is embedded within another page.
	 * @see org.jppf.ui.options.OptionsPage#isMainPage()
	 */
	public boolean isMainPage()
	{
		return mainPage;
	}

	/**
	 * Set whether this page is part of another.
	 * @param mainPage true if this page is an outermost page, false if it is embedded within another page.
	 */
	public void setMainPage(boolean mainPage)
	{
		this.mainPage = mainPage;
	}
}
