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

package org.jppf.utils;

import java.io.File;
import java.util.ResourceBundle;

import org.apache.commons.logging.*;

/**
 * Utility methods to localize messages in the JPPF components. 
 * @author Laurent Cohen
 */
public final class LocalizationUtils
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(LocalizationUtils.class);

	/**
	 * Get a localized property value.
	 * @param baseName the base name to use, in combination with the default locale,
	 * to lookup the appropriate resource bundle.
	 * @param key the key for the localized value to lookup.
	 * @return the name localized through the default locale information, or the key itself if
	 * it could not be localized.
	 * @see java.util.ResourceBundle
	 */
	public static String getLocalized(String baseName, String key)
	{
		return getLocalized(baseName, key, key);
	}

	/**
	 * Get a localized property value.
	 * @param baseName the base name to use, in combination with the default locale,
	 * to lookup the appropriate resource bundle.
	 * @param key the key for the localized value to lookup.
	 * @param def the default value to return if no localized string could be found.
	 * @return the name localized through the default locale information, or the key itself if
	 * it could not be localized.
	 * @see java.util.ResourceBundle
	 */
	public static String getLocalized(String baseName, String key, String def)
	{
		if (baseName == null) return def;
		ResourceBundle bundle = null;
		try
		{
			bundle = ResourceBundle.getBundle(baseName);
		}
		catch (Exception e)
		{
			log.debug("Could not find resource bundle \""+baseName+"\"");
			if (log.isDebugEnabled()) log.debug(e.getMessage(), e);
			return def;
		}
		String result = null;
		try
		{
			result = bundle.getString(key);
		}
		catch (Exception e)
		{
			log.debug("Could not find key \""+key+"\" in resource bundle \""+baseName+"\"");
			if (log.isDebugEnabled()) log.debug(e.getMessage(), e);
		}
		return result == null ? def : result;
	}

	/**
	 * Compute a localisation base path from a base folder and a file name.
	 * @param base the base folder path as a string.
	 * @param filename the filename from which to get the resource bundle name.
	 * @return the complete path to a resource bundle.
	 */
	public static String getLocalisationBase(String base, String filename)
	{
		String result = null;
		try
		{
			File file = new File(filename);
			result = file.getName();
			int idx = result.lastIndexOf(".");
			if (idx >= 0) result = result.substring(0, idx);
			result = base + "/" + result;
		}
		catch (Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return result;
	}
}
