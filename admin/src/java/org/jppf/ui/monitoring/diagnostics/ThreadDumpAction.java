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
package org.jppf.ui.monitoring.diagnostics;

import java.awt.Color;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import org.jppf.management.*;
import org.jppf.management.diagnostics.*;
import org.jppf.ui.actions.EditorMouseListener;
import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.monitoring.node.actions.AbstractTopologyAction;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * This action displays the driver or node environment information in a separate frame.
 */
public class ThreadDumpAction extends AbstractTopologyAction
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ThreadDumpAction.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Initialize this action.
   */
  public ThreadDumpAction()
  {
    setupIcon("/org/jppf/ui/resources/thread_dump.gif");
    setupNameAndTooltip("health.thread.dump");
  }

  @Override
  public void updateState(final List<Object> selectedElements)
  {
    this.selectedElements = selectedElements;
    dataArray = new TopologyData[selectedElements.size()];
    int count = 0;
    for (Object o: selectedElements) dataArray[count++] = (TopologyData) o;
    setEnabled(dataArray.length > 0);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    String s = null;
    String title = "";
    try {
      ThreadDump info = retrieveThreadDump(dataArray[0]);
      boolean isNode = dataArray[0].getType().equals(TopologyDataType.NODE);
      title = "Thread dump for " + (isNode ? "node " : "driver ") + dataArray[0];
      if (info == null) s = "<p><b>No thread dump was generated</b>";
      else s = HTMLThreadDumpWriter.printToString(info, title);
      final JFrame frame = new JFrame(title);
      frame.setIconImage(((ImageIcon) getValue(SMALL_ICON)).getImage());
      frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
      //frame.get
      frame.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(final WindowEvent e) {
          frame.dispose();
        }
      });
      JEditorPane editor = new JEditorPane("text/html", "");
      editor.setBackground(Color.WHITE);
      editor.setText(s);
      editor.setCaretPosition(0);
      editor.addMouseListener(new EditorMouseListener(TextThreadDumpWriter.printToString(info, title)));
      AbstractButton btn = (AbstractButton) event.getSource();
      if (btn.isShowing()) location = btn.getLocationOnScreen();
      editor.setEditable(false);
      editor.setOpaque(true);
      frame.getContentPane().add(new JScrollPane(editor));
      frame.setLocationRelativeTo(null);
      frame.setLocation(location);
      frame.setSize(600, 600);
      frame.setVisible(true);
    } catch(Exception e) {
      s = ExceptionUtils.getStackTrace(e).replace("\n", "<br>");
    }
  }

  /**
   * Retrieve the system information for the specified topology object.
   * @param data the topology object for which to get the information.
   * @return a {@link JPPFSystemInformation} or <code>null</code> if the information could not be retrieved.
   */
  private ThreadDump retrieveThreadDump(final TopologyData data)
  {
    ThreadDump info = null;
    try
    {
      if (TopologyDataType.NODE == data.getType())
      {
        TopologyData parent = data.getParent();
        Map<String, Object> result = parent.getNodeForwarder().threadDump(new NodeSelector.UuidSelector(data.getUuid()));
        Object o = result.get(data.getUuid());
        if (o instanceof ThreadDump) info = (ThreadDump) o;
      }
      else info = data.getDiagnostics().threadDump();
    }
    catch (Exception e)
    {
      if (debugEnabled) log.debug(e.getMessage(), e);
    }
    return info;
  }
}
