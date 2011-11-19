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
package org.jppf.ui.options;

import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import org.jppf.ui.utils.GuiUtils;

/**
 * Option holding a JList as its UI component.
 * The list selection mode can be either simple selection or multiple interval selection.
 * @author Laurent Cohen
 */
public class ListOption extends AbstractOption
{
	/**
	 * Explicit serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The underlying JList component.
	 */
	protected JList list = null;
	/**
	 * The items contained in the list.
	 */
	protected List items = null;
	/**
	 * The selection mode for the list.
	 */
	protected int selMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION;
	/**
	 * Selection listener set on the list and used to propagate selection
	 * changes as value change events.
	 */
	protected transient ListSelectionListener selectionListener = null;

	/**
	 * Default constructor.
	 */
	public ListOption()
	{
	}

	/**
	 * Initialize this combo box option with the specified parameters.
	 * @param name this component's name.
	 * @param label the label displayed with the checkbox.
	 * @param tooltip the tooltip associated with the combobox.
	 * @param value the initially selected value of this component.
	 * @param items the initial list of items in the combo box.
	 * @param selMode the mode of selection for the list, can be either
	 * {@link javax.swing.ListSelectionModel#SINGLE_SELECTION SINGLE_SELECTION} or
	 * {@link javax.swing.ListSelectionModel#MULTIPLE_INTERVAL_SELECTION MULTIPLE_INTERVAL_SELECTION}.
	 */
	public ListOption(final String name, final String label, final String tooltip, final List<Object> value, final List<Object> items,
			final int selMode)
	{
		this.name = name;
		this.label = label;
		setToolTipText(tooltip);
		this.value = value;
		this.items = items;
		this.selMode = selMode;
		createUI();
	}

	/**
	 * Define a selection listener that will forward selection changes to
	 * all value change listeners that registered with this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	@Override
	protected void setupValueChangeNotifications()
	{
		selectionListener = new ListSelectionListener()
		{
			@Override
			public void valueChanged(final ListSelectionEvent e)
			{
				if (e.getValueIsAdjusting()) return;
				List<Object> sel = new ArrayList<Object>();
				sel.addAll(Arrays.asList(list.getSelectedValues()));
				value = sel;
				fireValueChanged();
			}
		};
		list.addListSelectionListener(selectionListener);
	}

	/**
	 * Create the UI components for this option.
	 * @see org.jppf.ui.options.AbstractOptionElement#createUI()
	 */
	@Override
	public void createUI()
	{
		list = new JList();
		if (toolTipText != null) list.setToolTipText(toolTipText);
		DefaultListModel model = new DefaultListModel();
		list.setModel(model);
		list.setSelectionMode(selMode);
		JComponent comp = list;
		if (scrollable)
		{
			JScrollPane scrollPane = new JScrollPane(list);
			scrollPane.setBorder(BorderFactory.createEmptyBorder());
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
			comp = scrollPane;
		}
		if (bordered)
		{
			if (!scrollable) comp = layoutComponents(GuiUtils.createFiller(1, 1), list);
			comp.setBorder(BorderFactory.createTitledBorder(label));
		}
		UIComponent = comp;
		populateList();
		setupValueChangeNotifications();
	}

	/**
	 * Populate the list using the current list of items along with the current
	 * list of selected items.
	 */
	private void populateList()
	{
		if (list == null) return;
		list.removeListSelectionListener(selectionListener);
		list.clearSelection();
		DefaultListModel model = (DefaultListModel) list.getModel();
		if (value == null) value = new ArrayList();
		else if (value instanceof String)
		{
			List<Object> tmpList = new ArrayList<Object>();
			String[] names = ((String) value).split(",");
			for (String s: names)
			{
				s = s.trim();
				for (Object o: items)
				{
					if (s.equals(o.toString()))
					{
						tmpList.add(o);
						break;
					}
				}
			}
			value = tmpList;
		}
		List selectedItems = (List) value;
		model.removeAllElements();
		for (Object item: items) model.addElement(item);
		List<Integer> indices = new ArrayList<Integer>();
		for (Object item: selectedItems)
		{
			int n = items.indexOf(item);
			if (n >= 0) indices.add(n);
		}
		int[] array = new int[indices.size()];
		for (int i=0; i<array.length; i++) array[i] = indices.get(i);
		list.setSelectedIndices(array);
		list.addListSelectionListener(selectionListener);
	}

	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.OptionElement#setEnabled(boolean)
	 */
	@Override
	public void setEnabled(final boolean enabled)
	{
		list.setEnabled(enabled);
	}

	/**
	 * Set the value of this option.
	 * @param value the value as an <code>Object</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	@Override
	public void setValue(final Object value)
	{
		this.value = value;
		populateList();
		fireValueChanged();
	}

	/**
	 * Get the items in the list.
	 * @return a list of <code>Object</code> instances.
	 */
	public List getItems()
	{
		return items;
	}

	/**
	 * Set the items in the list.
	 * @param items a list of <code>Object</code> instances.
	 */
	public void setItems(final List items)
	{
		this.items = items;
		populateList();
		fireValueChanged();
	}

	/**
	 * Get the selection mode for the list.
	 * @return the selection mode as an int value.
	 */
	public int getSelMode()
	{
		return selMode;
	}

	/**
	 * Set the selection mode for the list.
	 * @param selMode the selection mode as an int value.
	 */
	public void setSelMode(final int selMode)
	{
		this.selMode = selMode;
	}

	/**
	 * Get the list component.
	 * @return a <code>JList</code> instance.
	 */
	public JList getList()
	{
		return list;
	}
}
