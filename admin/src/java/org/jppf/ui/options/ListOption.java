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
package org.jppf.ui.options;

import java.awt.Dimension;
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
	protected ListSelectionListener selectionListener = null;

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
	public ListOption(String name, String label, String tooltip, List<Object> value, List<Object> items,
			int selMode)
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
	protected void setupValueChangeNotifications()
	{
		selectionListener = new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				List<Object> sel = new ArrayList<Object>();
				for (Object o: list.getSelectedValues()) sel.add(o);
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
	public void createUI()
	{
		list = new JList();
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
		if ((width > 0) && (height > 0))
		{
			Dimension d = new Dimension(width, height);
			UIComponent.setPreferredSize(d);
		}
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
	public void setEnabled(boolean enabled)
	{
		list.setEnabled(enabled);
	}

	/**
	 * Set the value of this option.
	 * @param value the value as an <code>Object</code> instance.
	 * @see org.jppf.ui.options.AbstractOption#setValue(java.lang.Object)
	 */
	public void setValue(Object value)
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
	public void setItems(List items)
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
	public void setSelMode(int selMode)
	{
		this.selMode = selMode;
	}
}
