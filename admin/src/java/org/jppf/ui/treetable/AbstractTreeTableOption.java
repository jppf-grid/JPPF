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

import java.util.prefs.Preferences;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.ui.actions.*;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.utils.LocalizationUtils;

/**
 * Abstract implementation of a tree table-based option.
 * @author Laurent Cohen
 */
public abstract class AbstractTreeTableOption extends AbstractOption implements ActionHolder
{
	/**
	 * Base name for localization bundle lookups.
	 */
	protected String BASE = null;
	/**
	 * The tree table model associated with the tree table.
	 */
	protected transient AbstractJPPFTreeTableModel model = null;
	/**
	 * The root of the tree model.
	 */
	protected DefaultMutableTreeNode treeTableRoot = null;
	/**
	 * A tree table component displaying the driver and nodes information. 
	 */
	protected JPPFTreeTable treeTable = null;
	/**
	 * Handles all actions in toolbars or popup menus.
	 */
	protected JTreeTableActionHandler actionHandler = null;

	/**
	 * Get the object that handles all actions in toolbars or popup menus.
	 * @return a <code>JTreeTableActionHandler</code> instance.
	 */
	public JTreeTableActionHandler getActionHandler()
	{
		return actionHandler;
	}

	/**
	 * Get the tree table component displaying the driver and nodes information. 
	 * @return a <code>JPPFTreeTable</code> instance.
	 */
	public JPPFTreeTable getTreeTable()
	{
		return treeTable;
	}

	/**
	 * Not implemented.
	 * @param enabled not used.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
	}

	/**
	 * Not implemented.
	 * @param enabled not used.
	 * @see org.jppf.ui.options.OptionElement#setEventsEnabled(boolean)
	 */
	public void setEventsEnabled(boolean enabled)
	{
	}

	/**
	 * Not implemented.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Get a localized message given its unique name and the current locale.
	 * @param message - the unique name of the localized message.
	 * @return a message in the current locale, or the default locale 
	 * if the localization for the current locale is not found. 
	 */
	protected String localize(String message)
	{
		return LocalizationUtils.getLocalized(BASE, message);
	}

	/**
	 * Set the columns width based on values stored as preferences.
	 */
	public void setupTableColumns()
	{
		Preferences pref = OptionsHandler.getPreferences();
		String key = getName() + "_column_widths";
		String s = pref.get(key, null);
		if (s == null) return;
		String[] wStr = s.split("\\s");
		for (int i=0; i<Math.min(treeTable.getColumnCount(), wStr.length); i++)
		{
			int width = 60;
			try
			{
				width = Integer.valueOf(wStr[i]);
			}
			catch(NumberFormatException e)
			{
			}
			treeTable.getColumnModel().getColumn(i).setPreferredWidth(width);
		}
	}

	/**
	 * Set the columns width based on values stored as preferences.
	 */
	public void saveTableColumnsWidth()
	{
		Preferences pref = OptionsHandler.getPreferences();
		String key = getName() + "_column_widths";
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<treeTable.getColumnCount(); i++)
		{
			int width = treeTable.getColumnModel().getColumn(i).getPreferredWidth();
			if (i > 0) sb.append(' ');
			sb.append(width);
		}
		pref.put(key, sb.toString());
	}

	/**
	 * Get the root of the tree model.
	 * @return a {@link DefaultMutableTreeNode} instance.
	 */
	public DefaultMutableTreeNode getTreeTableRoot()
	{
		return treeTableRoot;
	}

	/**
	 * get the tree table model associated with the tree table.
	 * @return an {@link AbstractJPPFTreeTableModel} instance.
	 */
	public AbstractJPPFTreeTableModel getModel()
	{
		return model;
	}
}
