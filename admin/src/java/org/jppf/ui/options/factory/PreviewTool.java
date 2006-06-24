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

import java.util.*;
import javax.swing.*;
import org.jppf.ui.monitoring.JPPFTheme;
import org.jppf.ui.options.*;
import org.jppf.ui.options.xml.OptionsPageBuilder;
import org.jppf.ui.utils.GuiUtils;
import org.jvnet.substance.SubstanceLookAndFeel;
import org.jvnet.substance.tabbed.DefaultTabPreviewPainter;
import org.jvnet.substance.watermark.SubstanceNullWatermark;

/**
 * This class implements a tool that gives the user a preview of an XML-defined page.
 * @author Laurent Cohen
 */
public class PreviewTool extends AbstractActionsHolder
{
	/**
	 * The top-level frame of this tool.
	 */
	private static JFrame topFrame = null;

	/**
	 * Initialize the mapping of option name to the method to invoke when the option's value changes.
	 * @see org.jppf.ui.options.factory.AbstractActionsHolder#initializeMethodMap()
	 */
	protected void initializeMethodMap()
	{
		addMapping("Preview", "previewPage");
		addMapping("SelectFile", "previewPage");
	}

	/**
	 * Preview an XML-based page.
	 */
	public void previewPage()
	{
		try
		{
			Option file = (Option) option.findFirstWithName("/SelectFile");
			String s = (String) file.getValue();
			OptionsPageBuilder builder = new OptionsPageBuilder();
			OptionsPage page = builder.buildPage(s);
			OptionsPage pp = (OptionsPage) option.findFirstWithName("/PreviewPanel");
			List<OptionElement> list = new ArrayList<OptionElement>(pp.getChildren());
			for (OptionElement elt: list) pp.remove(elt);
			pp.add(page);
			pp.add(new FillerOption(1, 1));
			pp.getUIComponent().updateUI();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Entry point for this tool.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			UIManager.put(SubstanceLookAndFeel.ENABLE_INVERTED_THEMES, Boolean.TRUE);
			UIManager.put(SubstanceLookAndFeel.TABBED_PANE_PREVIEW_PAINTER, new DefaultTabPreviewPainter());
			JFrame.setDefaultLookAndFeelDecorated(true);
			UIManager.setLookAndFeel(new SubstanceLookAndFeel());
			SubstanceLookAndFeel.setCurrentWatermark(new SubstanceNullWatermark());
			SubstanceLookAndFeel.setCurrentTheme(new JPPFTheme());
			topFrame = new JFrame("JPPF XML-based UI preview tool");
			ImageIcon icon = GuiUtils.loadIcon("/org/jppf/ui/resources/logo-32x32.gif");
			topFrame.setIconImage(icon.getImage());
			topFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			topFrame.setSize(500, 400);
			OptionsPageBuilder builder = new OptionsPageBuilder();
			OptionsPage page = builder.buildPage("org/jppf/ui/options/xml/PreviewTool.xml");
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
