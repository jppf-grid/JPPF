/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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
import java.util.List;
import javax.swing.*;
import javax.swing.border.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * 
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
	 * The layout for the panel.
	 */
	protected GridBagLayout g = null;
	/**
	 * Constraints used to layout the components in this page.
	 */
	protected GridBagConstraints c = null;
	/**
	 * Filler component used to handle horizontal and vertical extensions of this page.
	 */
	protected JComponent filler = null;
	/**
	 * 
	 */
	protected GridBagConstraints fillerConstraints = null;
	/**
	 * Determines whether this page is an outermost page.
	 */
	protected boolean mainPage = false;

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
	 * @param orientation one of {@link org.jppf.ui.options.OptionsPage#HORIZONTAL ORIENTATION_HORIZONTAL} or
	 * {@link org.jppf.ui.options.OptionsPage#VERTICAL ORIENTATION_VERTICAL}.
	 * @param bordered determines whether this page has a border around it.
	 * @param mainPage determines whether this page is an outermost page.
	 */
	public OptionPanel(String name, String label, boolean scrollable, int orientation, boolean bordered, boolean mainPage)
	{
		this.name = name;
		this.label = label;
		this.scrollable = scrollable;
		this.orientation = orientation;
		this.bordered = bordered;
		this.mainPage = mainPage;
		createUI();
	}

	/**
	 * Initialize this option page with the specified parameters, setting up a page without border.
	 * This constructor is used for building outermost pages.
	 * @param name this component's name.
	 * @param label the panel's title. 
	 * @param scrollable determines whether this page should be enclosed within a scroll pane.
	 * @param orientation one of {@link org.jppf.ui.options.OptionsPage#HORIZONTAL ORIENTATION_HORIZONTAL} or
	 * {@link org.jppf.ui.options.OptionsPage#VERTICAL ORIENTATION_VERTICAL}.
	 */
	public OptionPanel(String name, String label, boolean scrollable, int orientation)
	{
		this(name, label, scrollable, orientation, false, true);
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
		if (toolTipText != null) panel.setToolTipText(toolTipText);
		g = new GridBagLayout();
    c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = insets;
		c.anchor = GridBagConstraints.LINE_START;
    if (orientation == HORIZONTAL)
    {
    	c.gridy = 0;
    	if (mainPage) c.weighty = 1.0;
    }
    else
    {
    	c.gridx = 0;
    	if (mainPage) c.weightx = 1.0;
    }
		panel.setLayout(g);
		createFiller();
		GuiUtils.addLayoutComp(panel, g, fillerConstraints, filler);
		if (scrollable)
		{
			JScrollPane sp = new JScrollPane(panel);
			sp.setBorder(BorderFactory.createEmptyBorder());
			UIComponent = sp;
		}
		else UIComponent = panel;
		
		if ((width > 0) && (height > 0))
		{
			Dimension d = new Dimension(width, height);
			UIComponent.setPreferredSize(d);
		}
	}

	/**
	 * Create a filler component used to handle horizontal and vertical
	 * extensions of the page.
	 */
	protected void createFiller()
	{
		filler = GuiUtils.createFiller(1, 1);
		fillerConstraints = new GridBagConstraints();
 		fillerConstraints.anchor = GridBagConstraints.SOUTHEAST;
		fillerConstraints.fill = GridBagConstraints.BOTH;
  	fillerConstraints.gridwidth = GridBagConstraints.REMAINDER;
  	fillerConstraints.gridheight = GridBagConstraints.REMAINDER;
  	if (mainPage)
  	{
	    if (orientation == HORIZONTAL)
	    {
	    	fillerConstraints.weightx = 1.0;
	    }
	    else
	    {
	    	fillerConstraints.weighty = 1.0;
	    }
  	}
  	else
  	{
    	fillerConstraints.weightx = 1.0;
    	fillerConstraints.weighty = 1.0;
	    if (orientation == HORIZONTAL)
	    {
	    	fillerConstraints.gridy = 1;
	    }
	    else
	    {
	    	fillerConstraints.gridx = 1;
	    }
  	}
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
		if (mainPage) panel.remove(filler);
		addLayoutComp(panel, g, c, element.getUIComponent());
		if (mainPage) GuiUtils.addLayoutComp(panel, g, fillerConstraints, filler);
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
