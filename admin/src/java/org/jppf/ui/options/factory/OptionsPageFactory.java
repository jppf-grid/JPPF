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
package org.jppf.ui.options.factory;

import java.util.*;
import org.jppf.ui.options.OptionsPage;

/**
 * Interface for all factories that define an options page in the UI.
 * @author Laurent Cohen
 */
public class OptionsPageFactory
{
	/**
	 * Mapping of root options page to their name.
	 */
	private static Map<String, OptionsPage> pageMap = new HashMap<String, OptionsPage>();
	/**
	 * Generate the options page witht he specified name.
	 * @param name the name of the page to generate.
	 * @return an OptionsPage instance.
	 */
	public static OptionsPage create(String name)
	{
		OptionsPage page = null;
		if (page != null) pageMap.put(page.getName(), page);
		return page;
	}
}
