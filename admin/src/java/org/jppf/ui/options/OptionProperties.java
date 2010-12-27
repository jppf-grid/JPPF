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

import java.io.Serializable;
import java.util.List;

import javax.swing.JComponent;

import org.jppf.ui.options.event.ValueChangeListener;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;


/**
 * Base interface for all UI components dynamically created from XML descriptors.
 * @author Laurent Cohen
 */
public interface OptionProperties extends Serializable
{
	/**
	 * Get the name of this options page.
	 * @return the name as a string.
	 */
	String getName();
	/**
	 * Get the title of this element.
	 * The title can be the title for a panel or a label associated with an option.
	 * @return the title as a string.
	 */
	String getLabel();
	/**
	 * Get the root of the option tree this option belongs to.
	 * @return a <code>OptionElement</code> instance. 
	 */
	OptionProperties getRoot();
	/**
	 * The tooltip text displayed with the UI component.
	 * @return the tooltip as a string.
	 */
	String getToolTipText();
	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 */
	void setEnabled(boolean enabled);
	/**
	 * Enable or disable this option.
	 * @param editable true to make this option editable, false otherwise.
	 */
	void setEditable(boolean editable);
	/**
	 * Determine whether the events firing in this option and/or its children are enabled.
	 * @return enabled true if the events are enabled, false otherwise.
	 */
	boolean isEventsEnabled();
	/**
	 * Enable or disable the events firing in this option and/or its children.
	 * @param enabled true to enable the events, false to disable them.
	 */
	void setEventsEnabled(boolean enabled);
	/**
	 * Determine whether this page should be enclosed within a scroll pane.
	 * @return true if the page is to be enclosed in a scroll pane, false otherwise.
	 */
	boolean isScrollable();
	/**
	 * Determine whether this page has a border around it.
	 * @return true if the page has a border, false otherwise.
	 */
	boolean isBordered();
	/**
	 * Get the UI component for this option.
	 * @return a <code>JComponent</code> instance.
	 */
	JComponent getUIComponent();
	/**
	 * Get the scripts used by this option or its children.
	 * @return a list of <code>ScriptDescriptor</code> instances.
	 */
	List<ScriptDescriptor> getScripts();
	/**
	 * Get the action to fire immediately after the page is built, allowing to
	 * perform initializations before the page is displayed and used.
	 * @return a ValueChangeListener instance.
	 */
	ValueChangeListener getInitializer();
	/**
	 * Get the action to fire immediately when the page is disposed.
	 * @return a ValueChangeListener instance.
	 */
	ValueChangeListener getFinalizer();
	/**
	 * Get the path to an eventual icon displayed in the button.
	 * @return the path as a string.
	 */
	String getIconPath();
	/**
	 * Get the Mig layout constraints for the entire layout.
	 * @return the constraints as a string.
	 */
	String getLayoutConstraints();
	/**
	 * Set the Mig layout constraints for the entire layout.
	 * @param layoutConstraints the constraints as a string.
	 */
	void setLayoutConstraints(String layoutConstraints);
	/**
	 * Get the Mig layout constraints for a component.
	 * @return the constraints as a string.
	 */
	String getComponentConstraints();
	/**
	 * Set the Mig layout constraints for a component.
	 * @param componentConstraints the constraints as a string.
	 */
	void setComponentConstraints(String componentConstraints);
}
