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

import java.awt.event.*;
import javax.swing.*;
import org.jppf.ui.monitoring.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.OptionsPage;
import org.jppf.ui.options.xml.OptionsPageBuilder;
import org.jppf.ui.utils.GuiUtils;
import org.jvnet.substance.SubstanceLookAndFeel;

/**
 * 
 * @author Laurent Cohen
 */
public class PanelsDefinition
{
	/**
	 * Entry point for testing the options framework.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			SubstanceLookAndFeel.setCurrentWatermark(new TiledImageWatermark("org/jppf/ui/resources/GridWatermark.gif"));
			SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme(new JPPFColorScheme(), "JPPF", false));
			JFrame frame = new JFrame("JPPF monitoring and administration tool");
			ImageIcon icon = GuiUtils.loadIcon("/org/jppf/ui/resources/logo-32x32.gif");
			frame.setIconImage(icon.getImage());
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			frame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					System.exit(0);
				}
			});
			OptionsPageBuilder builder = new OptionsPageBuilder();
			OptionsPage page = builder.buildPage("org/jppf/ui/options/xml/AdminPage.xml");
			frame.getContentPane().add(page.getUIComponent());
			frame.setSize(600, 600);
			frame.setVisible(true);
			StatsHandler.getInstance().startRefreshTimer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
