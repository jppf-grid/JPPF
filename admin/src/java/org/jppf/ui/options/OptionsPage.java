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

import java.util.List;

/**
 * 
 * @author Laurent Cohen
 */
public interface OptionsPage extends OptionElement
{
	/**
	 * Add an element to this options page.
	 * @param element the element to add.
	 */
	void add(OptionElement element);
	/**
	 * Remove an element from this options page.
	 * @param element the element to remove.
	 */
	void remove(OptionElement element);
	/**
	 * Determines whether this age is part of another.
	 * @return true if this page is an outermost page, false if it is embedded within another page.
	 */
	boolean isMainPage();
	/**
	 * Determine the orientation of this page's layout.
	 * @return one of {@link #HORIZONTAL} or {@link #VERTICAL}.
	 */
	int getOrientation();
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
	 * Get the options in this page.
	 * @return a list of <code>Option</code> instances.
	 */
	List<OptionElement> getChildren();
}
