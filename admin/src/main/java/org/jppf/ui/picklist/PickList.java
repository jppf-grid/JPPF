/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.ui.picklist;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;

import org.jppf.ui.utils.GuiUtils;
import org.jppf.utils.LocalizationUtils;

import net.miginfocom.swing.MigLayout;

/**
 * A pick list which provides a left and right list, where available items are picked from the left list and put into the right list.
 * @param <T> The type of the itzems in this pick list.
 * @author Laurent Cohen
 */
public class PickList<T> extends JPanel {
  /**
   * Base name for the localization resource bundle.
   */
  private static final String I18N_BASE = "org.jppf.ui.i18n.PickList";
  /**
   * Indicates an event for when items are added to the list of picked items.
   */
  private static final int ADDED = 1;
  /**
   * Indicates an event for when items are removed the list of picked items.
   */
  private static final int REMOVED = 2;
  /**
   * The JList holding the available items.
   */
  private JList<T> availableList = new JList<>();
  /**
   * The JList holding the picked items.
   */
  private JList<T> pickedList = new JList<>();
  /**
   * The buttons for manipulating the avialable and picked items.
   */
  private JButton btnLeft, btnRight, btnUp, btnDown;
  /**
   * The list of all items.
   */
  private final List<Object> allItems = new ArrayList<>();
  /**
   *
   */
  private final Map<T, Integer> indexMap = new HashMap<>();
  /**
   *
   */
  private String leftTitle, rightTitle;
  /**
   * The list of listeners to changes in this pick list.
   */
  private final List<PickListListener<T>> listeners = new CopyOnWriteArrayList<>();

  /**
   * Default constructor.
   */
  public PickList() {
    final JComponent compLeft = setupList(availableList, null, event -> onAvailableSelectionChange());
    final JComponent compRight = setupList(pickedList, null, event -> onPickedSelectionChange());
    final MigLayout layout = new MigLayout("insets 0");
    setLayout(layout);
    add(compLeft, "grow, push");
    add(createButtonsPanel(), "growy, pushy");
    add(compRight, "grow, push");
  }

  /**
   * Create a new {@link JList} the list of available items.
   * @param list the list to setup.
   * @param title the list title, may be {@code null}.
   * @param listener a list selection listener.
   * @return the list that was created.
   */
  private JComponent setupList(final JList<T> list, final String title, final ListSelectionListener listener) {
    list.setModel(new DefaultListModel<T>());
    list.addListSelectionListener(listener);
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), title == null ? "" : title));
    final JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setBorder(BorderFactory.createEmptyBorder());
    GuiUtils.adjustScrollbarsThickness(scrollPane);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    list.setCellRenderer(new TooltippedListCellRenderer());
    return scrollPane;
  }

  /**
   * Create the buttons panel.
   * @return a {@link JComponent} that holds the buttons.
   */
  private JComponent createButtonsPanel() {
    final JPanel panel = new JPanel();
    final MigLayout layout = new MigLayout("fill, flowy, insets 0 8 0 8");
    panel.setLayout(layout);
    btnLeft = createButton("Left", "/org/jppf/ui/resources/arrow-left-double-2.png", event -> doLeft());
    btnRight = createButton("Right", "/org/jppf/ui/resources/arrow-right-double-2.png", event -> doRight());
    btnUp = createButton("Up", "/org/jppf/ui/resources/arrow-up-double-2.png", event -> doUp());
    btnDown = createButton("Down", "/org/jppf/ui/resources/arrow-down-double-2.png", event -> doDown());
    panel.add(btnLeft, "grow 0");
    panel.add(btnRight, "grow 0");
    panel.add(btnUp, "grow 0");
    panel.add(btnDown, "grow 0");
    panel.add(new JPanel(), "grow, push");
    return panel;
  }

  /**
   * Create a button with the specified text and icon.
   * @param name the button text.
   * @param iconPath the path to the icon.
   * @param listener an action listener to register with the button.
   * @return a {@link JButton} instance.
   */
  private static JButton createButton(final String name, final String iconPath, final ActionListener listener) {
    final JButton btn = new JButton();
    final String text = LocalizationUtils.getLocalized(I18N_BASE, name + ".tooltip");
    final ImageIcon icon = GuiUtils.loadIcon(iconPath);
    if (icon != null) btn.setIcon(icon);
    if (text != null) btn.setToolTipText(text);
    btn.addActionListener(listener);
    btn.setEnabled(false);
    return btn;
  }

  /**
   * Invoked whenever the selection is changed in the list of available items.
   */
  private void onAvailableSelectionChange() {
    final int[] indices = availableList.getSelectedIndices();
    btnRight.setEnabled(indices.length > 0);
  }

  /**
   * Invoked whenever the selection is changed in the list of picked items.
   */
  private void onPickedSelectionChange() {
    final int[] indices = pickedList.getSelectedIndices();
    final boolean empty = indices.length <= 0;
    btnLeft.setEnabled(!empty);
    btnUp.setEnabled(!empty && indices[0] > 0);
    btnDown.setEnabled(!empty && indices[indices.length - 1] < pickedList.getModel().getSize() - 1);
  }

  /**
   * Move the selected available items to the picked items list.
   */
  private void doRight() {
    final int[] indices = availableList.getSelectedIndices();
    final DefaultListModel<T> availableModel = (DefaultListModel<T>) availableList.getModel();
    final DefaultListModel<T> pickedModel = (DefaultListModel<T>) pickedList.getModel();
    final List<T> addedItems = new ArrayList<>(indices.length);
    for (int i=indices.length-1; i>= 0; i--) {
      final T item = availableModel.remove(indices[i]);
      pickedModel.addElement(item);
      addedItems.add(item);
    }
    fireEvent(ADDED, addedItems);
  }

  /**
   * Move the selected picked items to the available items list.
   */
  private void doLeft() {
    final int[] indices = pickedList.getSelectedIndices();
    final DefaultListModel<T> availableModel = (DefaultListModel<T>) availableList.getModel();
    final DefaultListModel<T> pickedModel = (DefaultListModel<T>) pickedList.getModel();
    final List<T> removedItems = new ArrayList<>(indices.length);
    for (int i=indices.length-1; i>= 0; i--) {
      final T item = pickedModel.remove(indices[i]);
      removedItems.add(item);
      final int itemIndex = indexMap.get(item);
      boolean found = false;
      for (int j=0; j<availableModel.size(); j++) {
        final int n = indexMap.get(availableModel.elementAt(j));
        if (itemIndex < n) {
          availableModel.add(j, item);
          found = true;
          break;
        }
      }
      if (!found) availableModel.addElement(item);
    }
    fireEvent(REMOVED, removedItems);
  }

  /**
   * Move the selected available items to the picked items list.
   */
  private void doUp() {
    final int[] indices = pickedList.getSelectedIndices();
    final DefaultListModel<T> pickedModel = (DefaultListModel<T>) pickedList.getModel();
    for (final int n: indices) {
      final T item = pickedModel.remove(n);
      pickedModel.add(n-1, item);
    }
    for (int i=0; i<indices.length; i++) indices[i]--;
    pickedList.setSelectedIndices(indices);
  }

  /**
   * Move the selected available items to the picked items list.
   */
  private void doDown() {
    final int[] indices = pickedList.getSelectedIndices();
    final DefaultListModel<T> pickedModel = (DefaultListModel<T>) pickedList.getModel();
    for (final int n: indices) {
      final T item = pickedModel.remove(n);
      pickedModel.add(n+1, item);
    }
    for (int i=0; i<indices.length; i++) indices[i]++;
    pickedList.setSelectedIndices(indices);
  }

  /**
   * Resets the lists of items managed by this UI component: all/available/picked items.
   * The available items list is computed as the list of all items minus the items in the picked list.
   * @param allItems the list of all items.
   * @param pickedItems the items that are already picked, if any.
   */
  @SuppressWarnings("unchecked")
  public void resetItems(final List<T> allItems, final List<T> pickedItems) {
    this.allItems.clear();
    this.allItems.addAll(allItems);
    indexMap.clear();
    int maxLength = 0;
    T maxItem = null;
    for (int i=0; i<allItems.size(); i++) {
      final T item = allItems.get(i);
      indexMap.put(item, i);
      final String s = item.toString();
      if (s.length() > maxLength) {
        maxLength = s.length();
        maxItem = item;
      }
    }
    availableList.setPrototypeCellValue(maxItem);
    final List<T> availableItems = new ArrayList<>(allItems);
    availableItems.removeAll(pickedItems);
    DefaultListModel<Object> model = (DefaultListModel<Object>) availableList.getModel();
    model.removeAllElements();
    for (final Object o: availableItems) model.addElement(o);
    availableList.setVisibleRowCount(allItems.size());
    pickedList.setPrototypeCellValue(maxItem);
    model = (DefaultListModel<Object>) pickedList.getModel();
    model.removeAllElements();
    for (final Object o: pickedItems) model.addElement(o);
    pickedList.setVisibleRowCount(allItems.size());
  }

  /**
   * Get the items that were picked.
   * @return a list of items in the same order as in the corresponding {@link JList}.
   */
  public List<T> getPickedItems() {
    return getItems(pickedList);
  }

  /**
   * Get the items that were picked.
   * @return a list of items in the same order as in the corresponding {@link JList}.
   */
  public List<T> getAvailableItems() {
    return getItems(availableList);
  }

  /**
   * Get the items from the specified JList.
   * @param jlist the {@code JList} from which to extract the items.
   * @return a list of items.
   */
  private List<T> getItems(final JList<T> jlist) {
    final List<T> list = new ArrayList<>();
    final DefaultListModel<T> model = (DefaultListModel<T>) jlist.getModel();
    final Enumeration<T> en = model.elements();
    while (en.hasMoreElements()) list.add(en.nextElement());
    return list;
  }

  /**
   * Get the title of the left list.
   * @return the title as a string.
   */
  public String getLeftTitle() {
    return leftTitle;
  }

  /**
   * Set the title of the left list.
   * @param leftTitle the title as a string.
   */
  public void setLeftTitle(final String leftTitle) {
    this.leftTitle = leftTitle;
    availableList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), leftTitle == null ? "" : leftTitle));
  }

  /**
   * Get the title of the right list.
   * @return the title as a string.
   */
  public String getRightTitle() {
    return rightTitle;
  }

  /**
   * Set the title of the right list.
   * @param rightTitle the title as a string.
   */
  public void setRightTitle(final String rightTitle) {
    this.rightTitle = rightTitle;
    pickedList.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(EtchedBorder.RAISED), rightTitle == null ? "" : rightTitle));
  }

  /**
   * Set the same cell renderer for the left and right {@link JList} components of this pick list.
   * @param renderer the renderer for the lists on the left and on the right. If {@code null}, then no change is made.
   */
  public void setListCellRenderers(final ListCellRenderer<T> renderer) {
    if (renderer != null) setListCellRenderers(renderer, renderer);
  }

  /**
   * Set the cell renderers for the left and right {@link JList} components of this pick list.
   * @param leftRenderer the renderer for the list on the left. If {@code null}, then no change is made tot he list on the left.
   * @param rightRenderer the renderer for the list on the right. If {@code null}, then no change is made tot he list on the right.
   */
  public void setListCellRenderers(final ListCellRenderer<T> leftRenderer, final ListCellRenderer<T> rightRenderer) {
    if (leftRenderer != null) availableList.setCellRenderer(leftRenderer);
    if (rightRenderer != null) pickedList.setCellRenderer(rightRenderer);
  }

  /**
   * Add a listener for changes int his pick list.
   * @param listener the listener to add.
   */
  public void addPickListListener(final PickListListener<T> listener) {
    if (listener != null) listeners.add(listener);
  }

  /**
   * Remove a listener for changes int his pick list.
   * @param listener the listener to remove.
   */
  public void removePickListListener(final PickListListener<T> listener) {
    if (listener != null) listeners.remove(listener);
  }

  /**
   * Notify all listeners of a change in the list of picked items.
   * @param type the type of event, either {@link #ADDED} or {@link #REMOVED}.
   * @param items the items that were added to or removed from the picked items.
   */
  private void fireEvent(final int type, final List<T> items) {
    if (!listeners.isEmpty()) {
      final PickListEvent<T> event;
      switch(type) {
        case ADDED:
          event = new PickListEvent<>(this, items, null);
          for (PickListListener<T> listener: listeners) listener.itemsAdded(event);
          break;

        case REMOVED:
          event = new PickListEvent<>(this, null, items);
          for (PickListListener<T> listener: listeners) listener.itemsRemoved(event);
          break;
      }
    }
  }

  /**
   * Test the pick list.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      UIManager.setLookAndFeel("com.jgoodies.looks.plastic.PlasticXPLookAndFeel");
      final JFrame frame = new JFrame("testing pick-list");
      final MigLayout layout = new MigLayout("flowx, flowy");
      frame.setLayout(layout);
      final PickList<Object> plist = new PickList<>();
      final List<Object> allItems = new ArrayList<>();
      for (int i=1; i<=10; i++) allItems.add("pickable item " + i);
      final List<Object> pickedItems = Arrays.asList(allItems.get(1), allItems.get(3), allItems.get(5), allItems.get(7));
      plist.setLeftTitle("Available");
      plist.setRightTitle("Selected");
      plist.resetItems(allItems, pickedItems);
      plist.setListCellRenderers(null, new MyListCellRenderer());
      frame.add(plist, "grow, push");
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.pack();
      frame.setVisible(true);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   *
   */
  private static class MyListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
      final JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      setBackground(isSelected ? Color.YELLOW.brighter() : Color.WHITE);
      comp.setForeground(Color.GREEN.darker().darker());
      return comp;
    }
  }

  /**
   * A ListCellRenderer that displays a tooltip, if any, for each cell.
   */
  private static class TooltippedListCellRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
      final JLabel comp = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      if (value instanceof Tooltipped) comp.setToolTipText(((Tooltipped) value).getTooltip());
      return comp;
    }
  }
}
