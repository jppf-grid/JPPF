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

package org.jppf.ui.treetable;

import java.awt.*;

import javax.swing.tree.DefaultTreeCellRenderer;

import org.jppf.ui.monitoring.data.StatsHandler;

/**
 * Renderer used to render the tree nodes in the node data panel.
 * @author Laurent Cohen
 */
public abstract class AbstractTreeCellRenderer extends DefaultTreeCellRenderer {
  /**
   * The possibl boolean values.
   */
  public static final boolean[] BOOL_VALUES = { false, true };
  /**
   * Path to the location of the icon files.
   */
  public static final String RESOURCES = "/org/jppf/ui/resources/";
  /**
   * Path to the icon used for a driver.
   */
  public static final String DRIVER_ICON = RESOURCES + "mainframe.gif";
  /**
   * Path to the icon used for an inactive driver connection.
   */
  public static final String DRIVER_INACTIVE_ICON = RESOURCES + "mainframe_inactive.gif";
  /**
   * Path to the icon used for a non-master node.
   */
  public static final String NODE_ICON = RESOURCES + "node.png";
  /**
   * Path to the marker icon used for a master node.
   */
  public static final String MARKER_MASTER_ICON = RESOURCES + "marker-master.gif";
  /**
   * Path to the marker icon used for a .Net-capable node.
   */
  public static final String MARKER_DOTNET_ICON = RESOURCES + "marker-dotnet.gif";
  /**
   * Path to the marker icon used for a node that has a pending shutdown.
   */
  public static final String MARKER_PENDING_SHUTDOWN_ICON = RESOURCES + "marker-pending-shutdown.gif";
  /**
   * Path to the marker icon used for a node that has a pending restart.
   */
  public static final String MARKER_PENDING_RESTART_ICON = RESOURCES + "marker-pending-restart.gif";
  /**
   * Path to the icon used for a master node.
   */
  public static final String NODE_MASTER_ICON = RESOURCES + "node-master.png";
  /**
   * Path to the icon used for a non-master node with .Net bridge.
   */
  public static final String NODE_DOTNET_ICON = RESOURCES + "node-dotnet.png";
  /**
   * Path to the icon used for a master node with .Net bridge.
   */
  public static final String NODE_MASTER_DOTNET_ICON = RESOURCES + "node-master-dotnet.png";
  /**
   * Path to the icon used for a job.
   */
  public static final String JOB_ICON = RESOURCES + "job.png";
  /**
   * Critical icon.
   */
  public static final String CRITICAL_ICON = RESOURCES + "critical2.gif";
  /**
   * Highlighting color for active driver connections.
   */
  public static final Color ACTIVE_COLOR = new Color(144, 213, 149);
  /**
   * Highlighting color for non-selected inactive driver connections.
   */
  public static final Color INACTIVE_COLOR = new Color(244, 99, 107);
  /**
   * Highlighting color for selected inactive driver connections.
   */
  public static final Color INACTIVE_SELECTION_COLOR = new Color(214, 127, 255);
  /**
   * Highlighting color for non-selected suspended jobs.
   */
  public static final Color SUSPENDED_COLOR = new Color(255, 216, 0);
  /**
   * Default foreground color.
   */
  public static final Color DEFAULT_FOREGROUND = Color.BLACK;
  /**
   * Default selection foreground color.
   */
  public static final Color DEFAULT_SELECTION_FOREGROUND = Color.WHITE;
  /**
   * Default background color.
   */
  public static final Color DEFAULT_BACKGROUND = Color.WHITE;
  /**
   * Default selection background color.
   */
  public static final Color DEFAULT_SELECTION_BACKGROUND = new Color(0, 0, 255);
  /**
   * Default foreground color.
   */
  public static final Color DIMMED_FOREGROUND = Color.GRAY;
  /**
   * Foreground color for non-managed nodes.
   */
  public static final Color UNMANAGED_COLOR = new Color(255, 28, 28);
  /**
   * Default non-selection background.
   */
  protected Color defaultNonSelectionBackground;
  /**
   * Default selection background.
   */
  protected Color defaultSelectionBackground;
  /**
   * Default selection background.
   */
  protected Color defaultSelectionForeground;
  /**
   * The default plain font.
   */
  protected static Font plainFont;
  /**
   * The default italic font.
   */
  protected static Font italicFont;
  /**
   * The default bold font.
   */
  protected static Font boldFont;
  /**
   * The default bold and italic font.
   */
  protected static Font boldItalicFont;

  /**
   * Default constructor.
   */
  public AbstractTreeCellRenderer() {
    defaultNonSelectionBackground = getBackgroundNonSelectionColor();
    defaultSelectionBackground = getBackgroundSelectionColor();
    defaultSelectionForeground = getTextSelectionColor();
    //plainFont = new Font(Font.SANS_SERIF, 12, Font.PLAIN);
    //italicFont = new Font(Font.SANS_SERIF, 12, Font.ITALIC);
  }

  /**
   * Get the default plain font.
   * @param font the font to base the result on.
   * @return a {@link Font} instance.
   */
  public static Font getPlainFont(final Font font) {
    if (plainFont == null) {
      plainFont = new Font(font.getName(), Font.PLAIN, font.getSize());
    }
    return plainFont;
  }

  /**
   * Get the default italic font.
   * @param font the font to base the result on.
   * @return a {@link Font} instance.
   */
  public static Font getItalicFont(final Font font) {
    if (italicFont == null) {
      italicFont = new Font(font.getName(), Font.ITALIC, font.getSize());
    }
    return italicFont;
  }

  /**
   * Get the default bold font.
   * @param font the font to base the result on.
   * @return a {@link Font} instance.
   */
  public static Font getBoldFont(final Font font) {
    if (boldFont == null) {
      boldFont = new Font(font.getName(), Font.BOLD, font.getSize());
    }
    return boldFont;
  }

  /**
   * Get the default bold and italic font.
   * @param font the font to base the result on.
   * @return a {@link Font} instance.
   */
  public static Font getBoldItalicFont(final Font font) {
    if (boldItalicFont == null) {
      boldItalicFont = new Font(font.getName(), Font.BOLD | Font.ITALIC, font.getSize());
    }
    return boldItalicFont;
  }

  /**
   * Determine whether IP addresses or host names are displayed.
   * @return {@code true} if IP addresses are displayed, {@code false} otherwise.
   */
  protected static boolean isShowIP() {
    return StatsHandler.getInstance().getShowIPHandler().isShowIP();
  }
}
