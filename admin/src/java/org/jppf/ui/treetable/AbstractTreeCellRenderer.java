/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.ui.treetable;

import java.awt.*;

import javax.swing.tree.DefaultTreeCellRenderer;

/**
 * Renderer used to render the tree nodes in the node data panel.
 * @author Laurent Cohen
 */
public abstract class AbstractTreeCellRenderer extends DefaultTreeCellRenderer
{
	/**
	 * Path to the location of the icon files.
	 */
	protected static final String RESOURCES = "/org/jppf/ui/resources/";
	/**
	 * Path to the icon used for a driver.
	 */
	protected static final String DRIVER_ICON = RESOURCES + "mainframe.gif";
	/**
	 * Path to the icon used for an inactive driver connection.
	 */
	protected static final String DRIVER_INACTIVE_ICON = RESOURCES + "mainframe_inactive.gif";
	/**
	 * Path to the icon used for a node.
	 */
	protected static final String NODE_ICON = RESOURCES + "buggi_server.gif";
	/**
	 * Path to the icon used for a job.
	 */
	protected static final String JOB_ICON = RESOURCES + "rack.gif";
	/**
	 * Highlighting color for active driver connections.
	 */
	protected static final Color ACTIVE_COLOR = new Color(144, 213, 149);
	/**
	 * Highlighting color for non-selected inactive driver connections.
	 */
	protected static final Color INACTIVE_COLOR = new Color(244, 99, 107);
	/**
	 * Highlighting color for selected inactive driver connections.
	 */
	protected static final Color INACTIVE_SELECTION_COLOR = new Color(214, 127, 255);
	/**
	 * Highlighting color for non-selected suspended jobs.
	 */
	protected static final Color SUSPENDED_COLOR = new Color(255, 216, 0);
	/**
	 * Default foreground color.
	 */
	protected static final Color DEFAULT_FOREGROUND = Color.BLACK;
	/**
	 * Default foreground color.
	 */
	protected static final Color DIMMED_FOREGROUND = Color.GRAY;
	/**
	 * Default non-selection background.
	 */
	protected Color defaultNonSelectionBackground = null;
	/**
	 * Default non-selection background.
	 */
	protected Color defaultSelectionBackground = null;
	/**
	 * The default plain font.
	 */
	protected Font plainFont = null;
	/**
	 * The default italic font.
	 */
	protected Font italicFont = null;
	/**
	 * The default bold font.
	 */
	protected Font boldFont = null;
	/**
	 * The default bold and italic font.
	 */
	protected Font boldItalicFont = null;

	/**
	 * Default constructor.
	 */
	public AbstractTreeCellRenderer()
	{
		defaultNonSelectionBackground = getBackgroundNonSelectionColor();
		defaultSelectionBackground = getBackgroundSelectionColor();
		//plainFont = new Font(Font.SANS_SERIF, 12, Font.PLAIN);
		//italicFont = new Font(Font.SANS_SERIF, 12, Font.ITALIC);
	}

	/**
	 * Get the default plain font.
	 * @param font the font to base the result on.
	 * @return a {@link Font} instance.
	 */
	public Font getPlainFont(Font font)
	{
		if (plainFont == null)
		{
			plainFont = new Font(font.getName(), Font.PLAIN, font.getSize());
		}
		return plainFont;
	}

	/**
	 * Get the default italic font.
	 * @param font the font to base the result on.
	 * @return a {@link Font} instance.
	 */
	public Font getItalicFont(Font font)
	{
		if (italicFont == null)
		{
			italicFont = new Font(font.getName(), Font.ITALIC, font.getSize());
		}
		return italicFont;
	}

	/**
	 * Get the default bold font.
	 * @param font the font to base the result on.
	 * @return a {@link Font} instance.
	 */
	public Font getBoldFont(Font font)
	{
		if (boldFont == null)
		{
			boldFont = new Font(font.getName(), Font.BOLD, font.getSize());
		}
		return boldFont;
	}

	/**
	 * Get the default bold and italic font.
	 * @param font the font to base the result on.
	 * @return a {@link Font} instance.
	 */
	public Font getBoldItalicFont(Font font)
	{
		if (boldItalicFont == null)
		{
			boldItalicFont = new Font(font.getName(), Font.BOLD|Font.ITALIC, font.getSize());
		}
		return boldItalicFont;
	}
}
