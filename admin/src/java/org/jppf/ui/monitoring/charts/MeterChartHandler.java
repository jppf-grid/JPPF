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
package org.jppf.ui.monitoring.charts;

import static org.jppf.utils.ReflectionHelper.*;

import java.awt.Color;
import java.util.Map;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;

/**
 * Instances of this class are used to create and update 3D pie charts.
 * @author Laurent Cohen
 * @since 5.0
 */
public class MeterChartHandler implements ChartHandler {
  /**
   * The stats formatter that provides the data.
   */
  protected StatsHandler statsHandler = null;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public MeterChartHandler(final StatsHandler statsHandler) {
    this.statsHandler = statsHandler;
  }

  /**
   * Create a plot XY chart based on a chart configuration.
   * @param config holds the configuration parameters for the chart created, modified by this method.
   * @return a <code>ChartConfiguration</code> instance.
   */
  @Override
  public ChartConfiguration createChart(final ChartConfiguration config) {
    if ((config.fields == null) || (config.fields.length <= 0)) return config;
    Object[] charts = new Object[config.fields.length];
    config.chart = charts;
    Object[] plots = new Object[config.fields.length];
    Class<?> plotClass = getClass0("org.jfree.chart.plot.MeterPlot");
    for (int i=0; i<config.fields.length; i++) {
      // plots[i] = new MeterPlot();
      plots[i] = newInstance(plotClass);
    }
    config.params.put("plot", plots);
    Object[] ds = (Object[]) createDataset(config);

    for (int i=0; i<config.fields.length; i++) {
      // plots[i].setDataset(ds);
      invokeMethod(plotClass, plots[i], "setDataset", ds[i]);
      // plots[i].setUnits("%");
      invokeMethod(plotClass, plots[i], "setUnits", config.unit == null ? "" : config.unit);
      // plots[i].setDialBackgroundPaint(COLOR.GRAY);
      invokeMethod(plotClass, plots[i], "setDialBackgroundPaint", Color.GRAY);
      // plots[i].setNeedlePaint(COLOR.GREEN);
      invokeMethod(plotClass, plots[i], "setNeedlePaint", new Color(0, 255, 0, 128));
      // charts[i] = new JFreeChart(config.name, plots[i]);
      Class<?>[] paramTypes = {String.class, getClass0("org.jfree.chart.plot.Plot")};
      charts[i] = invokeConstructor(getClass0("org.jfree.chart.JFreeChart"), paramTypes, config.fields[i].toString(), plots[i]);
      // charts[i].setBackgroundPaint(Color.WHITE);
      invokeMethod(getClass0("org.jfree.chart.JFreeChart"), charts[i], "setBackgroundPaint", Color.WHITE );
    }
    return config;
  }

  /**
   * Create and populate a dataset with the values of the specified fields.
   * @param config the names of the fields whose values populate the dataset.
   * @return a <code>DefaultCategoryDataset</code> instance.
   */
  protected Object createDataset(final ChartConfiguration config) {
    Object[] datasets = new Object[config.fields.length];
    for (int i=0; i<config.fields.length; i++) {
      // datasets[i] = new DefaultValueDataset();
      datasets[i] = newInstance("org.jfree.data.general.DefaultValueDataset");
    }
    config.dataset = datasets;
    populateDataset(config);
    return datasets;
  }

  /**
   * Populate a dataset based on a chart configuration.
   * @param config the chart configuration containing the dataset to populate.
   * @return a <code>ChartConfiguration</code> instance.
   */
  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config) {
    return updateDataset(config);
  }

  /**
   * Update a dataset based on a chart configuration.
   * @param config the chart configuration containing the dataset to update.
   * @return a <code>ChartConfiguration</code> instance.
   */
  @Override
  public ChartConfiguration updateDataset(final ChartConfiguration config) {
    Object[] ds = (Object[]) config.dataset;
    Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
    if (valueMap != null) {
      for (int i=0; i<config.fields.length; i++) {
        Fields field = config.fields[i];
        //ds[i].setValue(valueMap.get(field));
        if (ds != null) invokeMethod(ds[i].getClass(), ds[i], "setValue", new Class[] {Number.class}, valueMap.get(field));
        setIntervals(config, i);
      }
    }
    return config;
  }

  /**
   * Set the meter intervals.
   * @param config the chart configuration containing the dataset to populate.
   * @param i the index of the dataset to update
   */
  private void setIntervals(final ChartConfiguration config, final int i) {
    Object[] intervals = statsHandler.getClientHandler().getMeterIntervals(config.fields[i]);
    if ((intervals == null) || (intervals.length <= 0)) return;
    Object plot = ((Object[]) config.params.get("plot"))[i];
    // plot.clearIntervals();
    invokeMethod(plot.getClass(), plot, "clearIntervals");
    for (Object interval: intervals) {
      // plot.addInterval(interval);
      invokeMethod(plot.getClass(), plot, "addInterval", interval);
    }
  }
}
