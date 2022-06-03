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
import org.knowm.xchart.PieSeries.PieSeriesRenderStyle;
import org.knowm.xchart.style.PieStyler;
import org.knowm.xchart.style.Styler.*;
import org.knowm.xchart.style.colors.BaseSeriesColors;

/**
 * Chart handler for pier charts and possible variations.
 * @author Laurent Cohen
 */
public class XChartPieHandler extends AbstractXChartHandler {
  /**
   * The pie's rendering style.
   */
  private final PieSeriesRenderStyle style;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   * @param style the pie rendering style.
   */
  public XChartPieHandler(final StatsHandler statsHandler, final PieSeriesRenderStyle style) {
    super(statsHandler);
    this.style = style;
  }

  @Override
  public ChartConfiguration createChart(final ChartConfiguration config) {
    final PieChart chart = new PieChartBuilder().title(getTitle(config)).build();
    config.chart = chart;
    final PieStyler styler = chart.getStyler();
    styler.setLegendVisible(true);
    styler.setLegendPosition(LegendPosition.OutsideS);
    styler.setLegendLayout(LegendLayout.Horizontal);
    styler.setDefaultSeriesRenderStyle(style);
    styler.setSeriesColors(new BaseSeriesColors().getSeriesColors());
    styler.setSumVisible(true);
    styler.setSumFontSize(20f);

    populateDataset(config);
    return config;
  }

  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config) {
    final PieChart chart = (PieChart) config.chart;
    for (final Fields key: config.fields) chart.addSeries(key.getLocalizedName(), 0);
    return updateDataset(config);
  }

  @Override
  public ChartConfiguration updateDataset(final ChartConfiguration config) {
    final PieChart chart = (PieChart) config.chart;
    if (chart == null) return config;
    final Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
    if (valueMap != null) {
      for (final Fields key: config.fields) chart.updatePieSeries(key.getLocalizedName(), valueMap.get(key));
    }
    if (config.chartPanel != null) SwingUtilities.invokeLater(() -> config.chartPanel.repaint());
    return config;
  }
}
