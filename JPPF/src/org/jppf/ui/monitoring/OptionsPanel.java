/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.color.ColorScheme;
import org.jvnet.substance.theme.SubstanceTheme;

/**
 * Options chartPanel for the monitor.
 * @author Laurent Cohen
 */
public class OptionsPanel extends JPanel
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
	private String currentColorScheme = colorSchemeMap.keySet().iterator().next();
	/**
	 * Determines whether the current theme is a dark theme.
	 */
	private boolean currentSchemeDark = false;
	/**
	 * The field containing the interval value.
	 */
	private JFormattedTextField field = new JFormattedTextField();
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsFormatter = null;

	/**
	 * Default contructor.
	 * @param statsFormatter the monitoring chartPanel to which the options apply.
	 */
	public OptionsPanel(StatsHandler statsFormatter)
	{
		this.statsFormatter = statsFormatter;
		SpringLayout layout = new SpringLayout();
		setLayout(layout);
		JPanel intervalPanel = createIntervalPanel();
		JPanel themePanel = createThemePanel();
		add(intervalPanel);
		add(themePanel);
		Dimension d = new Dimension(350, 60);
		intervalPanel.setPreferredSize(d);
		themePanel.setPreferredSize(d);

		layout.putConstraint(SpringLayout.NORTH, intervalPanel, 10, SpringLayout.NORTH, this);
		layout.putConstraint(SpringLayout.NORTH, themePanel, 10, SpringLayout.SOUTH, intervalPanel);
		layout.putConstraint(SpringLayout.WEST, intervalPanel, 10, SpringLayout.WEST, this);
		layout.putConstraint(SpringLayout.WEST, themePanel, 10, SpringLayout.WEST, this);
	}
	
	/**
	 * Creates a chartPanel with a field to enter the refresh interval, and a button to apply it.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createIntervalPanel()
	{
		JPanel panel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		panel.setBorder(BorderFactory.createTitledBorder("Monitor"));
		field.setValue(new Long(statsFormatter.getRefreshInterval())); 
		field.setHorizontalAlignment(JTextField.RIGHT);
		JButton btn = new JButton("Apply");
		btn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				long interval = (Long) field.getValue();
				if (interval != statsFormatter.getRefreshInterval())
				{
					statsFormatter.setRefreshInterval(interval);
					statsFormatter.stopRefreshTimer();
					statsFormatter.startRefreshTimer();
				}
			}
		});
		btn.setPreferredSize(new Dimension(60, 20));
		field.setPreferredSize(new Dimension(100, 20));
		field.setMinimumSize(new Dimension(100, 20));
		field.setMaximumSize(new Dimension(100, 20));
		panel.add(Box.createHorizontalStrut(10));
		panel.add(new JLabel("Interval in milliseconds"));
		panel.add(Box.createHorizontalStrut(10));
		panel.add(field);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(btn);
		panel.add(Box.createHorizontalStrut(10));
		panel.add(Box.createHorizontalGlue());
		return panel;
	}

	/**
	 * Creates a chartPanel with a combo box to switch the L&F theme.
	 * @return a <code>JPanel</code> instance.
	 */
	private JPanel createThemePanel()
	{
		JPanel comboPanel = GuiUtils.createBoxPanel(BoxLayout.X_AXIS);
		comboPanel.setBorder(BorderFactory.createTitledBorder("UI"));
		comboPanel.add(new JLabel("Choose a theme :"));
		comboPanel.add(Box.createHorizontalStrut(10));
		final JComboBox combo = new JComboBox(colorSchemeMap.keySet().toArray(new String[0]));
		combo.setSelectedItem("JPPF");
		combo.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				String themeName = (String) combo.getSelectedItem();
				if ((themeName != null) && !currentColorScheme.equals(themeName))
				{
					changeTheme(themeName, currentSchemeDark);
				}
			}
		});
		comboPanel.add(combo);
		combo.setMinimumSize(new Dimension(100, 25));
		combo.setMaximumSize(new Dimension(100, 25));
		final JCheckBox box = new JCheckBox(" Dark ", currentSchemeDark);
		box.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if (box.isSelected() != currentSchemeDark)
				{
					changeTheme(currentColorScheme, box.isSelected());
				}
			}
		});
		comboPanel.add(Box.createHorizontalStrut(10));
		comboPanel.add(box);
		comboPanel.add(Box.createHorizontalGlue());
		return comboPanel;
	}

	/**
	 * Change the theme by specifying a new color scheme and dark theme indicator.
	 * @param colorSchemeName the name of the color scheme to apply.
	 * @param dark true if the theme to apply is dark, false otherwise.
	 */
	private void changeTheme(String colorSchemeName, boolean dark)
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
}
