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

import java.io.Serializable;
import javax.swing.JComponent;
import javax.swing.tree.TreePath;


/**
 * 
 * @author Laurent Cohen
 */
public interface OptionElement extends Serializable
{
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
	 * Get the path of this element in the option tree.
	 * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances. 
	 */
	TreePath getPath();
	/**
	 * Get the path of this element in the option tree, represented as a string.
	 * The string path is a sequence of element names separted by slashes.
	 * @return a <code>TreePath</code> whose components are <code>OptionElement</code> instances. 
	 */
	String getStringPath();
	/**
	 * Find the element with the specified path in the options tree.
	 * The path can be absolute, in which case it starts with a &quote;/&quote, otherwise it
	 * is considered relative to the requesting element.
	 * @param path the path of the element to find.
	 * @return an <code>OptionElement</code> instance, or null if no element could be found with
	 * the specfied path. 
	 */
	OptionElement findElement(String path);
	/**
	 * Get the UI component for this option.
	 * @return a <code>JComponent</code> instance.
	 */
	JComponent getUIComponent();
}
