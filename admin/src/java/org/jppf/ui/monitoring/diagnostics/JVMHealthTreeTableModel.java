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

package org.jppf.ui.monitoring.diagnostics;

import java.util.*;

import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.management.diagnostics.*;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;
import org.jppf.utils.configuration.*;

/**
 * Tree table model for the tree table.
 */
public class JVMHealthTreeTableModel extends AbstractJPPFTreeTableModel {
  /**
   * Value of one megabyte.
   */
  private static final String NA = "n/a";
  /**
   * Column number for the driver or node's url.
   */
  public static final int URL = 0;
  /**
   * Properties representing the columns definitions.
   */
  private final List<JPPFProperty<?>> properties;

  /**
   * Initialize this model with the specified tree and locale.
   * @param node the root of the tree.
   * @param locale the locale to use for translation.
   * @param handler holds the defintions of the data rendered in the view.
   */
  public JVMHealthTreeTableModel(final TreeNode node, final Locale locale, final MonitoringDataProviderHandler handler) {
    super(node, locale);
    i18nBase = "org.jppf.ui.i18n.NodeDataPage";
    properties = handler.getPropertyList();
  }

  @Override
  public int getColumnCount() {
    return 1 + properties.size();
  }

  @Override
  public Object getValueAt(final Object node, final int column) {
    Object res = "";
    if (node instanceof DefaultMutableTreeNode) {
      final DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof AbstractTopologyComponent) {
        final AbstractTopologyComponent info = (AbstractTopologyComponent) defNode.getUserObject();
        final HealthSnapshot health = info.getHealthSnapshot();
        double d = -1d;
        if (health == null) return res;
        if (column == URL) res = info.toString();
        else {
          final JPPFProperty<?> prop = properties.get(column - 1);
          if (prop instanceof NumberProperty) {
            d = health.getDouble(prop.getName());
            res = d < 0d ? NA : nfDec.format(d);
          } else res = health.getString(prop.getName());
        }
      } else if (column == 0) res = defNode.getUserObject().toString();
    }
    return res;
  }

  @Override
  public String getBaseColumnName(final int column) {
    if (column == URL) return "column.health.url";
    return properties.get(column - 1).getName();
  }

  /**
   * @return the properties representing the columns definitions.
   */
  public List<JPPFProperty<?>> getProperties() {
    return properties;
  }

  @Override
  public String getColumnName(final int column) {
    if ((column < 0) && (column > getColumnCount())) return "";
    if (column == URL) return localize(getBaseColumnName(column) + ".label");
    return properties.get(column - 1).getShortLabel();
  }

  @Override
  public String getColumnTooltip(final int column) {
    if ((column < 0) && (column > getColumnCount())) return "";
    if (column == URL) return localize(getBaseColumnName(column) + ".label");
    final String s = properties.get(column - 1).getDocumentation();
    return (s == null) ? "" : s;
  }
}
