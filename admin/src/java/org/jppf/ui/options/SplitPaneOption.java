/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import java.awt.Dimension;
import java.util.*;
import javax.swing.JSplitPane;
import org.apache.commons.logging.*;

/**
 * This option class encapsulates a split pane, as the one present in the Swing api.
 * @author Laurent Cohen
 */
public class SplitPaneOption extends AbstractOptionElement implements OptionsPage
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(SplitPaneOption.class);
	/**
	 * Used when nothing is set in the left (top) panel.
	 */
	protected final Option FILLER1 = new FillerOption(0, 0);
	/**
	 * Used when nothing is set in the right (bottom) panel.
	 */
	protected final Option FILLER2 = new FillerOption(0, 0);
	/**
	 * The list of children of this options page.
	 */
	protected List<OptionElement> children = new ArrayList<OptionElement>();
	/**
	 * The split pane's resize weight.
	 */
	protected double resizeWeight = 0.5d;
	/**
	 * The split pane's divider width.
	 */
	protected int dividerWidth = 4;

	/**
	 * Initialize the split pane with 2 fillers as left (or top) and right (or bottom) components.
	 */
	public SplitPaneOption()
	{
	}

	/**
	 * Initialize the panel used to display this options page.
	 */
	public void createUI()
	{
		JSplitPane pane = new JSplitPane();
		if (orientation == HORIZONTAL)
		{
			pane.setOrientation(JSplitPane.HORIZONTAL_SPLIT);
		}
		else
		{
			pane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		}
		if ((width > 0) && (height > 0)) pane.setPreferredSize(new Dimension(width, height));
		UIComponent = pane;
		children.add(FILLER1);
		children.add(FILLER2);
		pane.setLeftComponent(FILLER1.getUIComponent());
		pane.setRightComponent(FILLER2.getUIComponent());
		pane.setDividerSize(dividerWidth);
		pane.setResizeWeight(resizeWeight);
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
		JSplitPane pane = (JSplitPane) UIComponent;
		if (FILLER1 == children.get(0))
		{
			children.remove(0);
			children.add(0, element);
			pane.setLeftComponent(element.getUIComponent());
		}
		else if (FILLER2 == children.get(1))
		{
			children.remove(1);
			children.add(1, element);
			pane.setRightComponent(element.getUIComponent());
		}
		else
		{
			String msg = "["+ this.toString() + "] This split pane can't contain more than 2 elements";
			System.err.println(msg);
			log.error(msg);
			return;
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
		int idx = children.indexOf(element);
		if (idx < 0) return;
		JSplitPane pane = (JSplitPane) UIComponent;
		if (idx == 0)
		{
			children.remove(0);
			children.add(0, FILLER1);
			pane.setLeftComponent(FILLER1.getUIComponent());
		}
		else
		{
			children.remove(1);
			children.add(1, FILLER2);
			pane.setRightComponent(FILLER2.getUIComponent());
		}
		if (element instanceof AbstractOptionElement)
			((AbstractOptionElement) element).setParent(null);
	}

	/**
	 * Determines whether this page is part of another.
	 * @return true if this page is an outermost page, false if it is embedded within another page.
	 * @see org.jppf.ui.options.OptionsPage#isMainPage()
	 */
	public boolean isMainPage()
	{
		return false;
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
	 * Get the split pane's divider width.
	 * @return the divider width as an int value.
	 */
	public int getDividerWidth()
	{
		return dividerWidth;
	}

	/**
	 * Set the split pane's divider width.
	 * @param dividerWidth the divider width as an int value.
	 */
	public void setDividerWidth(int dividerWidth)
	{
		this.dividerWidth = dividerWidth;
	}

	/**
	 * Get the split pane's resize weight.
	 * @return the resize weight as a double value.
	 */
	public double getResizeWeight()
	{
		return resizeWeight;
	}

	/**
	 * Set the split pane's resize weight.
	 * @param resizeWeight the resize weight as a double value.
	 */
	public void setResizeWeight(double resizeWeight)
	{
		this.resizeWeight = resizeWeight;
	}
}
