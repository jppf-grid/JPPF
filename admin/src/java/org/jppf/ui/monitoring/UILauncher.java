/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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
package org.jppf.ui.monitoring;

import java.security.*;

import javax.swing.UIManager;

import org.apache.commons.logging.*;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.JPPFSplash;
import org.jppf.utils.JPPFConfiguration;

/**
 * This class provides a graphical interface for monitoring the status and health 
 * of the JPPF server.<br>
 * It also provides a few customization options, such as setting the interval between 2 server refreshes,
 * and switching the color scheme (skin) for the whole UI.
 * @author Laurent Cohen
 */
public class UILauncher
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(UILauncher.class);
	/**
	 * The permissions for the UI.
	 */
	private static Permissions permissions = makePermissions();
	/**
	 * The splash screen window.
	 */
	private static JPPFSplash splash = null;

	/**
	 * Start this UI.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			//configureSecurity();
			if ((args  == null) || (args.length < 2)) throw new Exception("Usage: UILauncher page_location location_source");
			String s = System.getProperty("swing.defaultlaf");
			//UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			//System.out.println("system L&F: " + UIManager.getSystemLookAndFeelClassName());
			String[] laf = { "com.jgoodies.looks.windows.WindowsLookAndFeel", "com.jgoodies.looks.plastic.PlasticLookAndFeel",
				"com.jgoodies.looks.plastic.Plastic3DLookAndFeel", "com.jgoodies.looks.plastic.PlasticXPLookAndFeel" };
			int n = 3;
			try
			{
				UIManager.setLookAndFeel(laf[n]);
			}
			catch(Throwable t)
			{
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			}
			boolean showSplash = JPPFConfiguration.getProperties().getBoolean("jppf.ui.splash", true);
			if (showSplash)
			{
				splash = new JPPFSplash("The management console is starting ...");
				splash.start();
			}
			OptionElement elt = null;
			if ("url".equalsIgnoreCase(args[1])) elt = OptionsHandler.addPageFromURL(args[0], null);
			else elt = OptionsHandler.addPageFromXml(args[0]);
			OptionsHandler.loadPreferences();
			OptionsHandler.getBuilder().triggerInitialEvents(elt);
			if (showSplash) splash.stop();
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
				return permissions;
			}
			public PermissionCollection getPermissions(ProtectionDomain domain)
			{
				return permissions;
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
