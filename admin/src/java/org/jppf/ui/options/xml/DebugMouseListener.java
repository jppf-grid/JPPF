/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.ui.options.xml;

import java.awt.Component;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.options.*;
import org.slf4j.*;

/**
 * Mouse listener for debug use. Shows a popup menu on the top container of options loaded through an "import" tag in the XML descriptor.
 * The menu provides one option to reload the page.
 * @author Laurent Cohen
 */
public class DebugMouseListener extends MouseAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DebugMouseListener.class);
  /**
   * The option to debug.
   */
  private OptionElement option = null;
  /**
   * Determines whether the XML is loaded from a url or file location.
   */
  private String source = null;
  /**
   * Where to load the xml descriptor from.
   */
  private String location = null;

  /**
   * 
   * @param option the option to debug.
   * @param source determines whether the XML is loaded from a url or file location.
   * @param location where to load the xml descriptor from.
   */
  public DebugMouseListener(final OptionElement option, final String source, final String location) {
    this.option = option;
    this.source = source;
    this.location = location;
  }

  /**
   * Processes right-click events to display popup menus.
   * @param event the mouse event to process.
   */
  @Override
  public void mousePressed(final MouseEvent event) {
    if (event.getButton() != MouseEvent.BUTTON3) return;
    final Component comp = event.getComponent();
    final JPopupMenu menu = new JPopupMenu();
    final JMenuItem item = new JMenuItem("Reload");
    item.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        doReloadPage();
      }
    });
    menu.add(item);
    menu.show(comp, event.getX(), event.getY());
  }

  /**
   * Reload the page.
   */
  private void doReloadPage() {
    try {
      final OptionContainer parent = (OptionContainer) option.getParent();
      TabbedPaneOption tpo = null;
      int idx = -1;
      if (parent instanceof TabbedPaneOption){
        tpo = (TabbedPaneOption) parent;
        idx = ((JTabbedPane) tpo.getUIComponent()).getSelectedIndex();
      }
      parent.remove(option);
      final OptionsPageBuilder builder = new OptionsPageBuilder(true);
      OptionElement elt;
      if ("url".equalsIgnoreCase(source)) elt = builder.buildPageFromURL(location, builder.getBaseName());
      else elt = builder.buildPage(location, null);
      builder.getFactory().addDebugComp(elt, source, location);
      if (tpo != null) {
        tpo.add(elt, idx);
        final JTabbedPane pane = (JTabbedPane) tpo.getUIComponent();
        final int i = idx;
        SwingUtilities.invokeLater(new Runnable() {
          @Override
          public void run() {
            pane.setSelectedIndex(i);
          }
        });
      }
      else parent.add(elt);
      builder.triggerInitialEvents(elt);
      parent.getUIComponent().repaint();
    } catch(final Exception  e) {
      log.error(e.getMessage(), e);
    }
  }
}
