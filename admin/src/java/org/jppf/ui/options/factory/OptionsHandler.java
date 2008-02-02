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
package org.jppf.ui.options.factory;

import java.util.*;
import java.util.prefs.Preferences;
import org.apache.commons.logging.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.xml.OptionsPageBuilder;

/**
 * This class handles the persistence of the dynamic UI com
 * @author Laurent Cohen
 */
public final class OptionsHandler
{
	/**
	 * Log4j logger for this class.
	 */
	private static Log log = LogFactory.getLog(OptionsHandler.class);
	/**
	 * The root of the preferences subtree in which the chart configurations are saved.
	 */
	private static Preferences PREFERENCES = Preferences.userRoot().node("jppf/AdminTool");
	/**
	 * The list of option pages managed by this handler.
	 */
	private static List<OptionElement> pageList = new Vector<OptionElement>();
	/**
	 * A mapping of option pages to their name.
	 */
	private static Map<String, OptionElement> pageMap = new Hashtable<String, OptionElement>();
	/**
	 * The page builder used to instantiate pages from XML descriptors.
	 */
	private static OptionsPageBuilder builder = new OptionsPageBuilder();

	/**
	 * Get the list of option pages managed by this handler.
	 * @return a list of <code>OptionsPage</code> instances.
	 */
	public static List<OptionElement> getPageList()
	{
		return pageList;
	}

	/**
	 * Retrieve a page from its name.
	 * @param name the name of the page to retrieve.
	 * @return an <code>OptionsPage</code> instance.
	 */
	public static synchronized OptionElement getPage(String name)
	{
		return pageMap.get(name);
	}

	/**
	 * Add a page to the list of pages managed by this handler.
	 * @param page an <code>OptionsPage</code> instance.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement addPage(OptionElement page)
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
	public static synchronized OptionElement addPageFromXml(String xmlPath)
	{
		try
		{
			return addPage(builder.buildPage(xmlPath, null));
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Add a page built from an xml document.
	 * @param xmlPath the path to the xml document.
	 * @param baseName base name for resource bundle lookup.
	 * @return the page that was added.
	 */
	public static synchronized OptionElement addPageFromURL(String xmlPath, String baseName)
	{
		try
		{
			return addPage(builder.buildPageFromURL(xmlPath, baseName));
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Save the value of all persistent options in the preferences store.
	 */
	public static void savePreferences()
	{
		try
		{
			for (OptionElement elt: pageList)
			{
				Preferences prefs = PREFERENCES.node(elt.getName());
				prefs.clear();
				savePreferences(elt, prefs);
			}
			PREFERENCES.flush();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Save the value of all persistent options in the preferences store.
	 * @param elt the root of the options subtree to save.
	 * @param prefs the preferences node in which to save the vaues.
	 */
	public static void savePreferences(OptionElement elt, Preferences prefs)
	{
		if (elt instanceof OptionsPage)
		{
			for (OptionElement child: ((OptionsPage) elt).getChildren())
				savePreferences(child, prefs);
		}
		else if (elt instanceof Option)
		{
			Option option = (Option) elt;
			if (option.isPersistent()) prefs.put(option.getName(), ""+option.getValue());
		}
	}


	/**
	 * Load the value of all persistent options in the preferences store.
	 */
	public static void loadPreferences()
	{
		for (OptionElement elt: pageList)
		{
			Preferences prefs = PREFERENCES.node(elt.getName());
			loadPreferences(elt, prefs);
		}
	}

	/**
	 * Save the value of all persistent options in the preferences store.
	 * @param elt the root of the options subtree to save.
	 * @param prefs the preferences node in which to save the vaues.
	 */
	public static void loadPreferences(OptionElement elt, Preferences prefs)
	{
		if (elt instanceof OptionsPage)
		{
			for (OptionElement child: ((OptionsPage) elt).getChildren())
				loadPreferences(child, prefs);
		}
		else if (elt instanceof AbstractOption)
		{
			AbstractOption option = (AbstractOption) elt;
			if (option.isPersistent())
			{
				Object def = option.getValue();
				String val = prefs.get(option.getName(), def == null ? null : def.toString());
				option.setValue(val);
			}
		}
	}
}
