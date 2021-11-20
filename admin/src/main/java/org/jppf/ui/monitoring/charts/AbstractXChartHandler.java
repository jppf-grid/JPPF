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
package org.jppf.ui.monitoring.charts;

import java.awt.*;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;

/**
 * Instances of this class are used to define charts and update their data.
 * @author Laurent Cohen
 */
public abstract class AbstractXChartHandler implements ChartHandler {
  /**
   * The default line shape for the plot.
   */
  static final BasicStroke DEFAULT_LINE_STROKE = new BasicStroke(1f);
  /**
   * The stats formatter that provides the data.
   */
  StatsHandler statsHandler;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public AbstractXChartHandler(final StatsHandler statsHandler) {
    this.statsHandler = statsHandler;
  }

  @Override
  public ChartConfiguration updateDataset(final ChartConfiguration config) {
    populateDataset(config);
    return config;
  }

  /**
   * Get a new font from the specified source font, with the same name and size and with the specified style. 
   * @param source the source font to use.
   * @param style the style to set on the new font.
   * @return a new {@link Font} instance.
   */
  static Font getFont(final Font source, final int style) {
    return new Font(source.getName(), style, source.getSize());
  }

  /**
   * Build the chart title from its configuration.
   * @param config the chart configuration.
   * @return the computed chart name.
   */
  static String getTitle(final ChartConfiguration config) {
    String s = config.name;
    if (config.unit != null) s += " (" + config.unit + ')';
    return s;
  }

  /**
   * Get the chart data from its configuration.
   * @param config the chart confiiguration.
   * @return a {@link ChartData} whcih contains the x-axis data ant the time series for all the charts fields.
   */
  ChartData getSeriesData(final ChartConfiguration config) {
    final int start = Math.max(0, statsHandler.getTickCount() - statsHandler.getStatsCount());
    final ChartData data = new ChartData();
    data.values = ChartDataCache.getInstance().getData(config.fields);
    int maxCount = 0;
    for (final Fields field: config.fields) {
      java.util.List<Double> values = data.values.get(field);
      if (values == null) {
        values = new java.util.LinkedList<>();
        data.values.put(field, values);
      }
      final int n = values.size();
      if (n > maxCount) maxCount = n;
    }
    data.x = new java.util.ArrayList<>(maxCount == 0 ? 1 : maxCount);
    for (int i=0; i<maxCount; i++) data.x.add((double) (start + i));
    for (final Fields field: config.fields) {
      final java.util.LinkedList<Double> values = (java.util.LinkedList<Double>) data.values.get(field);
      final int n = values.size();
      if (n < maxCount) {
        for (int i=0; i < maxCount - n; i++) values.addFirst(0d);
      } else if (n > maxCount) {
        for (int i=0; i < maxCount - n; i++) values.poll();
      }
    }
    return data;
  }
}
