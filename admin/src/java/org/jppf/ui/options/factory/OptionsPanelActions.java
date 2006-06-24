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

import java.awt.Frame;
import java.util.*;
import javax.swing.SwingUtilities;
import org.jppf.ui.monitoring.JPPFTheme;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.Option;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.theme.*;

/**
 * This panel enbles the users to set the options for the administration and monitoring GUI.
 * @author Laurent Cohen
 */
public class OptionsPanelActions extends AbstractActionsHolder
{
	/**
	 * Contains a list of all available themes fully qualified class names.
	 */
	//private static Map<String, String> themeMap = createThemeMap();
	/**
	 * Contains a list of all available themes fully qualified class names.
	 */
	private static Map<String, SubstanceTheme> themeMap = createThemeInstanceMap();
	
	/**
	 * Create a map of names to corresponding themes fully qualified class names.
	 * @return a map of string keys associated to string values.
	 */
	private static Map<String, SubstanceTheme> createThemeInstanceMap()
	{
		Map<String, SubstanceTheme> map = new TreeMap<String, SubstanceTheme>();
		map.put("Aqua",  new SubstanceAquaTheme());
		map.put("BarbyPink",  new SubstanceBarbyPinkTheme());
		map.put("BottleGreen",  new SubstanceBottleGreenTheme());
		map.put("Brown",  new SubstanceBrownTheme());
		map.put("Charcoal",  new SubstanceCharcoalTheme());
		map.put("DarkViolet",  new SubstanceDarkVioletTheme());
		map.put("Ebony",  new SubstanceEbonyTheme());
		map.put("LightAqua",  new SubstanceLightAquaTheme());
		map.put("LimeGreen",  new SubstanceLimeGreenTheme());
		map.put("Olive",  new SubstanceOliveTheme());
		map.put("Orange",  new SubstanceOrangeTheme());
		map.put("Purple",  new SubstancePurpleTheme());
		map.put("Raspberry",  new SubstanceRaspberryTheme());
		map.put("Sepia",  new SubstanceSepiaTheme());
		map.put("SteelBlue",  new SubstanceSteelBlueTheme());
		map.put("SunfireRed",  new SubstanceSunfireRedTheme());
		map.put("SunGlare",  new SubstanceSunGlareTheme());
		map.put("Sunset",  new SubstanceSunsetTheme());
		map.put("Terracotta",  new SubstanceTerracottaTheme());
		map.put("JPPF", new JPPFTheme());
		return map;
	}
	
	/**
	 * Holds the current theme name.
	 */
	private static String currentColorScheme = "Aqua";
	/**
	 * Determines whether the current theme is a dark theme.
	 */
	private static boolean currentSchemeDark = false;

	/**
	 * Change the theme by specifying a new color scheme and dark theme indicator.
	 * @param colorSchemeName the name of the color scheme to apply.
	 * @param dark true if the theme to apply is dark, false otherwise.
	 */
	private static void changeTheme(String colorSchemeName, boolean dark)
	{
		currentColorScheme = colorSchemeName;
		currentSchemeDark = dark;
		//String className = themeMap.get(currentColorScheme);
		try
		{
			//ThemeInfo ti = new ThemeInfo(colorSchemeName, className, dark ? ThemeKind.DARK : ThemeKind.BRIGHT);
			//SubstanceTheme theme = SubstanceTheme.createInstance(ti);
			SubstanceTheme theme = themeMap.get(currentColorScheme);
			SubstanceLookAndFeel.setCurrentTheme(theme);
			for (Frame frame: Frame.getFrames()) SwingUtilities.updateComponentTreeUI(frame);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Initialize the mapping of an option name to the method to invoke when the option's value changes.
	 * @see org.jppf.ui.options.factory.AbstractActionsHolder#initializeMethodMap()
	 */
	protected void initializeMethodMap()
	{
		addMapping("ApplyInterval", "applyIntervalPressed");
		addMapping("Theme", "colorSchemeChanged");
		addMapping("Dark", "darkFlagChanged");
	}

	/**
	 * Action associated with the button to change the refresh interval.
	 */
	public void applyIntervalPressed()
	{
		Option field = (Option) option.findElement("../Interval");
		long interval = ((Number) field.getValue()).longValue();
		StatsHandler handler = StatsHandler.getInstance();
		if (interval != handler.getRefreshInterval())
		{
			handler.setRefreshInterval(interval);
			handler.stopRefreshTimer();
			handler.startRefreshTimer();
		}
	}

	/**
	 * Value change listener for the combo box that determines
	 * the color scheme to use in the UI.
	 */
	public void colorSchemeChanged()
	{
		String themeName = (String) option.getValue();
		if ((themeName != null) && !currentColorScheme.equals(themeName))
		{
			changeTheme(themeName, currentSchemeDark);
		}
	}

	/**
	 * Value change listener for the checkbox that controls whether
	 * to use the light or dark version of the color theme. 
	 */
	public void darkFlagChanged()
	{
		boolean isDark = (Boolean) option.getValue();
		if (isDark != currentSchemeDark)
		{
			changeTheme(currentColorScheme, isDark);
		}
	}
}
