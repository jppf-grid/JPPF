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
package org.jppf.ui.monitoring.node.actions;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.actions.EditorMouseListener;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.ui.utils.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This action displays the driver or node environment information in a separate frame.
 */
public class SystemInformationAction extends AbstractTopologyAction {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SystemInformationAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this action.
   */
  public SystemInformationAction() {
    BASE = "org.jppf.ui.i18n.SystemInfoPage";
    setupIcon("/org/jppf/ui/resources/info.gif");
    setupNameAndTooltip("system.show.information");
  }

  /**
   * Update this action's enabled state based on a list of selected elements.
   * @param selectedElements a list of objects.
   */
  @Override
  public void updateState(final List<Object> selectedElements) {
    this.selectedElements = selectedElements;
    dataArray = new AbstractTopologyComponent[selectedElements.size()];
    int count = 0;
    for (Object o: selectedElements) dataArray[count++] = (AbstractTopologyComponent) o;
    setEnabled(dataArray.length > 0);
  }

  /**
   * Perform the action.
   * @param event not used.
   */
  @Override
  public void actionPerformed(final ActionEvent event) {
    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), localize("system.information"), false);
    dialog.setIconImage(((ImageIcon) getValue(SMALL_ICON)).getImage());
    dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        dialog.setVisible(false);
        dialog.dispose();
      }
    });
    final JEditorPane editor = new JEditorPane("text/html", localize("system.retrieving"));
    final AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    editor.setEditable(false);
    editor.setOpaque(true);
    editor.setBackground(Color.WHITE);
    editor.setCaretPosition(0);
    final JScrollPane panel = new JScrollPane(editor);
    dialog.getContentPane().add(panel);
    final AbstractAction escAction = new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent event) {
        dialog.setVisible(false);
        dialog.dispose();
      }
    };
    setOkCancelKeys(panel, null, escAction);
    dialog.setLocationRelativeTo(null);
    dialog.setLocation(location);
    dialog.setSize(600, 600);
    dialog.setVisible(true);
    runAction(new AsyncRunnable(dialog, editor));
  }

  /**
   * This class asynchronously retrieves the node or driver information and displays it int he dialog.
   */
  private class AsyncRunnable implements Runnable {
    /**
     * The dialog containing the editor component.
     */
    private final JDialog dialog;
    /**
     * The editor whose text is th node information.
     */
    private final JEditorPane editor;
    
    /**
     * Initialize this asynchronous task.
     * @param dialog the dialog containing the editor component.
     * @param editor the editor whose text is th node information.
     */
    public AsyncRunnable(final JDialog dialog, final JEditorPane editor) {
      this.dialog = dialog;
      this.editor = editor;
    }

    @Override
    public void run() {
      final StringBuilder html = new StringBuilder();
      final StringBuilder toClipboard = new StringBuilder();
      final AbstractTopologyComponent comp = dataArray[0];
      final String title = TopologyUtils.getSystemInfoTitle(comp, Locale.getDefault(), StatsHandler.getInstance().getShowIPHandler().isShowIP());
      try {
        final JPPFSystemInformation info = TopologyUtils.retrieveSystemInfo(comp);
        html.append(TopologyUtils.formatProperties(info, new HTMLPropertiesTableFormat(title), Locale.getDefault()));
        toClipboard.append(TopologyUtils.formatProperties(info, new TextPropertiesTableFormat(title.toString()), Locale.getDefault()));
      } catch(final Exception e) {
        toClipboard.append(ExceptionUtils.getStackTrace(e));
        html.append(toClipboard.toString().replace("\n", "<br>"));
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
          dialog.setTitle(title);
          editor.setText(html.toString());
          editor.setCaretPosition(0);
          editor.addMouseListener(new EditorMouseListener(toClipboard.toString()));
          } catch(final Exception e) {
            if (debugEnabled) log.debug("exception while setting system information dialog data: ", e);
          }
        }
      });
    }
  };
}
