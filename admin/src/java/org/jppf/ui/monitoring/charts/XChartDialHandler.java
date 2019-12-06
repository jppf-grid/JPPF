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

import java.util.Map;

import javax.swing.SwingUtilities;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;
import org.knowm.xchart.*;
import org.knowm.xchart.style.DialStyler;
import org.slf4j.*;

/**
 * Chart handler for dial charts.
 * @author Laurent Cohen
 */
public class XChartDialHandler extends AbstractXChartHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(XChartDialHandler.class);

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public XChartDialHandler(final StatsHandler statsHandler) {
    super(statsHandler);
  }

  @Override
  public ChartConfiguration createChart(final ChartConfiguration config) {
    final Object[] charts = new Object[config.fields.length];
    for (int i=0; i<config.fields.length; i++) {
      final DialChart chart = new DialChartBuilder().title(config.fields[i].getLocalizedName()).build();
      charts[i] = chart;
      final DialStyler styler = chart.getStyler();
      styler.setToolTipsEnabled(false);
      styler.setHasAnnotations(true);
      styler.setLegendVisible(false);
    }
    config.chart = charts;
    //populateDataset(config);
    return config;
  }

  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config) {
    final Object[] charts = (Object[]) config.chart;
    final Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
    for (int i=0; i<config.fields.length; i++) {
      final Fields field = config.fields[i];
      final DialChart chart = (DialChart) charts[i];
      double value = valueMap == null ? 0 : valueMap.get(field) / 100d;
      if (value < 0d) value = 0d;
      if (value > 1d) value = 1d;
      chart.addSeries(field.getLocalizedName(), value);
    }
    return config;
  }

  @Override
  public ChartConfiguration updateDataset(final ChartConfiguration config) {
    try {
      final Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
      final Object[] charts = (Object[]) config.chart;
      if (valueMap != null) {
        for (int i=0; i<config.fields.length; i++) {
          final Fields field = config.fields[i];
          final DialChart chart = (DialChart) charts[i];
          chart.removeSeries(field.getLocalizedName());
          double value = valueMap.get(field) / 100d;
          if (value < 0d) value = 0d;
          if (value > 1d) value = 1d;
          chart.addSeries(field.getLocalizedName(), value);
        }
      }
      if (config.chartPanel != null) SwingUtilities.invokeLater(() -> config.chartPanel.repaint());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return config;
  }
}
