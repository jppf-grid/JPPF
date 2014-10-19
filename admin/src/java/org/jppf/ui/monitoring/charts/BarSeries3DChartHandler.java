/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import static org.jppf.utils.ReflectionHelper.*;

import java.util.Map;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;
import org.slf4j.*;

/**
 * Instances of this class are used to create and update line charts with an horizontal orientation.
 * @author Laurent Cohen
 * @since 5.0
 */
public class BarSeries3DChartHandler implements ChartHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(BarSeries3DChartHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The stats formatter that provides the data.
   */
  protected final StatsHandler statsHandler;
  /**
   * The name of the chart factory method to call.
   */
  protected final String chartMethodName;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public BarSeries3DChartHandler(final StatsHandler statsHandler) {
    this(statsHandler,  "createBarChart3D");
  }

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   * @param chartMethodName the name of the chart factory method to call.
   */
  protected BarSeries3DChartHandler(final StatsHandler statsHandler, final String chartMethodName) {
    this.statsHandler = statsHandler;
    this.chartMethodName = chartMethodName;
  }

  /**
   * Create a plot XY chart based on a chart configuration.
   * @param config holds the configuration parameters for the chart created, modified by this method.
   * @return a <code>ChartConfiguration</code> instance.
   */
  @Override
  public ChartConfiguration createChart(final ChartConfiguration config) {
    Object ds = createDataset(config);
    String s = config.name;
    if (config.unit != null) s += " (" + config.unit+ ')';
    //JFreeChart chart = ChartFactory.createBarChart3D(s, null, null, ds, PlotOrientation.VERTICAL, true, true, false);
    Object chart = invokeMethod(getClass0("org.jfree.chart.ChartFactory"), null, chartMethodName, s, null, null, ds, getField("org.jfree.chart.plot.PlotOrientation", "VERTICAL"), true, true, false);
    Object plot = invokeMethod(chart.getClass(), chart, "getCategoryPlot");
    //CategoryAxis axis = plot.getDomainAxis();
    Object axis = invokeMethod(plot.getClass(), plot, "getDomainAxis");
    //axis.setTickLabelsVisible(false);
    invokeMethod(axis.getClass(), axis, "setTickLabelsVisible", false);
    config.chart = chart;
    return config;
  }

  /**
   * Create and populate a dataset with the values of the specified fields.
   * @param config the names of the fields whose values populate the dataset.
   * @return a <code>DefaultCategoryDataset</code> instance.
   */
  private Object createDataset(final ChartConfiguration config) {
    //DefaultCategoryDataset ds = new DefaultCategoryDataset();
    Object ds = newInstance("org.jfree.data.category.DefaultCategoryDataset");
    config.dataset = ds;
    populateDataset(config);
    return ds;
  }

  /**
   * Populate a dataset based on a chart configuration.
   * @param config the chart configuration containing the dataset to populate.
   * @return a <code>ChartConfiguration</code> instance.
   */
  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config) {
    Object ds = config.dataset;
    //ds.clear();
    invokeMethod(ds.getClass(), ds, "clear");
    ConnectionDataHolder cdh = statsHandler.getCurrentDataHolder();
    if (cdh == null) return config;
    int statsCount = cdh.getDataList().size();
    if (debugEnabled) log.debug("data holder for {} has {} snapshots", statsHandler.getClientHandler().getCurrentDriver(), statsCount);
    int start = Math.max(0, statsHandler.getTickCount() - statsCount);
    int count = 0;
    for (Map<Fields, Double> valueMap: cdh.getDoubleValuesMaps()) {
      count++;
      for (Fields key: config.fields) {
        //ds.setValue(valueMap.get(key), key, Integer.valueOf(j + start));
        invokeMethod(ds.getClass(), ds, "setValue", valueMap.get(key), key, Integer.valueOf(count + start));
      }
    }
    /*
    for (int j=0; j<statsCount; j++) {
      Map<Fields, Double> valueMap = statsHandler.getDoubleValues(j);
      for (Fields key: config.fields) {
        //ds.setValue(valueMap.get(key), key, Integer.valueOf(j + start));
        invokeMethod(ds.getClass(), ds, "setValue", valueMap.get(key), key, Integer.valueOf(j + start));
      }
    }
    */
    return config;
  }

  /**
   * Update a dataset based on a chart configuration.
   * @param config the chart configuration containing the dataset to update.
   * @return a <code>ChartConfiguration</code> instance.
   * @see org.jppf.ui.monitoring.charts.ChartHandler#updateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
   */
  @Override
  public ChartConfiguration updateDataset(final ChartConfiguration config) {
    Object ds = config.dataset;
    Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
    if (valueMap != null) {
      for (Fields key: config.fields) {
        //ds.setValue(valueMap.get(key), key, Integer.valueOf(statsHandler.getTickCount()));
        invokeMethod(ds.getClass(), ds, "setValue", valueMap.get(key), key, Integer.valueOf(statsHandler.getTickCount()));
      }
    }
    //if (ds.getColumnCount() > statsHandler.getRolloverPosition())
    if ((Integer) invokeMethod(ds.getClass(), ds, "getColumnCount") > statsHandler.getRolloverPosition()) {
      //ds.removeColumn(0);
      invokeMethod(ds.getClass(), ds, "removeColumn", new Class[] {int.class}, 0);
    }
    return config;
  }
}
