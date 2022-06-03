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

package org.jppf.ui.monitoring.diagnostics;

import java.util.*;

import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.management.diagnostics.*;
import org.jppf.management.diagnostics.provider.MonitoringValueConverter;
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
   * Mapping of the property names to an optionally associated value converter.
   */
  private final Map<String, MonitoringValueConverter> converters;

  /**
   * Initialize this model with the specified tree and locale.
   * @param node the root of the tree.
   * @param locale the locale to use for translation.
   */
  public JVMHealthTreeTableModel(final TreeNode node, final Locale locale) {
    super(node, locale);
    i18nBase = "org.jppf.ui.i18n.NodeDataPage";
    properties = MonitoringDataProviderHandler.getAllProperties();
    converters = new HashMap<>(MonitoringDataProviderHandler.getConverters());
  }

  @Override
  public int getColumnCount() {
    return 1 + properties.size();
  }

  @Override
  public Object getValueAt(final Object node, final int column) {
    String res = "";
    if (node instanceof DefaultMutableTreeNode) {
      final DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof AbstractTopologyComponent) {
        final AbstractTopologyComponent info = (AbstractTopologyComponent) defNode.getUserObject();
        final HealthSnapshot health = info.getHealthSnapshot();
        if (health == null) return res;
        if (column == URL) res = info.getDisplayName();
        else {
          final JPPFProperty<?> prop = properties.get(column - 1);
          final String name = prop.getName();
          final MonitoringValueConverter converter = converters.get(name);
          if (converter != null) res = converter.convert(health.getString(name));
          else if ((prop instanceof FloatProperty) || (prop instanceof DoubleProperty)) {
            final double d = health.getDouble(name);
            res = d < 0d ? NA : nfDec.format(d);
          } else if ((prop instanceof IntProperty) || (prop instanceof LongProperty)) {
            final long l = health.getLong(name);
            res = l < 0L ? NA : nfInt.format(l);
          } else res = health.getString(name);
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
    return properties.get(column - 1).getShortLabel(locale);
  }

  @Override
  public String getColumnTooltip(final int column) {
    if ((column < 0) && (column > getColumnCount())) return "";
    if (column == URL) return localize(getBaseColumnName(column) + ".label");
    String s = properties.get(column - 1).getDocumentation(locale);
    if (s == null) s = "";
    if (s.contains("\n")) s = "<html>" + s.replace("\n", "<br>") + "</html>";
    return s;
  }
}
