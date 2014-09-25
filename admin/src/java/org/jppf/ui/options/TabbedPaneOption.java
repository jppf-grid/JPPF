/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import java.awt.Component;

import javax.swing.*;

import org.jppf.ui.options.docking.DockingManager;
import org.jppf.ui.utils.GuiUtils;

/**
 * This option class encapsulates a tabbed pane, as the one present in the Swing api.
 * @author Laurent Cohen
 */
public class TabbedPaneOption extends AbstractOptionContainer
{
  @Override
  public void createUI() {
    JTabbedPane pane = new JTabbedPane();
    pane.setDoubleBuffered(true);
    if (!bordered) pane.setBorder(BorderFactory.createEmptyBorder());
    UIComponent = pane;
  }

  @Override
  public void add(final OptionElement element) {
    add(element, children.size());
  }

  /**
   * Add the specified component at the specified index.
   * @param element the component to add.
   * @param index the index at which to add the component.
   */
  public void add(final OptionElement element, final int index) {
    if (index > children.size()) throw new IndexOutOfBoundsException("index should be < " + children.size() + " but is " + index);
    if (index < 0) throw new IndexOutOfBoundsException("negative index " + index);
    //int idx = children.size();
    super.add(element, index);
    JTabbedPane pane = (JTabbedPane) UIComponent;
    ImageIcon icon = null;
    if (element.getIconPath() != null) icon = GuiUtils.loadIcon(element.getIconPath());
    DockingManager dmgr = DockingManager.getInstance();
    try {
      //pane.addTab("", null, element.getUIComponent(), element.getToolTipText());
      pane.insertTab("", null, element.getUIComponent(), element.getToolTipText(), index);
      JLabel l = new JLabel(element.getLabel(), icon, SwingConstants.CENTER);
      pane.setTabComponentAt(index, l);
      if (element.isDetachable()) {
        l.addMouseListener(dmgr.getMouseAdapter());
        if (!dmgr.isRegistered(element)) dmgr.register(element, l);
        else dmgr.update(element, l);
      }
    } catch(Throwable t) {
      t.printStackTrace();
    }
  }

  @Override
  public void remove(final OptionElement element) {
    super.remove(element);
    UIComponent.remove(element.getUIComponent());
    Component comp = null;
  }
}
