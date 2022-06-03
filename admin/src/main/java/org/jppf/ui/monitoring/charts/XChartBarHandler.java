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
import java.util.*;

import javax.swing.SwingUtilities;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;
import org.knowm.xchart.*;
import org.knowm.xchart.CategorySeries.CategorySeriesRenderStyle;
import org.knowm.xchart.style.CategoryStyler;
import org.knowm.xchart.style.Styler.*;
import org.slf4j.*;

/**
 * Chart handler for bar charts and possible variations.
 * @author Laurent Cohen
 */
public class XChartBarHandler extends AbstractXChartHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(XChartBarHandler.class);
  /**
   * The chart's rendering style.
   */
  private final CategorySeriesRenderStyle style;
  /**
   * Whether tre series should be rendered as stacked.
   */
  private final boolean stacked;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   * @param style the chart's rendering style.
   * @param stacked whether tre series should be rendered as stacked.
   */
  public XChartBarHandler(final StatsHandler statsHandler, final CategorySeriesRenderStyle style, final boolean stacked) {
    super(statsHandler);
    this.style = style;
    this.stacked = stacked;
  }

  @Override
  public ChartConfiguration createChart(final ChartConfiguration config) {
    final CategoryChart chart = new CategoryChartBuilder().build();
    config.chart = chart;
    final CategoryStyler styler = chart.getStyler();
    styler.setDefaultSeriesRenderStyle(style);
    styler.setXAxisTicksVisible(false);
    styler.setMarkerSize(0);
    styler.setPlotMargin(1);
    styler.setChartPadding(2);
    styler.setLegendVisible(true);
    styler.setLegendLayout(LegendLayout.Horizontal);
    styler.setLegendPosition(LegendPosition.OutsideS);
    styler.setAxisTickLabelsFont(getFont(styler.getAxisTickLabelsFont(), Font.PLAIN));
    styler.setStacked(stacked);

    createDataset(config);
    final BasicStroke[] strokes = new BasicStroke[config.fields.length];
    Arrays.fill(strokes, DEFAULT_LINE_STROKE);
    styler.setSeriesLines(strokes);
    chart.setTitle(getTitle(config));

    return config;
  }

  /**
   * Create and populate a dataset with the values of the specified fields.
   * @param config the names of the fields whose values populate the dataset.
   * @return a <code>DefaultCategoryDataset</code> instance.
   */
  private Object createDataset(final ChartConfiguration config) {
    final CategoryChart chart = (CategoryChart) config.chart;
    for (final Fields key: config.fields) {
      chart.addSeries(key.getLocalizedName(), new double[1], new double[1]);
    }
    populateDataset(config);
    return null;
  }

  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config) {
    try {
      final ChartData data = getSeriesData(config);
      final CategoryChart chart = (CategoryChart) config.chart;
      if (chart == null) return config;
      for (final Fields field: config.fields) {
        try {
          chart.updateCategorySeries(field.getLocalizedName(), data.x, data.values.get(field), null);
        } catch (final Exception e) {
          log.error("exception for {}: ", config.toDebugString(), e);
        }
      }
      if (config.chartPanel != null) SwingUtilities.invokeLater(() -> config.chartPanel.repaint());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return config;
  }
}
