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
package org.jppf.ui.options;

import java.awt.event.*;
import java.io.File;
import java.util.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.jppf.ui.utils.GuiUtils;

/**
 * This option encapsulates a file chooser control. It is composed of three components:
 * a label, a text field containing the file name, and a button used to pop up a file
 * chooser dialog.
 * @author Laurent Cohen
 */
public class FileChooserOption extends AbstractOption
{
	/**
	 * The file chooser is for opening files.
	 */
	public static final int OPEN = 1;
	/**
	 * The file chooser is for saving files.
	 */
	public static final int SAVE = 2;
	/**
	 * Default extension of the files the users can select.	Default is to allow all files.
	 */
	private static final String DEFAULT_EXTENSIONS = "*; All Files";
	/**
	 * Determines the type of file chooser dialog, either open or save.
	 */
	private int dialogType = OPEN;
	/**
	 * The button used to popup a file chooser dialog.
	 */
	private JButton button = null;
	/**
	 * The list of extensions for the files the user can select.
	 */
	private String extensions = DEFAULT_EXTENSIONS;
	/**
	 * The list of filters used to control what files can be selected.
	 */
	private List<Filter> filters = new ArrayList<Filter>();

	/**
	 * Default constructor.
	 */
	public FileChooserOption()
	{
	}

	/**
	 * Initialize this combo box option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox. 
	 * @param tooltip the tooltip associated with the combobox.
	 * @param value the initially selected value of this component.
	 * @param dialogType determines the type of file chooser dialog, either open or save.
	 */
	public FileChooserOption(String name, String label, String tooltip, String value, int dialogType)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		this.dialogType = dialogType;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 * @see org.jppf.ui.options.AbstractOptionElement#createUI()
	 */
	public void createUI()
	{
		button = new JButton();
		if ((label != null) && !"".equals(label.trim())) button.setText(label);
		if (iconPath != null)
		{
			ImageIcon icon = GuiUtils.loadIcon(iconPath);
			if (icon != null) button.setIcon(icon);
		}
		if ((toolTipText != null) && !"".equals(toolTipText.trim()))
		{
			button.setToolTipText(toolTipText);
		}
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				doChooseFile();
			}
		});
		UIComponent = button;
	}

	/**
	 * Define a selection listener that will forward changes to the chosen file name
	 * to all value change listeners that registered with this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
		if (button != null) button.setEnabled(enabled);
	}

	/**
	 * Select a file using a chooser dialog and according to this option's specifications.
	 */
	public void doChooseFile()
	{
		String val = (String) value;
		if ((val == null) || "".equals(val.trim()))
		{
			val = System.getProperty("user.dir");
			//value = val;
		}
		JFileChooser chooser = new JFileChooser(val);
		chooser.setDialogType(dialogType == OPEN ? JFileChooser.OPEN_DIALOG : JFileChooser.SAVE_DIALOG);
		for (FileFilter filter: filters) chooser.addChoosableFileFilter(filter);
		int result = -1;
		if (OPEN == dialogType) result = chooser.showOpenDialog(button);
		else result = chooser.showSaveDialog(button);
		if (result == JFileChooser.APPROVE_OPTION)
		{
			value = chooser.getSelectedFile().getAbsolutePath();
			fireValueChanged();
		}
	}

	/**
	 * Get the type of the file chooser dialog.
	 * @return the type as an int value.
	 */
	public int getDialogType()
	{
		return dialogType;
	}

	/**
	 * Set the type of the file chooser dialog.
	 * @param dialogType the type as an int value.
	 */
	public void setDialogType(int dialogType)
	{
		this.dialogType = dialogType;
	}

	/**
	 * Get the list of extensions for the files the user can select.
	 * @return the extensions as a string.
	 * @see #setExtensions(java.lang.String)
	 */
	public String getExtensions()
	{
		return extensions;
	}

	/**
	 * Set the list of extensions for the files the user can select.
	 * @param extensions the extensions as a string with the format
	 * <i>ext1</i>; <i>desc1</i> | ... | <i>extN</i>; <i>descN</i>.
	 */
	public void setExtensions(String extensions)
	{
		filters.clear();
		if ((extensions == null) || "".equals(extensions.trim()))
			this.extensions = DEFAULT_EXTENSIONS;
		else
		{
			this.extensions = extensions;
			String[] rawExt = extensions.split("\\|");
			for (String s: rawExt)
			{
				String ext = "";
				String desc = "";
				int idx = s.indexOf(";");
				if (idx < 0) ext = s.trim();
				else
				{
					ext = s.substring(0, idx).trim();
					desc = s.substring(idx + 1).trim();
				}
				filters.add(new Filter(ext, desc));
			}
		}
	}

	/**
	 * A file filter corresponding to a file extension.
	 */
	private static class Filter extends FileFilter
	{
		/**
		 * The file extension this filter accepts.
		 */
		private String ext = null;
		/**
		 * The description for this filter.
		 */
		private String desc = null;

		/**
		 * Initialize this filter with the specified extension and description.
		 * @param ext the extensions of the file to allow.
		 * @param desc the description shown in the file chooser dialog.
		 */
		public Filter(String ext, String desc)
		{
			this.ext = (ext == null) ? "*" : ext.trim();
			this.desc = (desc == null) ? "" : desc.trim();
		}

		/**
		 * Determines whther a file can be selected.
		 * @param f the file to lookup.
		 * @return true if the file is accepted, false otherwise.
		 * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File f)
		{
			if (f == null) return false;
			if (f.isDirectory()) return true;
			if ("*".equals(ext)) return true;
			String s = f.getAbsolutePath();
			int idx = s.lastIndexOf(".");
			if ((idx < 0) && "".equals(ext)) return true;
			return ext.equals(s.substring(idx + 1));
		}

		/**
		 * Get the description for this filter.
		 * @return the description as a string.
		 * @see javax.swing.filechooser.FileFilter#getDescription()
		 */
		public String getDescription()
		{
			return desc;
		}
	}
}
