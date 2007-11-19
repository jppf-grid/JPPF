/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

import java.io.Serializable;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;
import org.jppf.ui.options.event.ValueChangeListener;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;


/**
 * Base interface for all UI components dynamically created from XML descriptors.
 * @author Laurent Cohen
 */
public interface OptionElement extends Serializable
{
	/**
	 * Constant defining a horizontal layout in the page.
	 */
	int HORIZONTAL = 1;
	/**
	 * Constant defining a vertical layout in the page.
	 */
	int VERTICAL = 2;
	/**
	 * Get the name of this options page.
	 * @return the name as a string.
	 */
	String getName();
	/**
	 * Get the parent page for this options page.
	 * @return an <code>OptionsPage</code> instance.
	 */
	OptionElement getParent();
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
	OptionElement getRoot();
	/**
	 * Get the path of this element in the option tree.
	 * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances. 
	 */
	TreePath getPath();
	/**
	 * Determine the orientation of this page's layout.
	 * @return one of {@link #HORIZONTAL} or {@link #VERTICAL}.
	 */
	int getOrientation();
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
	 * Get the path of this element in the option tree, represented as a string.
	 * The string path is a sequence of element names separted by slashes.
	 * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances. 
	 */
	String getStringPath();
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
	 * Find the first element with the specified name in the subtree of which
	 * this element is the root.
	 * The notion of first element relates to a depth-first search in the tree. 
	 * @param name the name of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element
	 * could be found with the specfied name.
	 */
	OptionElement findFirstWithName(String name);
	/**
	 * Find the last element with the specified name in the subtree of which
	 * this element is the root. 
	 * The notion of last element relates to a depth-first search in the tree. 
	 * @param name the name of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element
	 * could be found with the specfied name.
	 */
	OptionElement findLastWithName(String name);
	/**
	 * Find all the elements with the specified name in the subtree of which
	 * this element is the root. 
	 * @param name the name of the elements to find.
	 * @return a list of <code>OptionElement</code> instances, or null if no element
	 * could be found with the specfied name. 
	 */
	List<OptionElement> findAllWithName(String name);
	/**
	 * Find the element with the specified path in the options tree.
	 * The path can be absolute, in which case it starts with a &quot;/&quot;, otherwise it
	 * is considered relative to the requesting element.
	 * @param path the path of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element could be found with
	 * the specified path. 
	 */
	OptionElement findElement(String path);
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
	 * Get the path to an eventual icon displayed in the button.
	 * @return the path as a string.
	 */
	String getIconPath();
}
