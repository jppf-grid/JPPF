/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
import org.jppf.ui.utils.TreeTableUtils;
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
    setupIcon("/org/jppf/ui/resources/info.gif");
    setupNameAndTooltip("show.information");
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
    final JDialog dialog = new JDialog(OptionsHandler.getMainWindow(), "System information", false);
    //dialog.getRootPane().setWindowDecorationStyle(JRootPane.WARNING_DIALOG);
    dialog.setIconImage(((ImageIcon) getValue(SMALL_ICON)).getImage());
    dialog.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        dialog.setVisible(false);
        dialog.dispose();
      }
    });
    final JEditorPane editor = new JEditorPane("text/html", "retrieving information ...");
    AbstractButton btn = (AbstractButton) event.getSource();
    if (btn.isShowing()) location = btn.getLocationOnScreen();
    editor.setEditable(false);
    editor.setOpaque(true);
    editor.setBackground(Color.WHITE);
    editor.setCaretPosition(0);
    JScrollPane panel = new JScrollPane(editor);
    dialog.getContentPane().add(panel);
    AbstractAction escAction = new AbstractAction() {
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
   * Retrieve the system information for the specified topology object.
   * @param data the topology object for which to get the information.
   * @return a {@link JPPFSystemInformation} or <code>null</code> if the information could not be retrieved.
   */
  private JPPFSystemInformation retrieveInfo(final AbstractTopologyComponent data) {
    JPPFSystemInformation info = null;
    try {
      if (data.isNode()) {
        TopologyDriver parent = (TopologyDriver) data.getParent();
        Map<String, Object> result = parent.getForwarder().systemInformation(new UuidSelector(data.getUuid()));
        Object o = result.get(data.getUuid());
        if (o instanceof JPPFSystemInformation) info = (JPPFSystemInformation) o;
      } else {
        if (data.isPeer()) {
          String uuid = ((TopologyPeer) data).getUuid();
          if (uuid != null) {
            TopologyDriver driver = StatsHandler.getInstance().getTopologyManager().getDriver(uuid);
            if (driver != null) info = driver.getJmx().systemInformation();
          }
        }
        info = ((TopologyDriver) data).getJmx().systemInformation();
      }
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return info;
  }

  /**
   * Print the specified system info to a string.
   * @param info the information to print.
   * @param format the formatter to use.
   * @return a String with the formatted information.
   */
  private String formatProperties(final JPPFSystemInformation info, final PropertiesTableFormat format) {
    format.start();
    if (info == null) format.print("No information was found");
    else {
      format.formatTable(info.getUuid(), "UUID");
      format.formatTable(info.getSystem(), "System Properties");
      format.formatTable(info.getEnv(), "Environment Variables");
      format.formatTable(info.getRuntime(), "Runtime Information");
      format.formatTable(info.getJppf(), "JPPF configuration");
      format.formatTable(info.getNetwork(), "Network configuration");
      format.formatTable(info.getStorage(), "Storage Information");
      format.formatTable(info.getOS(), "Operating System Information");
    }
    format.end();
    return format.getText();
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
      final StringBuilder title = new StringBuilder("System information");
      try {
        AbstractTopologyComponent comp = dataArray[0];
        JPPFSystemInformation info = retrieveInfo(comp);
        String name = TreeTableUtils.getDisplayName(comp);
        title.append(" for ").append(comp.isNode() ? "node " : "driver ").append(name);
        html.append(formatProperties(info, new HTMLPropertiesTableFormat(title.toString())));
        toClipboard.append(formatProperties(info, new TextPropertiesTableFormat(title.toString())));
      } catch(Exception e) {
        toClipboard.append(ExceptionUtils.getStackTrace(e));
        html.append(toClipboard.toString().replace("\n", "<br>"));
      }
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          try {
          dialog.setTitle(title.toString());
          editor.setText(html.toString());
          editor.setCaretPosition(0);
          editor.addMouseListener(new EditorMouseListener(toClipboard.toString()));
          } catch(Exception e) {
            if (debugEnabled) log.debug("exception while setting system information dialog data: ", e);
          }
        }
      });
    }
  };
}
