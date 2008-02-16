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
package org.jppf.ui.monitoring;

import java.security.*;

import javax.swing.*;

import org.apache.commons.logging.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.tabbed.DefaultTabPreviewPainter;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.theme.ThemeChangeListener;

/**
 * This class provides a graphical interface for monitoring the status and health 
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) fot the whole UI.
 * @author Laurent Cohen
 */
public class UILauncher
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(UILauncher.class);
	/**
	 * Start this UI.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			configureSecurity();
			if ((args  == null) || (args.length < 2))
				throw new Exception("Usage: UILauncher page_location location_source");
			String s = System.getProperty("swing.defaultlaf");
			if ((s == null) || SubstanceLookAndFeel.class.getName().equals(s))
			{
				UIManager.put(SubstanceLookAndFeel.ENABLE_INVERTED_THEMES, Boolean.TRUE);
				UIManager.put(LafWidget.TABBED_PANE_PREVIEW_PAINTER, new DefaultTabPreviewPainter());
				JFrame.setDefaultLookAndFeelDecorated(true);
				UIManager.setLookAndFeel(new SubstanceLookAndFeel());
				SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme());
				//SubstanceLookAndFeel.setCurrentTheme(new SubstanceAquaTheme());
				//SubstanceLookAndFeel.setCurrentWatermark(new SubstanceNullWatermark());
				SubstanceLookAndFeel.setCurrentWatermark(new JPPFTiledWatermark());
				SubstanceLookAndFeel.registerThemeChangeListener(new ThemeChangeListener()
				{
					public void themeChanged()
					{
						//SubstanceTheme th = SubstanceLookAndFeel.getTheme();
						//ColorScheme scheme = th.getColorScheme();
					}
				});
			}
			if ("url".equalsIgnoreCase(args[1])) OptionsHandler.addPageFromURL(args[0], null);
			else OptionsHandler.addPageFromXml(args[0]);
			OptionsHandler.loadPreferences();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}

	/**
	 * 
	 */
	public static void configureSecurity()
	{
		Policy p = new Policy()
		{
			public PermissionCollection getPermissions(CodeSource codesource)
			{
				return makePermissions();
			}
			public PermissionCollection getPermissions(ProtectionDomain domain)
			{
				return makePermissions();
			}
			public boolean implies(ProtectionDomain domain, Permission permission)
			{
				return true;
			}
			public void refresh(){}
		};
		Policy.setPolicy(p);
		System.setSecurityManager(new SecurityManager());
	}

	/**
	 * 
	 * @return a Permissions instance that contains AllPermission.
	 */
	private static Permissions makePermissions()
	{
		Permissions permissions = new Permissions();
		permissions.add(new AllPermission());
		return permissions;
	}
}
