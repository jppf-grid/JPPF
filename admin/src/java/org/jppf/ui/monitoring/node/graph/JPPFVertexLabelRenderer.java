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

package org.jppf.ui.monitoring.node.graph;

import java.awt.*;

import javax.swing.*;

import org.jppf.client.JPPFClientConnection;
import org.jppf.client.monitoring.topology.*;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.ui.utils.*;

import edu.uci.ics.jung.visualization.renderers.DefaultVertexLabelRenderer;

/**
 * Instances of this class handle the rendering of the vertices in the graph view.
 * @author Laurent Cohen
 */
public class JPPFVertexLabelRenderer extends DefaultVertexLabelRenderer {
  /**
   * 
   */
  private Font baseFont = null;
  /**
   * 
   */
  private Font boldFont = null;
  /**
   * 
   */
  private Font italicFont = null;
  /**
   * 
   */
  private Font boldItalicFont = null;

  /**
   * Default constructor, initializes this renderer with a white background.
   */
  public JPPFVertexLabelRenderer() {
    super(Color.white);
  }

  @Override
  public <V> Component getVertexLabelRendererComponent(final JComponent vv, final Object value, final Font fnt, final boolean sel, final V vertex) {
    DefaultVertexLabelRenderer renderer = (DefaultVertexLabelRenderer) super.getVertexLabelRendererComponent(vv, value, fnt, sel, vertex);
    AbstractTopologyComponent data = (AbstractTopologyComponent) vertex;
    renderer.setHorizontalTextPosition(SwingConstants.CENTER);
    renderer.setVerticalTextPosition(SwingConstants.BOTTOM);
    renderer.setText(TreeTableUtils.getDisplayName(data));

    String path = null;
    Color background = Color.white;
    Color backgroundSelected = Color.blue;
    Color foreground = sel ? AbstractTreeCellRenderer.DEFAULT_SELECTION_FOREGROUND : AbstractTreeCellRenderer.DEFAULT_FOREGROUND;
    if (baseFont == null) {
      Font f2 = renderer.getFont();
      baseFont = new Font(f2.getFontName(), f2.getStyle(), 10);
      boldFont = new Font(baseFont.getName(), Font.BOLD, baseFont.getSize());
      italicFont = new Font(baseFont.getName(), Font.ITALIC, baseFont.getSize());
      boldItalicFont = new Font(baseFont.getName(), Font.BOLD|Font.ITALIC, baseFont.getSize());
    }
    Font font = baseFont;

    if (data.isDriver()) {
      JPPFClientConnection c = ((TopologyDriver) data).getConnection();
      if (c == null) {
        path = AbstractTreeCellRenderer.DRIVER_ICON;
        background = AbstractTreeCellRenderer.INACTIVE_COLOR;
      } else if (c.getStatus().isWorkingStatus()) {
        path = AbstractTreeCellRenderer.DRIVER_ICON;
        background = AbstractTreeCellRenderer.ACTIVE_COLOR;
        font = boldFont;
      } else {
        path = AbstractTreeCellRenderer.DRIVER_INACTIVE_ICON;
        background = AbstractTreeCellRenderer.INACTIVE_COLOR;
        backgroundSelected = AbstractTreeCellRenderer.INACTIVE_SELECTION_COLOR;
        font = boldItalicFont;
      }
    } else {
      path = GuiUtils.computeNodeIconKey((TopologyNode) data);
      if (((TopologyNode) data).getStatus() != TopologyNodeStatus.UP) {
        background = AbstractTreeCellRenderer.INACTIVE_COLOR;
        backgroundSelected = AbstractTreeCellRenderer.INACTIVE_SELECTION_COLOR;
        font = italicFont;
      }
    }

    if (font != null) renderer.setFont(font);
    ImageIcon icon = GuiUtils.loadIcon(path);
    renderer.setIcon(icon);
    renderer.setBackground(sel ? backgroundSelected : background);
    renderer.setForeground(foreground);

    return renderer;
  }
}
