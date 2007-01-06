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
package org.jppf.ui.options.factory;

import java.awt.Frame;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.monitoring.JPPFTheme;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.xml.OptionsPageBuilder;
import org.jppf.ui.utils.GuiUtils;
import org.jvnet.lafwidget.LafWidget;
import org.jvnet.lafwidget.tabbed.DefaultTabPreviewPainter;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.watermark.SubstanceNullWatermark;

/**
 * This class implements a tool that gives the user a preview of an XML-defined page.
 * @author Laurent Cohen
 */
public class JPPFGuiBuilder
{
	/**
	 * The top-level frame of this tool.
	 */
	private static JFrame topFrame = null;

	/**
	 * Entry point for this tool.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			UIManager.put(SubstanceLookAndFeel.ENABLE_INVERTED_THEMES, Boolean.TRUE);
			UIManager.put(LafWidget.TABBED_PANE_PREVIEW_PAINTER, new DefaultTabPreviewPainter());
			JFrame.setDefaultLookAndFeelDecorated(true);
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			UIManager.getDefaults().put("UJACTextAreaUI", "org.ujac.ui.editor.TextAreaUI");
			for (Frame frame: Frame.getFrames()) SwingUtilities.updateComponentTreeUI(frame);
			SubstanceLookAndFeel.setCurrentWatermark(new SubstanceNullWatermark());
			SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme());
			topFrame = new JFrame("JPPF XML-based UI building tool");
			topFrame.addWindowListener(new WindowAdapter()
			{
				public void windowClosing(WindowEvent e)
				{
					System.exit(0);
				}
			});
			ImageIcon icon = GuiUtils.loadIcon("/org/jppf/ui/resources/logo-32x32.gif");
			topFrame.setIconImage(icon.getImage());
			topFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			topFrame.setSize(500, 400);
			OptionsPageBuilder builder = new OptionsPageBuilder();
			OptionElement page = builder.buildPage("org/jppf/ui/options/xml/JPPFGuiBuilder.xml", null);
			page.getUIComponent().setDoubleBuffered(true);
			topFrame.getContentPane().add(page.getUIComponent());
			topFrame.setVisible(true);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(1);
		}
	}
}
