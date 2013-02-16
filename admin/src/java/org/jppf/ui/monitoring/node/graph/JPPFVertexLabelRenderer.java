/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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

package org.jppf.ui.monitoring.node.graph;

import java.awt.*;

import javax.swing.*;

import org.jppf.client.*;
import org.jppf.ui.monitoring.node.*;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.ui.utils.GuiUtils;

import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;

/**
 * Instances of this class handle the rendering of the vertices in the graph view.
 * @author Laurent Cohen
 */
public class JPPFVertexLabelRenderer extends DefaultVertexLabelRenderer
{
  /**
   * Default constructor, initializes this renderer with a white background.
   */
  public JPPFVertexLabelRenderer()
  {
    super(Color.white);
  }

  @Override
  public <V> Component getVertexLabelRendererComponent(final JComponent vv, final Object value, final Font fnt, final boolean sel, final V vertex)
  {
    DefaultVertexLabelRenderer renderer = (DefaultVertexLabelRenderer) super.getVertexLabelRendererComponent(vv, value, fnt, sel, vertex);
    TopologyData data = (TopologyData) vertex;
    renderer.setHorizontalTextPosition(SwingConstants.CENTER);
    renderer.setVerticalTextPosition(SwingConstants.BOTTOM);
    //renderer.setText(data.getId());
    renderer.setText(data.toString());

    String path = null;
    Color background = Color.white;
    Color backgroundSelected = Color.blue;
    Color foreground = sel ? AbstractTreeCellRenderer.DEFAULT_SELECTION_FOREGROUND : AbstractTreeCellRenderer.DEFAULT_FOREGROUND;
    Font f = renderer.getFont();
    Font font = AbstractTreeCellRenderer.getPlainFont(f);

    if (!data.isNode())
    {
      JPPFClientConnection c = data.getClientConnection();
      if (c == null)
      {
        path = AbstractTreeCellRenderer.DRIVER_ICON;
        background = AbstractTreeCellRenderer.INACTIVE_COLOR;
      }
      else if ((c.getStatus() != JPPFClientConnectionStatus.FAILED) && (c.getStatus() != JPPFClientConnectionStatus.DISCONNECTED))
      {
        path = AbstractTreeCellRenderer.DRIVER_ICON;
        background = AbstractTreeCellRenderer.ACTIVE_COLOR;
        font = AbstractTreeCellRenderer.getBoldFont(f);
      }
      else
      {
        path = AbstractTreeCellRenderer.DRIVER_INACTIVE_ICON;
        background = AbstractTreeCellRenderer.INACTIVE_COLOR;
        backgroundSelected = AbstractTreeCellRenderer.INACTIVE_SELECTION_COLOR;
        font = AbstractTreeCellRenderer.getBoldItalicFont(f);
      }
    }
    else
    {
      path = AbstractTreeCellRenderer.NODE_ICON;
      if (!TopologyDataStatus.UP.equals(data.getStatus()))
      {
        background = AbstractTreeCellRenderer.INACTIVE_COLOR;
        backgroundSelected = AbstractTreeCellRenderer.INACTIVE_SELECTION_COLOR;
        font = AbstractTreeCellRenderer.getItalicFont(f);
      }
      /*
      else
      {
        JMXConnectionWrapper wrapper = data.getJmxWrapper();
        boolean b = wrapper != null && wrapper.isConnected();
        if (!b) foreground = AbstractTreeCellRenderer.UNMANAGED_COLOR;
      }
      */
      //renderer.setToolTipText(computeNodeText(data));
    }

    if (font != null) setFont(font);
    ImageIcon icon = GuiUtils.loadIcon(path);
    renderer.setIcon(icon);
    renderer.setBackground(sel ? backgroundSelected : background);
    renderer.setForeground(foreground);

    return renderer;
  }

  /**
   * Compute ther tooltipe for a node vertex.
   * @param node contains the information to put in the tooltip.
   * @return the text to set as tooltip.
   */
  private String computeNodeText(final TopologyData node)
  {
    StringBuilder sb = new StringBuilder();
    sb.append(node.getId()).append("<br>");
    sb.append("Threads: ").append(node.getNodeState().getThreadPoolSize());
    sb.append(" | Tasks: ").append(node.getNodeState().getNbTasksExecuted());
    return sb.toString();
  }
}
