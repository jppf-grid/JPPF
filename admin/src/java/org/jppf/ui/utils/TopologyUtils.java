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

import java.util.*;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jppf.client.monitoring.topology.*;
import org.jppf.management.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Utility methods for manipulating the topology tree model.
 * @author Laurent Cohen
 */
public class TopologyUtils {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(TopologyUtils.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Base name of the resource bundles for localizing the system information messages.
   */
  private static final String SYSINFO_BASE = "org.jppf.ui.i18n.SystemInfoPage";

  /**
   * Add the specified driver to the treeTable.
   * @param model the tree table model.
   * @param driver the driver to add.
   * @return the newly created {@link DefaultMutableTreeNode}, if any.
   */
  public static synchronized DefaultMutableTreeNode addDriver(final AbstractJPPFTreeTableModel model, final TopologyDriver driver) {
    DefaultMutableTreeNode driverNode = null;
    if (!driver.getConnection().getStatus().isWorkingStatus()) return null;
    final String uuid = driver.getUuid();
    final DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    driverNode = TreeTableUtils.findComponent(treeTableRoot, uuid);
    if (driverNode == null) {
      final int index = TreeTableUtils.insertIndex(treeTableRoot, driver);
      if (index >= 0) {
        driverNode = new DefaultMutableTreeNode(driver);
        if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("adding driver: " + driver + " at index " + index);
        model.insertNodeInto(driverNode, treeTableRoot, index);
      }
    }
    return driverNode;
  }

  /**
   * Remove the specified driver from the treeTable.
   * @param model the tree table model.
   * @param driverData the driver to add.
   */
  public static synchronized void removeDriver(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData) {
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("removing driver: " + driverData);
    final DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final String uuid = driverData.getUuid();
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, uuid);
    if (driverNode == null) return;
    model.removeNodeFromParent(driverNode);
  }

  /**
   * Add the specified node to the specified driver in the treeTable.
   * @param model the tree table model.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   * @return the newly created {@link DefaultMutableTreeNode}, if any.
   */
  public static synchronized DefaultMutableTreeNode addNode(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return null;
    final DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if (driverNode == null) return null;
    final String nodeUuid = nodeData.getUuid();
    if (TreeTableUtils.findComponent(driverNode, nodeUuid) != null) return null;
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("attempting to add node={} to driver={}", nodeData, driverData);
    final int index = TreeTableUtils.insertIndex(driverNode, nodeData);
    if (index < 0) return null;
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("adding node: " + nodeUuid + " at index " + index);
    final DefaultMutableTreeNode nodeNode = new DefaultMutableTreeNode(nodeData);
    model.insertNodeInto(nodeNode, driverNode, index);
    return nodeNode;
  }

  /**
   * Remove the specified node from the specified driver in the treeTable.
   * @param model the tree table model.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  public static synchronized void removeNode(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData, final TopologyNode nodeData) {
    if ((driverData == null) || (nodeData == null)) return;
    if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("attempting to remove node=" + nodeData + " from driver=" + driverData);
    final DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final DefaultMutableTreeNode driver = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if (driver == null) return;
    final String nodeUuid = nodeData.getUuid();
    final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driver, nodeUuid);
    if (node != null) {
      if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug("removing node: " + nodeData);
      model.removeNodeFromParent(node);
    }
  }
 
  /**
   * Update the specified node.
   * @param model the tree table model.
   * @param driverData the driver to add to.
   * @param nodeData the node to add.
   */
  public static synchronized void updateNode(final AbstractJPPFTreeTableModel model, final TopologyDriver driverData, final TopologyNode nodeData) {
    final DefaultMutableTreeNode treeTableRoot = (DefaultMutableTreeNode) model.getRoot();
    final DefaultMutableTreeNode driverNode = TreeTableUtils.findComponent(treeTableRoot, driverData.getUuid());
    if ((driverNode != null) && (nodeData != null)) {
      final DefaultMutableTreeNode node = TreeTableUtils.findComponent(driverNode, nodeData.getUuid());
      if (node != null) model.changeNode(node);
    }
  }

  /**
   * COmpute a display name for the given topology component.
   * @param comp the ocmponent for which to get a display name.
   * @param showIP whether to show IP addresses vs. host names.
   * @return the display name as a string.
   */
  public static String getDisplayName(final AbstractTopologyComponent comp, final boolean showIP) {
    final JPPFManagementInfo info = comp.getManagementInfo();
    if (info != null) return (showIP ? info.getIpAddress() : info.getHost()) + ":" + info.getPort();
    return comp.getDisplayName();
  }

  /**
   * Retrieve the system information for the specified topology object.
   * @param data the topology object for which to get the information.
   * @return a {@link JPPFSystemInformation} or <code>null</code> if the information could not be retrieved.
   */
  public static JPPFSystemInformation retrieveSystemInfo(final AbstractTopologyComponent data) {
    JPPFSystemInformation info = null;
    try {
      if (data.isNode()) {
        final TopologyDriver parent = (TopologyDriver) data.getParent();
        final Map<String, Object> result = parent.getForwarder().systemInformation(new UuidSelector(data.getUuid()));
        final Object o = result.get(data.getUuid());
        if (o instanceof JPPFSystemInformation) info = (JPPFSystemInformation) o;
      } else {
        if (data.isPeer()) {
          final String uuid = ((TopologyPeer) data).getUuid();
          if (uuid != null) {
            final TopologyDriver driver = StatsHandler.getInstance().getTopologyManager().getDriver(uuid);
            if (driver != null) info = driver.getJmx().systemInformation();
          }
        }
        else info = ((TopologyDriver) data).getJmx().systemInformation();
      }
    } catch (final Exception e) {
      if (TreeTableUtils.debugEnabled) TreeTableUtils.log.debug(e.getMessage(), e);
    }
    return info;
  }

  /**
   * Print the specified system info to a string.
   * @param info the information to print.
   * @param format the formatter to use.
   * @param locale the locale to use.
   * @return a String with the formatted information.
   */
  public static String formatProperties(final JPPFSystemInformation info, final PropertiesTableFormat format, final Locale locale) {
    format.start();
    if (info == null) format.print(localizeSysInfo("system.info_not_found", locale));
    else {
      format.formatTable(info.getUuid(), localizeSysInfo("system.uuid", locale));
      format.formatTable(info.getSystem(), localizeSysInfo("system.system", locale));
      format.formatTable(info.getEnv(), localizeSysInfo("system.env", locale));
      format.formatTable(info.getRuntime(), localizeSysInfo("system.runtime", locale));
      format.formatTable(info.getJppf(), localizeSysInfo("system.jppf", locale));
      format.formatTable(info.getNetwork(), localizeSysInfo("system.network", locale));
      format.formatTable(info.getStorage(), localizeSysInfo("system.storage", locale));
      format.formatTable(info.getOS(), localizeSysInfo("system.os", locale));
      if (!info.getStats().isEmpty()) format.formatTable(info.getStats(), localizeSysInfo("system.stats", locale));
    }
    format.end();
    return format.getText();
  }

  /**
   * Generate the localized title for the system information popup dialog/window for a given topology component.
   * @param comp the the topology object for which to get the information.
   * @param locale the locale to display the title in.
   * @param showIP whether to show IP addresses vs. host names.
   * @return a localized string.
   */
  public static String getSystemInfoTitle(final AbstractTopologyComponent comp, final Locale locale, final boolean showIP) {
    return localizeSysInfo("system.info_for", locale) + " " +
      localizeSysInfo(comp.isNode() ? "system.node" : "system.driver", locale) + " " + getDisplayName(comp, showIP);
  }

  /**
   * Localize the specified key in the system information page.
   * @param key the key to localize.
   * @param locale the locale to use.
   * @return the localized string for the key.
   */
  static String localizeSysInfo(final String key, final Locale locale) {
    return LocalizationUtils.getLocalized(SYSINFO_BASE, key, locale);
  }
}
