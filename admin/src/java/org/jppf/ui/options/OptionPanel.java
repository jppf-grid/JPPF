/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

import java.util.*;

import javax.swing.*;
import javax.swing.border.*;

import net.miginfocom.swing.MigLayout;

/**
 * Instances of this page represent dynamic UI components representing a page (or panel) container. 
 * @author Laurent Cohen
 */
public class OptionPanel extends AbstractOptionElement implements OptionsPage
{
	/**
	 * The list of children of this options page.
	 */
	protected List<OptionElement> children = new ArrayList<OptionElement>();
	/**
	 * The panel used to display this options page.
	 */
	protected JPanel panel = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public OptionPanel()
	{
	}

	/**
	 * Initialize this option page with the specified parameters.
	 * @param name this component's name.
	 * @param label the panel's title. 
	 * @param scrollable determines whether this page should be enclosed within a scroll pane.
	 * @param bordered determines whether this page has a border around it.
	 */
	public OptionPanel(String name, String label, boolean scrollable, boolean bordered)
	{
		this.name = name;
		this.label = label;
		this.scrollable = scrollable;
		this.bordered = bordered;
		createUI();
	}

	/**
	 * Initialize this option page with the specified parameters, setting up a page without border.
	 * This constructor is used for building outermost pages.
	 * @param name this component's name.
	 * @param label the panel's title. 
	 * @param scrollable determines whether this page should be enclosed within a scroll pane.
	 */
	public OptionPanel(String name, String label, boolean scrollable)
	{
		this(name, label, scrollable, false);
	}

	/**
	 * Initialize the panel used to display this options page.
	 */
	public void createUI()
	{
		panel = new JPanel();
		if (bordered)
		{
			Border border = (label != null) 
				? border = BorderFactory.createTitledBorder(label)
				: BorderFactory.createEtchedBorder(EtchedBorder.RAISED);
			panel.setBorder(border);
		}
		else panel.setBorder(BorderFactory.createEmptyBorder());
		if (toolTipText != null) panel.setToolTipText(toolTipText);
		MigLayout mig = new MigLayout(layoutConstraints);
		panel.setLayout(mig);
		if (scrollable)
		{
			JScrollPane sp = new JScrollPane(panel);
			sp.setBorder(BorderFactory.createEmptyBorder());
			UIComponent = sp;
		}
		else UIComponent = panel;
	}

	/**
	 * Get the options in this page.
	 * @return a list of <code>Option</code> instances.
	 * @see org.jppf.ui.options.OptionsPage#getChildren()
	 */
	public List<OptionElement> getChildren()
	{
		return children;
	}

	/**
	 * Add an element to this options page. The element can be either an option, or another page.
	 * @param element the element to add.
	 * @see org.jppf.ui.options.OptionsPage#add(org.jppf.ui.options.OptionElement)
	 */
	public void add(OptionElement element)
	{
		children.add(element);
		if (element instanceof AbstractOptionElement)
		{
			((AbstractOptionElement) element).setParent(this);
		}
		panel.add(element.getUIComponent(), element.getComponentConstraints());
	}

	/**
	 * Remove an element from this options page.
	 * @param element the element to remove.
	 * @see org.jppf.ui.options.OptionsPage#remove(org.jppf.ui.options.OptionElement)
	 */
	public void remove(OptionElement element)
	{
		children.remove(element);
		if (element instanceof AbstractOption)
		{
			((AbstractOption) element).setParent(null);
		}
		panel.remove(element.getUIComponent());
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
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
}
