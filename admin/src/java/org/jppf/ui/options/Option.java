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


/**
 * Instances of this interface represent one option in an options page.
 * @author Laurent Cohen
 */
public interface Option extends OptionElement
{
	/**
	 * The value of this option.
	 * @return the value as an <code>Object</code> instance.
	 */
	Object getValue();
	/**
	 * Determine whether the value of this option should be saved in the user preferences.
	 * @return true if the value should be saved, false otherwise.
	 */
	boolean isPersistent();
}
