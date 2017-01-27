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
package org.jppf.ui.utils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

import javax.swing.*;

import org.jppf.client.monitoring.topology.TopologyNode;
import org.jppf.management.*;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.treetable.AbstractTreeCellRenderer;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * Collection of GUI utility methods.
 * @author Laurent Cohen
 */
public final class GuiUtils {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(GuiUtils.class);
  /**
   * Path to the JPPF icon used in the GUI.
   */
  public static final String JPPF_ICON = "/org/jppf/ui/resources/jppf-icon.gif";
  /**
   * Default thickness for adjusted scrollbars.
   */
  public static final int DEFAULT_SCROLLBAR_THICKNESS = JPPFConfiguration.get(JPPFProperties.DEFAULT_SCROLLBAR_THICKNESS);
  /**
   * A mapping of icons to their path, to use as an icon cache.
   */
  private static Map<String, ImageIcon> iconMap = new Hashtable<>();
  /**
   * A mapping of mouse cursors to their name, to use as a cursor cache.
   */
  private static Map<String, Cursor> cursorMap = new Hashtable<>();
  /**
   * Precompiled pattern for searching line breaks in a string.
   */
  private static final Pattern TOOLTIP_PATTERN = Pattern.compile("\\n");
  /**
   * A mapping of words or phrases to abbreviated versions for use in the UI.
   */
  private static Map<String, String> shortenerMap = createShortener();
  static {
    GuiUtils.initNodeIcons();
  }

  /**
   * Create a chartPanel with a box layout with the specified orientation.
   * @param orientation the box orientation, one of {@link javax.swing.BoxLayout#Y_AXIS BoxLayout.Y_AXIS} or {@link javax.swing.BoxLayout#X_AXIS BoxLayout.X_AXIS}.
   * @return a <code>JPanel</code> instance.
   */
  public static JPanel createBoxPanel(final int orientation) {
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, orientation));
    return panel;
  }

  /**
   * Load and cache an icon from the file system or classpath.
   * @param path the path to the icon.
   * @return the loaded icon as an <code>ImageIcon</code> instance, or null if the icon
   *         could not be loaded.
   */
  public static ImageIcon loadIcon(final String path) {
    return loadIcon(path, true);
  }

  /**
   * Load and cache an icon from the file system or classpath.
   * @param path the path to the icon.
   * @param useCache specifies whether the icon should be retrieved from and/or put in the icon cache.
   * @return the loaded icon as an <code>ImageIcon</code> instance, or null if the icon could not be loaded.
   */
  public static ImageIcon loadIcon(final String path, final boolean useCache) {
    ImageIcon icon = null;
    if (useCache) {
      icon = iconMap.get(path);
      if (icon != null) return icon;
    }
    URL url = null;
    try {
      File file = new File(path);
      if (file.exists()) url = file.toURI().toURL();
      else url = GuiUtils.class.getResource(path);
      if (url == null) return null;
      icon = new ImageIcon(url);
      if (useCache) iconMap.put(path, icon);
    } catch (Exception e) {
      log.warn(e.getMessage(), e);
    }
    return icon;
  }

  /**
   * Put the specified icon in the cache wiht the specified path.
   * @param path the cache key for the icon.
   * @param icon the icon to cache.
   */
  public static void cacheIcon(final String path, final ImageIcon icon) {
    iconMap.put(path, icon);
  }

  /**
   * Create or get a custom mouse cursor.
   * @param name the name given to the cursor.
   * @param imagePath the path to the image.
   * @param hotspot the cursor's hotspot.
   * @return the create cursor, or {@code null} if it couldn't be created.
   */
  public static Cursor createCursor(final String name, final String imagePath, final Point hotspot) {
    Cursor cursor = cursorMap.get(name);
    if (cursor == null) {
      ImageIcon icon = loadIcon(imagePath);
      if (icon == null) return null;
      Image image = icon.getImage();
      if (image == null) return null;
      cursor = Toolkit.getDefaultToolkit().createCustomCursor(image, hotspot, name);
      if (cursor != null) cursorMap.put(name, cursor);
    }
    return cursor;
  }

  /**
   * Get a custom mouse cursor.
   * @param name the name given to the cursor.
   * @return a {@link Cursor} object, or {@code null} if it couldn't be retrieved.
   */
  public static Cursor getCursor(final String name) {
    return cursorMap.get(name);
  }

  /**
   * Create a copy of the specified image.
   * @param source the image to copy.
   * @return the copy of the image as a {@link BufferedImage} object.
   */
  private static Image imageCopy(final Image source) {
    BufferedImage copy = new BufferedImage(source.getWidth(null), source.getHeight(null), BufferedImage.TYPE_4BYTE_ABGR);
    copy.getGraphics().drawImage(source, 0, 0, null);
    return copy;
  }

  /**
   * Merge the specified images into one.All images are assumed to be of the same size.
   * @param source the source image, upon which all others will overwrite.
   * @param toMerge the images that overwrite the source, int he specified order.
   * @return a new ImageIcon whose image is the result of merging all specified images.
   */
  public static ImageIcon mergeIcons(final String source, final String...toMerge) {
    ImageIcon sourceIco = loadIcon(source);
    Image resImg = imageCopy(sourceIco.getImage());
    for (String s: toMerge) {
      Graphics g = resImg.getGraphics();
      ImageIcon ico = loadIcon(s);
      g.drawImage(ico.getImage(), 0, 0, null);
      g.dispose();
    }
    return new ImageIcon(resImg);
  }

  /**
   * Initialize the possible icons for the nodes.
   */
  public static void initNodeIcons() {
    for (boolean master: AbstractTreeCellRenderer.BOOL_VALUES) {
      for (boolean dotnet: AbstractTreeCellRenderer.BOOL_VALUES) {
        for (NodePendingAction action: NodePendingAction.class.getEnumConstants()) {
          StringBuilder sb = new StringBuilder("node");
          java.util.List<String> iconsToMerge = new ArrayList<>();
          if (master) {
            sb.append("-master");
            iconsToMerge.add(AbstractTreeCellRenderer.MARKER_MASTER_ICON);
          }
          if (dotnet) {
            sb.append("-dotnet");
            iconsToMerge.add(AbstractTreeCellRenderer.MARKER_DOTNET_ICON);
          }
          switch(action) {
            case SHUTDOWN:
              sb.append("-pending-shutdown");
              iconsToMerge.add(AbstractTreeCellRenderer.MARKER_PENDING_SHUTDOWN_ICON);
              break;
            case RESTART:
              iconsToMerge.add(AbstractTreeCellRenderer.MARKER_PENDING_RESTART_ICON);
              sb.append("-pending-restart");
              break;
          }
          ImageIcon img = mergeIcons(AbstractTreeCellRenderer.NODE_ICON, iconsToMerge.toArray(new String[iconsToMerge.size()]));
          cacheIcon(sb.toString(), img);
        }
      }
    }
  }

  /**
   * Compute the cache key for the specified node, to determine which icon to dispaly.
   * @param node the node for which to compute the key.
   * @return the icon cache key as a string.
   */
  public static String computeNodeIconKey(final TopologyNode node) {
    StringBuilder sb = new StringBuilder("node");
    JPPFManagementInfo info = node.getManagementInfo();
    if (info != null) {
      if (info.isMasterNode()) sb.append("-master");
      if (info.isDotnetCapable()) sb.append("-dotnet");
      switch(node.getPendingAction()) {
        case SHUTDOWN:
          sb.append("-pending-shutdown");
          break;
        case RESTART:
          sb.append("-pending-restart");
          break;
      }
    }
    return sb.toString();
  }

  /**
   * Create a filler component with the specified fixed size. The resulting component can be used as a
   * separator for layout purposes.
   * @param width the component's width.
   * @param height the component's height.
   * @return a <code>JComponent</code> instance.
   */
  public static JComponent createFiller(final int width, final int height) {
    JPanel filler = new JPanel();
    Dimension d = new Dimension(width, height);
    filler.setMinimumSize(d);
    filler.setMaximumSize(d);
    filler.setPreferredSize(d);
    return filler;
  }

  /**
   * Format a possibly multi-line text into a a string that can be properly displayed as a tooltip.
   * @param tooltip the non-formatted text of the tooltip.
   * @return the input text if it does not contain any line break, otherwise the input text wrapped in
   * &lt;html&gt; ... &lt;/html&gt; tags, with the line breaks transformed into &lt;br&gt; tags.
   */
  public static String formatToolTipText(final String tooltip) {
    if (tooltip == null) return null;
    String s = TOOLTIP_PATTERN.matcher(tooltip).replaceAll("<br>");
    return "<html>" + s + "</html>";
  }

  /**
   * Retrieve the top frame ro which a component belongs.
   * @param comp the component whose frame to retrieve.
   * @return a {@link Frame} instance if it can be found, null otherwise.
   */
  public static Frame getTopFrame(final Component comp) {
    Component tmp = SwingUtilities.getRoot(comp);
    return (tmp instanceof Frame) ? (Frame) tmp : null;
  }

  /**
   * Replace pre-determined keywords in a string, with shorter ones.
   * @param key the string to shorten.
   * @return the string with its keywords replaced.
   */
  public static String shortenLabel(final String key) {
    String[] words = RegexUtils.SPACES_PATTERN.split(key);
    StringBuilder sb = new StringBuilder();
    int count = 0;
    for (String word : words) {
      String result = shortenerMap.get(word);
      if (result == null) result = word;
      sb.append(result);
      if ((count < words.length - 1) && !"".equals(result)) sb.append(' ');
      count++;
    }
    return sb.toString();
  }

  /**
   * Create a map to shorten labels in the charts.
   * @return a map of keyword to shorter replacements.
   */
  private static Map<String, String> createShortener() {
    Map<String, String> map = new HashMap<>();
    map.put("Execution", "Exec");
    map.put("execution", "exec");
    map.put("Maximum", "Max");
    map.put("maximum", "max");
    map.put("Minimum", "Min");
    map.put("minimum", "min");
    map.put("Average", "Avg");
    map.put("average", "avg");
    map.put("Cumulated", "Cumul.");
    map.put("cumulated", "cumul.");
    map.put("Number", "Nb");
    map.put("number", "nb");
    map.put("Of", "");
    map.put("of", "");
    return map;
  }

  /**
   * Execute the specified runnable in a new thread.
   * @param r the <code>Runnable</code> to execute.
   * @param name the thread name.
   */
  public static void runAction(final Runnable r, final String name) {
    new Thread(r, name).start();
  }

  /**
   * Get the component used in the tab to which the specified option belongs.
   * @param option the option for which to find the tab component.
   * @return the tab component, or {@code null} if the option is not in a tab.
   */
  public static Component getTabComponent(final OptionElement option) {
    Component comp = option.getUIComponent();
    while ((comp != null) && (comp.getParent() != null)) {
      Component parent = comp.getParent();
      if (parent instanceof JTabbedPane) {
        JTabbedPane tabbedPane = (JTabbedPane) parent;
        int index = tabbedPane.indexOfComponent(comp);
        Component tabComp = tabbedPane.getTabComponentAt(index);
        if (tabComp instanceof JLabel) return tabComp;
        break;
      }
      comp = parent;
    }
    return null;
  }

  /**
   * Change the thickness of the horizontal and vertical scrollbars in the specified scroll pane to the {@link #DEFAULT_SCROLLBAR_THICKNESS default value}.
   * @param scrollPane the scroll pane to change.
   */
  public static void adjustScrollbarsThickness(final JScrollPane scrollPane) {
    adjustScrollbarsThickness(scrollPane, DEFAULT_SCROLLBAR_THICKNESS);
  }

  /**
   * Change the thickness of the horizontal and vertical scrollbars in the specified scroll pane.
   * @param scrollPane the scroll pane to change.
   * @param thickness the desried scrollbar thickness.
   */
  public static void adjustScrollbarsThickness(final JScrollPane scrollPane, final int thickness) {
    scrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(thickness, 0));
    scrollPane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, thickness));
  }
}
