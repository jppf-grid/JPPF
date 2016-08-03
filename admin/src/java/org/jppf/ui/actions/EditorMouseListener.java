/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.ui.actions;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;

import javax.swing.*;

import org.jppf.ui.monitoring.diagnostics.ThreadDumpAction;
import org.slf4j.*;

/**
 * This mouse listener is set on an editor panel and
 * creates a popup menu to enable copying its content as text to the system clipboard.
 */
public class EditorMouseListener extends MouseAdapter {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(EditorMouseListener.class);
  /**
   * The string to copy to the clipboard.
   */
  private final String text;

  /**
   * Intiialize this mouse listener.
   * @param text the string to copy to the clipboard.
   */
  public EditorMouseListener(final String text) {
    this.text = text;
  }

  /**
   * Processes right-click events to display popup menus.
   * @param event the mouse event to process.
   * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
   */
  @Override
  public void mousePressed(final MouseEvent event) {
    int x = event.getX();
    int y = event.getY();
    int button = event.getButton();
    if (button == MouseEvent.BUTTON3) {
      JPopupMenu menu = new JPopupMenu();
      ClipboardAction action = new ClipboardAction();
      action.putValue(ThreadDumpAction.NAME, "Copy to clipboard");
      menu.add(new JMenuItem(action));
      menu.show(event.getComponent(), x, y);
    }
  }

  /**
   * This action copies a string specified in its constructor to the system clipboard.
   */
  private class ClipboardAction extends AbstractAction {
    @Override
    public void actionPerformed(final ActionEvent event) {
      try {
        Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
        clip.setContents(new StringSelection(text), null);
      } catch (Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
