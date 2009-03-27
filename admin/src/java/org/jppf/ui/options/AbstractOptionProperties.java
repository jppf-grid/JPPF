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

import javax.swing.JComponent;

import org.jppf.ui.options.event.ValueChangeListener;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;

/**
 * Default abstract implementation of the <code>OptionElement</code> interface.
 * @author Laurent Cohen
 */
public abstract class AbstractOptionProperties implements OptionProperties
{
	/**
	 * The label or title displayed with the UI component.
	 */
	protected String label = null;
	/**
	 * The name of this option element.
	 */
	protected String name;
	/**
	 * The tooltip text displayed with the UI component.
	 */
	protected String toolTipText = null;
	/**
	 * The root of the option tree this option belongs to.
	 */
	protected OptionElement root = null;
	/**
	 * Get the UI component for this option element.
	 */
	protected JComponent UIComponent = null;
	/**
	 * Path to an eventual icon displayed in the button.
	 */
	protected String iconPath = null;
	/**
	 * Determines whether this page should be enclosed within a scroll pane.
	 */
	protected boolean scrollable = false;
	/**
	 * Determines whether this option has a border around it.
	 */
	protected boolean bordered = false;
	/**
	 * Scripts used by this option or its children.
	 */
	protected List<ScriptDescriptor> scripts = new ArrayList<ScriptDescriptor>();
	/**
	 * The action to fire immediately after the page is built, allowing to
	 * perform initializations before the page is displayed and used.
	 */
	protected ValueChangeListener initializer = null;
	/**
	 * Determines whether firing events is enabled or not.
	 */
	protected boolean eventsEnabled = true;
	/**
	 * Mig layout constraints for the entire layout.
	 */
	protected String layoutConstraints = null;
	/**
	 * Mig layout constraints for a component.
	 */
	protected String componentConstraints = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	protected AbstractOptionProperties()
	{
	}

	/**
	 * Create the UI components for this option.
	 */
	public abstract void createUI();

	/**
	 * Get the label displayed with the UI component.
	 * @return the label as a string.
	 * @see org.jppf.ui.options.Option#getLabel()
	 */
	public String getLabel()
	{
		return label;
	}

	/**
	 * Set the label displayed with the UI component.
	 * @param label the label as a string.
	 */
	public void setLabel(String label)
	{
		this.label = label;
	}

	/**
	 * Get the name of this option.
	 * @return the name as a string.
	 * @see org.jppf.ui.options.Option#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name of this option.
	 * @param name the name as a string.
	 */
	public void setName(String name)
	{
		this.name = name;
	}

	/**
	 * Get the UI component for this option.
	 * @return a <code>JComponent</code> instance.
	 * @see org.jppf.ui.options.Option#getUIComponent()
	 */
	public JComponent getUIComponent()
	{
		return UIComponent;
	}

	/**
	 * Set the UI component for this option.
	 * @param component a <code>JComponent</code> instance.
	 */
	public void setUIComponent(JComponent component)
	{
		UIComponent = component;
	}

	/**
	 * Get the tooltip text displayed with the UI component.
	 * @return the tooltip as a string.
	 * @see org.jppf.ui.options.OptionElement#getToolTipText()
	 */
	public String getToolTipText()
	{
		return toolTipText;
	}

	/**
	 * Set the tooltip text displayed with the UI component.
	 * @param tooltip the tooltip as a string.
	 */
	public void setToolTipText(String tooltip)
	{
		if (((tooltip == null) || "".equals(tooltip.trim()))) tooltip = null;
		else if (tooltip.indexOf("\\n") >= 0)
			tooltip = "<html>"+tooltip.replace("\\n", "<br>")+"</html>";
		this.toolTipText = tooltip;
	}

	/**
	 * Determine whether this page should be enclosed within a scroll pane.
	 * @return true if the page is to be enclosed in a scroll pabe, false otherwise.
	 * @see org.jppf.ui.options.OptionElement#isScrollable()
	 */
	public boolean isScrollable()
	{
		return scrollable;
	}

	/**
	 * Determine whether this page should be enclosed within a scroll pane.
	 * @param scrollable true if the page is to be enclosed in a scroll pane, false otherwise.
	 */
	public void setScrollable(boolean scrollable)
	{
		this.scrollable = scrollable;
	}

	/**
	 * Determine whether this page has a border around it.
	 * @return true if the page has a border, false otherwise.
	 * @see org.jppf.ui.options.OptionElement#isBordered()
	 */
	public boolean isBordered()
	{
		return bordered;
	}

	/**
	 * Determine whether this page has a border around it.
	 * @param bordered true if the page has a border, false otherwise.
	 */
	public void setBordered(boolean bordered)
	{
		this.bordered = bordered;
	}

	/**
	 * Get the scripts used by this option or its children.
	 * @return a list of <code>ScriptDescriptor</code> instances.
	 * @see org.jppf.ui.options.OptionElement#getScripts()
	 */
	public List<ScriptDescriptor> getScripts()
	{
		return scripts;
	}

	/**
	 * Get the initializer for this option.
	 * @return a <code>ValueChangeListener</code> instance. 
	 * @see org.jppf.ui.options.OptionElement#getInitializer()
	 */
	public ValueChangeListener getInitializer()
	{
		return initializer;
	}

	/**
	 * Set the initializer for this option.
	 * @param initializer a <code>ValueChangeListener</code> instance.
	 */
	public void setInitializer(ValueChangeListener initializer)
	{
		this.initializer = initializer;
	}

	/**
	 * Get the path to an eventual icon displayed in the button.
	 * @return the path as a string.
	 */
	public String getIconPath()
	{
		return iconPath;
	}

	/**
	 * Set the path to an eventual icon displayed in the button.
	 * @param iconPath the path as a string.
	 */
	public void setIconPath(String iconPath)
	{
		this.iconPath = iconPath;
	}

	/**
	 * Determine whether the events firing in this option and/or its children are enabled.
	 * @return enabled true if the events are enabled, false otherwise.
	 * @see org.jppf.ui.options.OptionElement#isEventsEnabled()
	 */
	public boolean isEventsEnabled()
	{
		return eventsEnabled;
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

	/**
	 * Get the Mig layout constraints for the entire layout.
	 * @return the constraints as a string.
	 * @see org.jppf.ui.options.OptionProperties#getLayoutConstraints()
	 */
	public String getLayoutConstraints()
	{
		return layoutConstraints;
	}

	/**
	 * Set the Mig layout constraints for the entire layout.
	 * @param layoutConstraints - the constraints as a string.
	 * @see org.jppf.ui.options.OptionProperties#setLayoutConstraints(java.lang.String)
	 */
	public void setLayoutConstraints(String layoutConstraints)
	{
		this.layoutConstraints = layoutConstraints;
	}

	/**
	 * Get the Mig layout constraints for a component.
	 * @return the constraints as a string.
	 * @see org.jppf.ui.options.OptionProperties#getComponentConstraints()
	 */
	public String getComponentConstraints()
	{
		return componentConstraints;
	}

	/**
	 * Set the Mig layout constraints for a component.
	 * @param componentConstraints - the constraints as a string.
	 * @see org.jppf.ui.options.OptionProperties#setComponentConstraints(java.lang.String)
	 */
	public void setComponentConstraints(String componentConstraints)
	{
		this.componentConstraints = componentConstraints;
	}
}
