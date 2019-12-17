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
import org.knowm.xchart.XYSeries.XYSeriesRenderStyle;
import org.knowm.xchart.style.Styler.*;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.markers.SeriesMarkers;
import org.slf4j.*;

/**
 * Chart handler for plot xy charts and possible variations.
 * @author Laurent Cohen
 */
public class XChartPlotXYHandler extends AbstractXChartHandler {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(XChartPlotXYHandler.class);
  /**
   * The chart's rendering style.
   */
  private final XYSeriesRenderStyle style;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   * @param style .
   */
  public XChartPlotXYHandler(final StatsHandler statsHandler, final XYSeriesRenderStyle style) {
    super(statsHandler);
    this.style = style;
  }

  @Override
  public ChartConfiguration createChart(final ChartConfiguration config) {
    final XYChart chart = new XYChartBuilder().title(getTitle(config)).build();
    config.chart = chart;
    final XYStyler styler = chart.getStyler();
    //styler.setSeriesColors(new BaseSeriesColors().getSeriesColors());
    styler.setDefaultSeriesRenderStyle(style);
    styler.setXAxisTicksVisible(false);
    styler.setMarkerSize(style == XYSeriesRenderStyle.Scatter ? 3 : 0);
    styler.setPlotMargin(1);
    styler.setChartPadding(2);
    styler.setLegendVisible(true);
    styler.setLegendLayout(LegendLayout.Horizontal);
    styler.setLegendPosition(LegendPosition.OutsideS);
    styler.setAxisTickLabelsFont(getFont(styler.getAxisTickLabelsFont(), Font.PLAIN));
    final BasicStroke[] strokes = new BasicStroke[config.fields.length];
    Arrays.fill(strokes, DEFAULT_LINE_STROKE);
    styler.setSeriesLines(strokes);

    final Map<Fields, XYSeries> dataset = new LinkedHashMap<>();
    int count = 0;
    for (final Fields key: config.fields) {
      XYSeries series;
      if (count == 0) series = chart.addSeries(key.getLocalizedName(), new double[1], new double[1]);
      else series = chart.addSeries(key.getLocalizedName(), new double[1]);
      dataset.put(key, series);
      if (style == XYSeriesRenderStyle.Scatter) {
        series.setMarker(SeriesMarkers.CIRCLE);
        series.setShowInLegend(true);
      }
      count++;
    }
    config.dataset = dataset;
    populateDataset(config);

    return config;
  }

  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config) {
    try {
      final XYChart chart = (XYChart) config.chart;
      if (chart == null) return config;
      final ChartData data = getSeriesData(config);
      for (final Fields field: config.fields) {
        final String name = field.getLocalizedName();
        try {
          chart.updateXYSeries(name, data.x, data.values.get(field), null);
        } catch (final Exception e) {
          log.error("exception for {}: ", config.toDebugString(), e);
          log.error("field = {}, name = {}, data = {}], chart = {}", field, name, data, chart);
        }
      }
      if (config.chartPanel != null) SwingUtilities.invokeLater(() -> config.chartPanel.repaint());
    } catch (final Exception e) {
      log.error(e.getMessage(), e);
    }
    return config;
  }
}
