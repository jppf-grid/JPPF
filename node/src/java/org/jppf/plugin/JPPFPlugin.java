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

package org.jppf.plugin;

/**
 * Interface for defining a plugin and managing its lifecycle and dependencies.
 * @author Laurent Cohen
 */
public interface JPPFPlugin
{
	/**
	 * Get the id of this plugin. The id should be unique within a single JVM.
	 * @return the id as a string.
	 */
	String getPluginId();
	/**
	 * Start this plugin and eventually the plugins it depends on.
	 */
	void startPlugin();
	/**
	 * Terminate this plugin and free the resources it uses.
	 */
	void endPlugin();
	/**
	 * Get the ids of the plugins this plugin depends on.
	 * @return an array of string ids.
	 */
	String[] getPluginDependencies();
}
