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
package org.jppf.ui.monitoring;

import static org.jppf.utils.stats.JPPFStatisticsHelper.createServerStatistics;

import java.util.*;

import javax.swing.table.AbstractTableModel;

import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.ui.monitoring.data.*;
import org.jppf.ui.utils.GuiUtils;

/**
 * Data model for the tables displaying the values.
 */
class MonitorTableModel extends AbstractTableModel {
  /**
   * Default values to use when the driver connection is no longer available.
   */
  private static final Map<Fields, String> EMPTY_VALUES = initEmptyValues();
  /**
   * The list of fields whose values are displayed in the table.
   */
  private Fields[] fields = null;

  /**
   * Initialize this table model with the specified list of fields.
   * @param fields the list of fields whose values are displayed in the table.
   */
  MonitorTableModel(final Fields[] fields) {
    this.fields = fields;
  }

  /**
   * Get the number of columns in the table.
   * @return 2.
   */
  @Override
  public int getColumnCount() {
    return 2;
  }

  /**
   * Get the number of rows in the table.
   * @return the number of fields displayed in the table.
   */
  @Override
  public int getRowCount() {
    return fields.length;
  }

  /**
   * Get a value at specified coordinates in the table.
   * @param row the row coordinate.
   * @param column the column coordinate.
   * @return the value as an object.
   */
  @Override
  public Object getValueAt(final int row, final int column) {
    Fields field = fields[row];
    if (column == 0) return GuiUtils.shortenLabel(field.toString());
    StatsHandler handler = StatsHandler.getInstance();
    if (handler.getStatsCount() <= 0) return EMPTY_VALUES;
    return handler.formatLatestValue(Locale.getDefault(), handler.getCurrentDataHolder().getDriver(), field);
  }

  /**
   * 
   * @return .
   */
  private static Map<Fields, String> initEmptyValues() {
    StatsTransformer t = new StatsTransformer();
    Map<Fields, Double> values = t.formatDoubleValues(createServerStatistics(), new HealthSnapshot());
    return new StatsFormatter(Locale.getDefault()).formatValues(values);
  }
}
