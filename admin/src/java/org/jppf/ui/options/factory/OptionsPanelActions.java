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
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;
import org.jppf.ui.monitoring.JPPFTheme;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.*;
import org.jppf.ui.options.event.*;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.theme.SubstanceTheme;

/**
 * This panel enbles the users to set the options for the administration and monitoring GUI.
 * @author Laurent Cohen
 */
public class OptionsPanelActions extends JPanel
{
	/**
	 * Contains a list of all available themes fully qualified class names.
	 */
	private static Map<String, String> colorSchemeMap = createThemeMap();
	
	/**
	 * Create a map of names to corresponding themes fully qualified class names.
	 * @return a map of string keys associated to string values.
	 */
	private static Map<String, String> createThemeMap()
	{
		Map<String, String> map = new TreeMap<String, String>();
		map.put("Aqua",  "org.jvnet.substance.color.AquaColorScheme");
		map.put("BarbyPink",  "org.jvnet.substance.color.BarbyPinkColorScheme");
		map.put("BottleGreen",  "org.jvnet.substance.color.BottleGreenColorScheme");
		map.put("Brown",  "org.jvnet.substance.color.BrownColorScheme");
		map.put("Charcoal",  "org.jvnet.substance.color.CharcoalColorScheme");
		map.put("DarkViolet",  "org.jvnet.substance.color.DarkVioletColorScheme");
		map.put("Ebony",  "org.jvnet.substance.color.EbonyColorScheme");
		map.put("LightAqua",  "org.jvnet.substance.color.LightAquaColorScheme");
		map.put("LimeGreen",  "org.jvnet.substance.color.LimeGreenColorScheme");
		map.put("Olive",  "org.jvnet.substance.color.OliveColorScheme");
		map.put("Orange",  "org.jvnet.substance.color.OrangeColorScheme");
		map.put("Purple",  "org.jvnet.substance.color.PurpleColorScheme");
		map.put("Raspberry",  "org.jvnet.substance.color.RaspberryColorScheme");
		map.put("Sepia",  "org.jvnet.substance.color.SepiaColorScheme");
		map.put("SteelBlue",  "org.jvnet.substance.color.SteelBlueColorScheme");
		map.put("SunfireRed",  "org.jvnet.substance.color.SunfireRedColorScheme");
		map.put("SunGlare",  "org.jvnet.substance.color.SunGlareColorScheme");
		map.put("Sunset",  "org.jvnet.substance.color.SunsetColorScheme");
		map.put("Terracotta",  "org.jvnet.substance.color.TerracottaColorScheme");
		map.put("JPPF", "org.jppf.ui.monitoring.JPPFColorScheme");
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
		String className = colorSchemeMap.get(currentColorScheme);
		try
		{
			ColorScheme scheme = (ColorScheme) Class.forName(className).newInstance();
			SubstanceTheme theme = new JPPFTheme(scheme, colorSchemeName, dark);
			SubstanceLookAndFeel.setCurrentTheme(theme);
			for (Frame frame: Frame.getFrames()) SwingUtilities.updateComponentTreeUI(frame);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Action associated with the button to change the refresh interval.
	 */
	public static class ChangeIntervalAction extends OptionAction
	{
		/**
		 * Perform the action.
		 * @param event not used.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event)
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
	}

	/**
	 * Value change listener for the combo box that determines
	 * the colr scheme to use in the UI.
	 */
	public static class ComboBoxListener implements ValueChangeListener
	{
		/**
		 * Invoked when the selection in the combo box has changed.
		 * @param event not used.
		 * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
		 */
		public void valueChanged(ValueChangeEvent event)
		{
			String themeName = (String) event.getOption().getValue();
			if ((themeName != null) && !currentColorScheme.equals(themeName))
			{
				changeTheme(themeName, currentSchemeDark);
			}
		}
	}

	/**
	 * Value change listener for the checkbox that controls whether
	 * to use the light or dark version of the col0or theme. 
	 */
	public static class CheckBoxListener implements ValueChangeListener
	{
		/**
		 * Invoked when the state of the check box has changed.
		 * @param event not used.
		 * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
		 */
		public void valueChanged(ValueChangeEvent event)
		{
			boolean isDark = (Boolean) event.getOption().getValue();
			if (isDark != currentSchemeDark)
			{
				changeTheme(currentColorScheme, isDark);
			}
		}
	}
}
