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

import java.util.*;
import javax.swing.ListSelectionModel;
import org.jppf.ui.options.*;
import org.jppf.ui.options.event.ValueChangeListener;
import org.jppf.ui.options.xml.OptionDescriptor.*;
import org.jppf.utils.StringUtils;

/**
 * Instances of this class build options pages from XML descriptors.
 * @author Laurent Cohen
 */
public class OptionsPageBuilder
{
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private final static String BASE_NAME = "org.jppf.ui.i18n.";
	/**
	 * Base name used to localize labels and tooltips.
	 */
	private String baseName = null;

	/**
	 * Build an option page from the specified XML descriptor.
	 * @param xmlPath the path to the XML descriptor file.
	 * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while parsing the xml document or building the page.
	 */
	public OptionsPage buildPage(String xmlPath) throws Exception
	{
		int idx = xmlPath.lastIndexOf("/");
		baseName = BASE_NAME + ((idx < 0) ? xmlPath : xmlPath.substring(idx + 1));
		idx = baseName.lastIndexOf(".xml");
		if (idx >= 0) baseName = baseName.substring(0, idx);
		OptionDescriptor desc = new OptionDescriptorParser().parse(xmlPath);
		if (desc == null) return null;
		OptionsPage page = buildPage(desc);
		triggerInitialEvents(page);
		return page;
	}

	/**
	 * Trigger all events listeners for all options, immeidately after the page has been built.
	 * This ensures the consistence of the UI's initial state.
	 * @param page the root page of the options on which to trigger the events.
	 */
	private void triggerInitialEvents(OptionsPage page)
	{
		for (OptionElement child: page.getChildren())
		{
			if ((child instanceof AbstractOption) && !(child instanceof ButtonOption))
			{
				((AbstractOption) child).fireValueChanged();
			}
			else if (child instanceof OptionsPage)
			{
				triggerInitialEvents((OptionsPage) child);
			}
		}
	}

	/**
	 * Initialize the attributes common to all option elements from an option descriptor. 
	 * @param elt the element whose attributes are to be initialized.
	 * @param desc the descriptor to get the attribute values from.
	 * @throws Exception if an error was raised while initializing the attributes.
	 */
	public void initCommonAttributes(AbstractOptionElement elt, OptionDescriptor desc) throws Exception
	{
		elt.setName(desc.name);
		elt.setLabel(StringUtils.getLocalized(baseName, desc.name+".label"));
		String s = desc.getProperty("orientation", "horizontal");
		elt.setOrientation("horizontal".equalsIgnoreCase(s) ? OptionsPage.HORIZONTAL : OptionsPage.VERTICAL);
		elt.setToolTipText(StringUtils.getLocalized(baseName, desc.name+".tooltip"));
		elt.setScrollable(desc.getBoolean("scrollable", false));
		elt.setBordered(desc.getBoolean("bordered", false));
		elt.setWidth(desc.getInt("width", -1));
		elt.setHeight(desc.getInt("height", -1));
	}

	/**
	 * Initialize the attributes common to all options from an option descriptor. 
	 * @param option the option whose attributes are to be initialized.
	 * @param desc the descriptor to get the attribute values from.
	 * @throws Exception if an error was raised while initializing the attributes.
	 */
	public void initCommonOptionAttributes(AbstractOption option, OptionDescriptor desc) throws Exception
	{
		initCommonAttributes(option, desc);
		for (ListenerDescriptor listenerDesc: desc.listeners)
		{
			Class clazz = Class.forName(listenerDesc.className);
			ValueChangeListener listener = (ValueChangeListener) clazz.newInstance();
			option.addValueChangeListener(listener);
		}
	}

	/**
	 * Build an option page from the specified option descriptor.
	 * @param desc the descriptor to get the page properties from.
	 * @return an <code>OptionsPage</code> instance, or null if the page could not be build.
	 * @throws Exception if an error was raised while building the page.
	 */
	public OptionsPage buildPage(OptionDescriptor desc) throws Exception
	{
		OptionPanel page = new OptionPanel();
		page.setEventsEnabled(false);
		initCommonAttributes(page, desc);
		page.setMainPage(desc.getBoolean("main"));
		page.createUI();
		for (OptionDescriptor child: desc.children)
		{
			String type = child.type;
			if ("page".equals(type)) page.add(buildPage(child));
			else if ("Button".equals(type)) page.add(buildButton(child));
			else if ("TextArea".equals(type)) page.add(buildTextArea(child));
			else if ("Password".equals(type)) page.add(buildPassword(child));
			else if ("PlainText".equals(type)) page.add(buildPlainText(child));
			else if ("FormattedNumber".equals(type)) page.add(buildFormattedNumber(child));
			else if ("SpinnerNumber".equals(type)) page.add(buildSpinnerNumber(child));
			else if ("Boolean".equals(type)) page.add(buildBoolean(child));
			else if ("ComboBox".equals(type)) page.add(buildComboBox(child));
			else if ("Filler".equals(type)) page.add(buildFiller(child));
			else if ("List".equals(type)) page.add(buildList(child));
		}
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
}
