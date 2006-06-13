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
import org.apache.log4j.Logger;
import org.jppf.ui.options.OptionsPage;
import org.jppf.ui.options.xml.OptionsPageBuilder;

/**
 * 
 * @author Laurent Cohen
 */
public final class OptionsHandler
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(OptionsHandler.class);
	/**
	 * The list of option pages managed by this handler.
	 */
	private static List<OptionsPage> pageList = new Vector<OptionsPage>();
	/**
	 * A mapping of option pages to their name.
	 */
	private static Map<String, OptionsPage> pageMap = new Hashtable<String, OptionsPage>();
	/**
	 * The page builder used to instantiate pages from XML descriptors.
	 */
	private static OptionsPageBuilder builder = new OptionsPageBuilder();

	/**
	 * Get the list of option pages managed by this handler.
	 * @return a list of <code>OptionsPage</code> instances.
	 */
	public static List<OptionsPage> getPageList()
	{
		return pageList;
	}

	/**
	 * Retrieve a page from its name.
	 * @param name the name of the page to retrieve.
	 * @return an <code>OptionsPage</code> instance.
	 */
	public static synchronized OptionsPage getPage(String name)
	{
		return pageMap.get(name);
	}

	/**
	 * Add a page to the list of pages managed by this handler.
	 * @param page an <code>OptionsPage</code> instance.
	 * @return the page that was added.
	 */
	public static synchronized OptionsPage addPage(OptionsPage page)
	{
		pageList.add(page);
		pageMap.put(page.getName(), page);
		return page;
	}

	/**
	 * Remove a page from the list of pages managed by this handler.
	 * @param page an <code>OptionsPage</code> instance.
	 */
	public static synchronized void removePage(OptionsPage page)
	{
		pageList.remove(page);
		pageMap.remove(page.getName());
	}

	/**
	 * Add a page built from an xml document.
	 * @param xmlPath the path to the xml document.
	 * @return the page that was added.
	 */
	public static synchronized OptionsPage addPageFromXml(String xmlPath)
	{
		try
		{
			return addPage(builder.buildPage(xmlPath));
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}
}
