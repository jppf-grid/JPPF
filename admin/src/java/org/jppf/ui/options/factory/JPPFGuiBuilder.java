/*
 * JPPF.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
