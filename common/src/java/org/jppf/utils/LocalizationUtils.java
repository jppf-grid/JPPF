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

package org.jppf.utils;

import java.io.File;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

/**
 * 
 * @author Laurent Cohen
 */
public final class LocalizationUtils
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(LocalizationUtils.class);

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
		String result = null;
		try
		{
			ResourceBundle bundle = ResourceBundle.getBundle(baseName);
			result = bundle.getString(key);
		}
		catch (Exception e)
		{
			log.error(e.getMessage());
			if (log.isDebugEnabled()) log.debug(e);
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
			int idx = result.lastIndexOf(".xml");
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
