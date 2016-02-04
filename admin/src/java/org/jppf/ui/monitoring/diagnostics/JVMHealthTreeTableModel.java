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

package org.jppf.ui.monitoring.diagnostics;

import java.text.NumberFormat;

import javax.swing.tree.*;

import org.jppf.client.monitoring.topology.AbstractTopologyComponent;
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.treetable.AbstractJPPFTreeTableModel;

/**
 * Tree table model for the tree table.
 */
public class JVMHealthTreeTableModel extends AbstractJPPFTreeTableModel {
  /**
   * Value of one megabyte.
   */
  private static final long MB = 1024L * 1024L;
  /**
   * Value of one megabyte.
   */
  private static final String NA = "n/a";
  /**
   * Column number for the driver or node's url.
   */
  static final int URL = 0;
  /**
   * Column number for the heap memory usage in percentage.
   */
  static final int HEAP_MEM_PCT = 1;
  /**
   * Column number for the heap usage in MB.
   */
  static final int HEAP_MEM_MB = 2;
  /**
   * Column number for the non-heap memory usage in percentage.
   */
  static final int NON_HEAP_MEM_PCT = 3;
  /**
   * Column number for the non-heap memory usage in percentage.
   */
  static final int NON_HEAP_MEM_MB = 4;
  /**
   * Column number for the RAM usage in percentage.
   */
  static final int RAM_PCT = 5;
  /**
   * Column number for the RAM usage in MB.
   */
  static final int RAM_MB = 6;
  /**
   * Column number for the node's last event.
   */
  static final int THREADS = 7;
  /**
   * Column number for the process CPU load.
   */
  static final int CPU_LOAD = 8;
  /**
   * Column number for the process CPU load.
   */
  static final int SYSTEM_CPU_LOAD = 9;
  /**
   * A number formatter for the used memory %.
   */
  NumberFormat nf = createNumberFormat();

  /**
   * Initialize this model with the specified tree.
   * @param node the root of the tree.
   */
  public JVMHealthTreeTableModel(final TreeNode node) {
    super(node);
    BASE = "org.jppf.ui.i18n.NodeDataPage";
  }

  @Override
  public int getColumnCount() {
    return 10;
  }

  @Override
  public Object getValueAt(final Object node, final int column) {
    Object res = "";
    if (node instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode defNode = (DefaultMutableTreeNode) node;
      if (defNode.getUserObject() instanceof AbstractTopologyComponent) {
        AbstractTopologyComponent info = (AbstractTopologyComponent) defNode.getUserObject();
        HealthSnapshot health = info.getHealthSnapshot();
        double d = -1d;
        if (health == null) return res;
        switch (column) {
          case URL:
            res = info.toString();
            break;
          case HEAP_MEM_PCT:
            d = health.getHeapUsedRatio();
            res = d < 0d ? NA : nf.format(d * 100d) + " %";
            break;
          case HEAP_MEM_MB:
            d = health.getHeapUsed();
            res = d < 0d ? NA : nf.format(d / MB);
            break;
          case NON_HEAP_MEM_PCT:
            d = health.getNonheapUsedRatio();
            res = d < 0d ? NA : nf.format(d * 100d) + " %";
            break;
          case NON_HEAP_MEM_MB:
            d = health.getNonheapUsed();
            res = d < 0d ? NA : nf.format(d / MB);
            break;
          case RAM_PCT:
            d = health.getRamUsedRatio();
            res = d < 0d ? NA : nf.format(d * 100d) + " %";
            break;
          case RAM_MB:
            d = health.getRamUsed();
            res = d < 0d ? NA : nf.format(d / MB);
            break;
          case THREADS:
            int n = health.getLiveThreads();
            res = n < 0 ? NA : Integer.toString(n);
            break;
          case CPU_LOAD:
            d = health.getCpuLoad();
            res = d < 0d ? NA : nf.format(d * 100d) + " %";
            break;
          case SYSTEM_CPU_LOAD:
            d = health.getSystemCpuLoad();
            res = d < 0d ? NA : nf.format(d * 100d) + " %";
            break;
        }
      } else {
        if (column == 0) res = defNode.getUserObject().toString();
      }
    }
    return res;
  }

  @Override
  public String getBaseColumnName(final int column) {
    switch (column) {
      case URL:
        return "column.health.url";
      case HEAP_MEM_PCT:
        return "column.health.heap.pct";
      case HEAP_MEM_MB:
        return "column.health.heap.mb";
      case NON_HEAP_MEM_PCT:
        return "column.health.nonheap.pct";
      case NON_HEAP_MEM_MB:
        return "column.health.nonheap.mb";
      case RAM_PCT:
        return "column.health.ram.pct";
      case RAM_MB:
        return "column.health.ram.mb";
      case THREADS:
        return "column.health.livethreads";
      case CPU_LOAD:
        return "column.health.cpuload";
      case SYSTEM_CPU_LOAD:
        return "column.health.systemCpuload";
    }
    return "";
  }

  /**
   * Get a number formatter for the used memory %.
   * @return a <code>NumberFormat</code> instance.
   */
  private NumberFormat createNumberFormat() {
    NumberFormat nf = NumberFormat.getInstance();
    nf.setGroupingUsed(true);
    nf.setMaximumFractionDigits(1);
    nf.setMinimumFractionDigits(1);
    return nf;
  }
}
