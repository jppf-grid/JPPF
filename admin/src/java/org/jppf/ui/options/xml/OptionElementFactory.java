/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.options.xml;

import java.util.*;

import javax.swing.*;

import org.jppf.ui.monitoring.node.NodeDataPanel;
import org.jppf.ui.options.*;
import org.jppf.ui.options.xml.OptionDescriptor.ItemDescriptor;
import org.jppf.utils.JPPFConfiguration;

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
		//option.setBordered(desc.getBoolean("bordered", true));
		option.setEditable(desc.getBoolean("editable", false));
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
		option.setColumns(desc.getInt("columns", 16));
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
		option.setMin(Integer.valueOf(desc.getInt("minValue")));
		option.setMax(Integer.valueOf(desc.getInt("maxValue")));
		option.setValue(Integer.valueOf(desc.getInt("value")));
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
		option.setValue(Boolean.valueOf(desc.getBoolean("value")));
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
		String s = desc.getString("orientation", "horizontal");
		option.setOrientation("horizontal".equalsIgnoreCase(s) ? SplitPaneOption.HORIZONTAL : SplitPaneOption.VERTICAL);
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
		String source = desc.getProperty("source");
		String location = desc.getProperty("location");
		if ("url".equalsIgnoreCase(source)) elt = builder.buildPageFromURL(location, builder.getBaseName());
		else elt = builder.buildPage(location, null);
		if (JPPFConfiguration.getProperties().getBoolean("jppf.ui.debug.enabled", false)) addDebugComp(elt, source, location);
		return elt;
	}

	/**
	 * Add an invisible component from which to get a popup menu to reload the page.
	 * @param elt - the option to debug.
	 * @param source - determines whether the XML is loaded from a url or file location.
	 * @param location - where to load the xml descriptor from.
	 */
	public void addDebugComp(OptionElement elt, String source, String location)
	{
		JLabel label = new JLabel("X")
		{
			public java.awt.Color getBackground()
			{
				return java.awt.Color.red;
			}
		};
		label.setMinimumSize(new java.awt.Dimension(10, 10));
		label.setBackground(java.awt.Color.red);
		JComponent comp = elt.getUIComponent();
		if (comp instanceof JScrollPane) comp = (JComponent) ((JScrollPane) comp).getViewport().getView();
		comp.add(label, "w 10:10:10, h 10:10:10", 0);
		label.addMouseListener(new DebugMouseListener(elt, source, location));
	}

	/**
	 * Build an option with a UI component created from a Java class.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildJavaOption(OptionDescriptor desc) throws Exception
	{
		JavaOption option = new JavaOption();
		builder.initCommonOptionAttributes(option, desc);
		option.setClassName(desc.getProperty("class"));
		option.setMouseListenerClassName(desc.getProperty("mouseListenerClass"));
		option.createUI();
		return option;
	}

	/**
	 * Build a NodeDataPanel from the specified descriptor.
	 * @param desc the descriptor to get the properties from.
	 * @return an <code>Option</code> instance, or null if the option could not be build.
	 * @throws Exception if an error was raised while building the option.
	 */
	public Option buildNodeDataPanel(OptionDescriptor desc) throws Exception
	{
		NodeDataPanel option = new NodeDataPanel();
		builder.initCommonOptionAttributes(option, desc);
		return option;
	}
}
