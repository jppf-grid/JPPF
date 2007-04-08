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
package org.jppf.ui.options.xml;

import java.util.*;
import javax.swing.ListSelectionModel;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.options.xml.OptionDescriptor.ItemDescriptor;

/**
 * Factory class used to build UI eleemnts from XML descriptors.
 * @author Laurent Cohen
 */
public class OptionElementFactory
{
	/**
	 * The builder using this factory.
	 */
	private OptionsPageBuilder builder = null;

	/**
	 * Initialize this factory.
	 * @param builder the builder using this factory.
	 */
	public OptionElementFactory(OptionsPageBuilder builder)
	{
		this.builder = builder;
	}

	/**
	 * Build an option page from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while building the page.
	 */
	public OptionElement buildPage(OptionDescriptor desc) throws Exception
	{
		OptionPanel page = new OptionPanel();
		page.setEventsEnabled(false);
		builder.initCommonAttributes((OptionPanel) page, desc);
		page.setMainPage(desc.getBoolean("main"));
		page.createUI();
		for (OptionDescriptor child: desc.children) page.add(builder.build(child));
		page.setEventsEnabled(true);
		return page;
	}

	/**
	 * Build a button option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildButton(OptionDescriptor desc) throws Exception
	{
		ButtonOption option = new ButtonOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setIconPath(desc.getProperty("icon"));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a label option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildLabel(OptionDescriptor desc) throws Exception
	{
		LabelOption option = new LabelOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setValue(desc.getProperty("value"));
		option.setIconPath(desc.getProperty("icon"));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a text area option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildTextArea(OptionDescriptor desc) throws Exception
	{
		TextAreaOption option = new TextAreaOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a password option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildPassword(OptionDescriptor desc) throws Exception
	{
		PasswordOption option = new PasswordOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setValue(desc.getProperty("value"));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a plain text option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildPlainText(OptionDescriptor desc) throws Exception
	{
		PlainTextOption option = new PlainTextOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setValue(desc.getProperty("value"));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a formatted number option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildFormattedNumber(OptionDescriptor desc) throws Exception
	{
		FormattedNumberOption option = new FormattedNumberOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setValue(new Double(desc.getDouble("value")));
		option.setPattern(desc.getProperty("pattern"));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a spinner number option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildSpinnerNumber(OptionDescriptor desc) throws Exception
	{
		SpinnerNumberOption option = new SpinnerNumberOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setMin(new Integer(desc.getInt("minValue")));
		option.setMax(new Integer(desc.getInt("maxValue")));
		option.setValue(new Integer(desc.getInt("value")));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a check box option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildBoolean(OptionDescriptor desc) throws Exception
	{
		BooleanOption option = new BooleanOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.setValue(new Boolean(desc.getBoolean("value")));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a combo box option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildComboBox(OptionDescriptor desc) throws Exception
	{
		ComboBoxOption option = new ComboBoxOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		List<Object> items = new ArrayList<Object>();
		for (ItemDescriptor itemDesc: desc.items) items.add(itemDesc.name);
		option.setItems(items);
		option.setValue(desc.getProperty("value"));
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a list option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildList(OptionDescriptor desc) throws Exception
	{
		ListOption option = new ListOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		List<Object> items = new ArrayList<Object>();
		for (ItemDescriptor itemDesc: desc.items) items.add(itemDesc.name);
		int selMode = "single".equals(desc.getProperty("selection"))
			? ListSelectionModel.SINGLE_SELECTION : ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
		option.setSelMode(selMode);
		option.setItems(items);
		option.setValue(new ArrayList<Object>());
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a filler option from the specified option descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildFiller(OptionDescriptor desc) throws Exception
	{
		int width = desc.getInt("width", 1);
		int height = desc.getInt("height", 1);
		FillerOption option = new FillerOption(width, height);
		return option;
	}

	/**
	 * Build a toolbar separator option from the specified option descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildToolbarSeparator(OptionDescriptor desc) throws Exception
	{
		int width = desc.getInt("width", 1);
		int height = desc.getInt("height", 1);
		ToolbarSeparatorOption option = new ToolbarSeparatorOption(width, height);
		return option;
	}

	/**
	 * Build a file chooser option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildFileChooser(OptionDescriptor desc) throws Exception
	{
		FileChooserOption option = new FileChooserOption();
		builder.initCommonOptionAttributes(option, desc);
		int dlgType = "open".equals(desc.getProperty("type")) ? FileChooserOption.OPEN : FileChooserOption.SAVE;
		option.setDialogType(dlgType);
		option.setExtensions(desc.getProperty("extensions"));
		option.setValue(desc.getProperty("value"));
		option.setIconPath(desc.getProperty("icon"));
		option.createUI();
		return option;
	}

	/**
	 * Build a split pane option from the specified option descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public OptionElement buildSplitPane(OptionDescriptor desc) throws Exception
	{
		SplitPaneOption option = new SplitPaneOption();
		builder.initCommonAttributes(option, desc);
		option.setDividerWidth(desc.getInt("dividerWidth", 4));
		option.setResizeWeight(desc.getDouble("resizeWeight", 0.5d));
		option.createUI();
		for (OptionDescriptor child: desc.children) option.add(builder.build(child));
		return option;
	}

	/**
	 * Build an XML editor option from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildXMLEditor(OptionDescriptor desc) throws Exception
	{
		XMLEditorOption option = new XMLEditorOption();
		option.setEventsEnabled(false);
		builder.initCommonOptionAttributes(option, desc);
		option.createUI();
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a toolbar option from the specified option descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public OptionElement buildToolbar(OptionDescriptor desc) throws Exception
	{
		ToolbarOption option = new ToolbarOption();
		option.setEventsEnabled(false);
		builder.initCommonAttributes(option, desc);
		option.createUI();
		for (OptionDescriptor child: desc.children) option.add(builder.build(child));
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a toolbar option from the specified option descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public OptionElement buildTabbedPane(OptionDescriptor desc) throws Exception
	{
		TabbedPaneOption option = new TabbedPaneOption();
		option.setEventsEnabled(false);
		builder.initCommonAttributes(option, desc);
		option.setMainPage(desc.getBoolean("main"));
		option.createUI();
		for (OptionDescriptor child: desc.children) option.add(builder.build(child));
		option.setEventsEnabled(true);
		return option;
	}

	/**
	 * Build a toolbar option from the specified option descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public OptionElement loadImport(OptionDescriptor desc) throws Exception
	{
		OptionsPageBuilder builder = new OptionsPageBuilder(true);
		OptionElement elt = null;
		if ("url".equalsIgnoreCase(desc.getProperty("source")))
			elt = builder.buildPageFromURL(desc.getProperty("location"), builder.getBaseName());
		else elt = builder.buildPage(desc.getProperty("location"), null);
		OptionsHandler.addPage(elt);
		return elt;
	}
}
