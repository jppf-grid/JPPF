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
package org.jppf.ui.options.xml;

import java.awt.Insets;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.swing.ListSelectionModel;

import org.apache.log4j.Logger;
import org.jppf.ui.options.*;
import org.jppf.ui.options.event.*;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.options.xml.OptionDescriptor.*;
import org.jppf.utils.*;

/**
 * Instances of this class build options pages from XML descriptors.
 * @author Laurent Cohen
 */
public class OptionsPageBuilder
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(OptionsPageBuilder.class);
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private final static String BASE_NAME = "org.jppf.ui.i18n.";
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private String baseName = null;
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private boolean eventEnabled = true;
	/**
	 * Default constructor.
	 */
	public OptionsPageBuilder()
	{
	}

	/**
	 * Initialize this page builder.
	 * @param enableEvents determines if events triggering should be performed
	 * once the page is built.
	 */
	public OptionsPageBuilder(boolean enableEvents)
	{
		this.eventEnabled = enableEvents;
	}

	/**
	 * Build an option page from the specified XML descriptor.
	 * @param content the text of the XML document to parse.
	 * @param baseName the base path where the localization resources are located.
	 * @return an <code>OptionElement</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionElement buildPageFromContent(String content, String baseName) throws Exception
	{
		this.baseName = baseName;
		OptionDescriptor desc = new OptionDescriptorParser().parse(new StringReader(content));
		if (desc == null) return null;
		OptionElement page = build(desc);
		if (eventEnabled) triggerInitialEvents(page);
		return page;
	}

	/**
	 * Build an option page from an XML descriptor specified as a URL.
	 * @param urlString the URL of the XML descriptor file.
	 * @param baseName the base path where the localization resources are located.
	 * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionElement buildPageFromURL(String urlString, String baseName) throws Exception
	{
		if (urlString == null) return null;
		URL url = null;
		try
		{
			url = new URL(urlString);
		}
		catch(MalformedURLException e)
		{
			log.error(e.getMessage(), e);
			return null;
		}
		Reader reader = new InputStreamReader(url.openStream());
		return buildPageFromContent(FileUtils.readTextFile(reader), baseName);
	}

	/**
	 * Build an option page from the specified XML descriptor.
	 * @param xmlPath the path to the XML descriptor file.
	 * @param baseName the base path where the localization resources are located.
	 * @return an <code>OptionElement</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionElement buildPage(String xmlPath, String baseName) throws Exception
	{
		if (baseName == null)
		{
			int idx = xmlPath.lastIndexOf("/");
			this.baseName = BASE_NAME + ((idx < 0) ? xmlPath : xmlPath.substring(idx + 1));
			idx = this.baseName.lastIndexOf(".xml");
			if (idx >= 0) this.baseName = this.baseName.substring(0, idx);
		}
		else this.baseName = baseName;
		OptionDescriptor desc = new OptionDescriptorParser().parse(xmlPath);
		if (desc == null) return null;
		OptionElement page = build(desc);
		if (eventEnabled) triggerInitialEvents(page);
		return page;
	}

	/**
	 * Trigger all events listeners for all options, immeidately after the page has been built.
	 * This ensures the consistence of the UI's initial state.
	 * @param elt the root element of the options on which to trigger the events.
	 */
	private void triggerInitialEvents(OptionElement elt)
	{
		if (elt == null) return;
		if (elt.getInitializer() != null)
		{
			elt.getInitializer().valueChanged(new ValueChangeEvent(elt));
		}
		if (elt instanceof OptionsPage)
		{
			for (OptionElement child: ((OptionsPage) elt).getChildren())
			{
				triggerInitialEvents(child);
			}
		}
	}

	/**
	 * Initialize the attributes common to all option elements from an option descriptor. 
	 * @param elt the element whose attributes are to be initialized.
	 * @param desc the descriptor to get the attribute values from.
	 */
	public void initCommonAttributes(AbstractOptionElement elt, OptionDescriptor desc)
	{
		elt.setName(desc.name);
		elt.setLabel(StringUtils.getLocalized(baseName, desc.name+".label", desc.getProperty("label")));
		String s = desc.getProperty("orientation", "horizontal");
		elt.setOrientation("horizontal".equalsIgnoreCase(s) ? OptionsPage.HORIZONTAL : OptionsPage.VERTICAL);
		elt.setToolTipText(StringUtils.getLocalized(baseName, desc.name+".tooltip", desc.getProperty("tooltip")));
		elt.setScrollable(desc.getBoolean("scrollable", false));
		elt.setBordered(desc.getBoolean("bordered", false));
		elt.setWidth(desc.getInt("width", -1));
		elt.setHeight(desc.getInt("height", -1));
		s = desc.getProperty("insets");
		int defMargin = 2;
		if ((s == null) || ("".equals(s.trim())))
			elt.setInsets(new Insets(defMargin, defMargin, defMargin, defMargin));
		else
		{
			String[] sVals = s.split(",");
			if (sVals.length != 4) elt.setInsets(new Insets(defMargin, defMargin, defMargin, defMargin));
			else
			{
				int[] vals = new int[4];
				for (int i=0; i<4; i++)
				{
					try
					{
						vals[i] = Integer.parseInt(sVals[i].trim());
					}
					catch(NumberFormatException e)
					{
						vals[i] = defMargin;
					}
				}
				elt.setInsets(new Insets(vals[0], vals[1], vals[2], vals[3]));
			}
		}
		for (ScriptDescriptor script: desc.scripts) elt.getScripts().add(script);
		if (desc.initializer != null) elt.setInitializer(createListener(desc.initializer));
	}

	/**
	 * Initialize the attributes common to all options from an option descriptor. 
	 * @param option the option whose attributes are to be initialized.
	 * @param desc the descriptor to get the attribute values from.
	 */
	public void initCommonOptionAttributes(AbstractOption option, OptionDescriptor desc)
	{
		initCommonAttributes(option, desc);
		option.setPersistent(desc.getBoolean("persistent", false));
		for (ListenerDescriptor listenerDesc: desc.listeners)
		{
			ValueChangeListener listener = createListener(listenerDesc);
			if (listener != null) option.addValueChangeListener(listener);
		}
	}

	/**
	 * Create a value change listener from a listener descriptor.
	 * @param listenerDesc the listener descriptor to get the listener properties from.
	 * @return a ValueChangeListener instance.
	 */
	public ValueChangeListener createListener(ListenerDescriptor listenerDesc)
	{
		ValueChangeListener listener = null;
		try
		{
			if (listenerDesc != null)
			{
				if ("java".equals(listenerDesc.type))
				{
					Class clazz = Class.forName(listenerDesc.className);
					listener = (ValueChangeListener) clazz.newInstance();
				}
				else
				{
					ScriptDescriptor script = listenerDesc.script;
					listener = new ScriptedValueChangeListener(script.language, script.source);
				}
			}
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return listener;
	}

	/**
	 * Add all the children elements in a page.
	 * @param desc the descriptor for the page.
	 * @return an OptionElement instance.
	 * @throws Exception if an error was raised while building the page.
	 */
	public OptionElement build(OptionDescriptor desc) throws Exception
	{
		OptionElement elt = null; 
		String type = desc.type;
		if ("page".equals(type)) elt = buildPage(desc);
		else if ("SplitPane".equals(desc.type)) elt = buildSplitPane(desc);
		else if ("TabbedPane".equals(desc.type)) elt = buildTabbedPane(desc);
		else if ("Toolbar".equals(desc.type)) elt = buildToolbar(desc);
		else if ("ToolbarSeparator".equals(desc.type)) elt = buildToolbarSeparator(desc);
		else if ("Button".equals(desc.type)) elt = buildButton(desc);
		else if ("TextArea".equals(desc.type)) elt = buildTextArea(desc);
		else if ("XMLEditor".equals(desc.type)) elt = buildXMLEditor(desc);
		else if ("Password".equals(desc.type)) elt = buildPassword(desc);
		else if ("PlainText".equals(desc.type)) elt = buildPlainText(desc);
		else if ("FormattedNumber".equals(desc.type)) elt = buildFormattedNumber(desc);
		else if ("SpinnerNumber".equals(desc.type)) elt = buildSpinnerNumber(desc);
		else if ("Boolean".equals(desc.type)) elt = buildBoolean(desc);
		else if ("ComboBox".equals(desc.type)) elt = buildComboBox(desc);
		else if ("Filler".equals(desc.type)) elt = buildFiller(desc);
		else if ("List".equals(desc.type)) elt = buildList(desc);
		else if ("FileChooser".equals(desc.type)) elt = buildFileChooser(desc);
		else if ("Label".equals(desc.type)) elt = buildLabel(desc);
		else if ("import".equals(desc.type)) elt = loadImport(desc);
		return elt;
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
		initCommonAttributes((OptionPanel) page, desc);
		page.setMainPage(desc.getBoolean("main"));
		page.createUI();
		for (OptionDescriptor child: desc.children) page.add(build(child));
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
		option.setValue(new Integer(desc.getInt("value")));
		option.setMin(new Integer(desc.getInt("minValue")));
		option.setMax(new Integer(desc.getInt("maxValue")));
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
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
		initCommonOptionAttributes(option, desc);
		int dlgType = "open".equals(desc.getProperty("type"))
			? FileChooserOption.OPEN : FileChooserOption.SAVE;
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
		initCommonAttributes(option, desc);
		option.setDividerWidth(desc.getInt("dividerWidth", 4));
		option.setResizeWeight(desc.getDouble("resizeWeight", 0.5d));
		option.createUI();
		for (OptionDescriptor child: desc.children) option.add(build(child));
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
		initCommonOptionAttributes(option, desc);
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
		initCommonAttributes(option, desc);
		option.createUI();
		for (OptionDescriptor child: desc.children) option.add(build(child));
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
		initCommonAttributes(option, desc);
		option.setMainPage(desc.getBoolean("main"));
		option.createUI();
		for (OptionDescriptor child: desc.children) option.add(build(child));
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
			elt = builder.buildPageFromURL(desc.getProperty("location"), baseName);
		else elt = builder.buildPage(desc.getProperty("location"), null);
		OptionsHandler.addPage(elt);
		return elt;
	}
}
