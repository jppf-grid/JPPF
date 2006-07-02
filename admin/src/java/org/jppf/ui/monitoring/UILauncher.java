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
package org.jppf.ui.monitoring;

import javax.swing.*;
import org.apache.log4j.Logger;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.tabbed.DefaultTabPreviewPainter;
import org.jvnet.substance.watermark.SubstanceNullWatermark;

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
	 * Log4j logger for this class.
	 */
	static Logger log = Logger.getLogger(UILauncher.class);
	/**
	 * Start this UI.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			if ((args  == null) || (args.length < 2))
				throw new Exception("Usage: UILauncher page_location location_source");
			UIManager.put(SubstanceLookAndFeel.ENABLE_INVERTED_THEMES, Boolean.TRUE);
			UIManager.put(SubstanceLookAndFeel.TABBED_PANE_PREVIEW_PAINTER, new DefaultTabPreviewPainter());
			JFrame.setDefaultLookAndFeelDecorated(true);
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme());
			SubstanceLookAndFeel.setCurrentWatermark(new SubstanceNullWatermark());
			if ("url".equalsIgnoreCase(args[1]))
				OptionsHandler.addPageFromURL(args[0], null);
			else OptionsHandler.addPageFromXml(args[0]);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			log.error(e.getMessage(), e);
			System.exit(1);
		}
	}
}
