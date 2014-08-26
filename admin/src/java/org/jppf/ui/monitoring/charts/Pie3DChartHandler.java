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

import java.awt.Color;
import java.util.Map;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;

/**
 * Instances of this class are used to create and update 3D pie charts.
 * @author Laurent Cohen
 */
public class Pie3DChartHandler implements ChartHandler
{
  /**
   * The stats formatter that provides the data.
   */
  protected StatsHandler statsHandler = null;

  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public Pie3DChartHandler(final StatsHandler statsHandler)
  {
    this.statsHandler = statsHandler;
  }

  /**
   * Create a plot XY chart based on a chart configuration.
   * @param config holds the configuration parameters for the chart created, modified by this method.
   * @return a <code>ChartConfiguration</code> instance.
   * @see org.jppf.ui.monitoring.charts.ChartHandler#createChart(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
   */
  @Override
  public ChartConfiguration createChart(final ChartConfiguration config)
  {
    Object ds = createDataset(config);
    Class[] paramTypes = { String.class, getClass0("org.jfree.data.general.PieDataset"), Boolean.TYPE, Boolean.TYPE, Boolean.TYPE };
    Object chart = invokeMethod(getClass0("org.jfree.chart.ChartFactory"), null, "createPieChart3D", paramTypes,
        config.name, ds, false, true, false);
    invokeMethod(getClass0("org.jfree.chart.JFreeChart"), chart, "setBackgroundPaint", Color.WHITE );
    config.chart = chart;
    return config;
  }

  /**
   * Create and populate a dataset with the values of the specified fields.
   * @param config the names of the fields whose values populate the dataset.
   * @return a <code>DefaultCategoryDataset</code> instance.
   */
  protected Object createDataset(final ChartConfiguration config)
  {
    //PieDataset ds = new DefaultPieDataset();
    Object ds = newInstance("org.jfree.data.general.DefaultPieDataset");
    config.dataset = ds;
    populateDataset(config);
    return ds;
  }

  /**
   * Populate a dataset based on a chart configuration.
   * @param config the chart configuration containing the dataset to populate.
   * @return a <code>ChartConfiguration</code> instance.
   * @see org.jppf.ui.monitoring.charts.ChartHandler#populateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
   */
  @Override
  public ChartConfiguration populateDataset(final ChartConfiguration config)
  {
    return updateDataset(config);
  }

  /**
   * Update a dataset based on a chart configuration.
   * @param config the chart configuration containing the dataset to update.
   * @return a <code>ChartConfiguration</code> instance.
   * @see org.jppf.ui.monitoring.charts.ChartHandler#updateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
   */
  @Override
  public ChartConfiguration updateDataset(final ChartConfiguration config)
  {
    Object ds = config.dataset;
    Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
    if (valueMap != null)
    {
      for (Fields key: config.fields)
      {
        //ds.setValue(key, valueMap.get(key));
        invokeMethod(ds.getClass(), ds, "setValue", new Class[] {Comparable.class, Number.class}, key, valueMap.get(key));
      }
    }
    return config;
  }
}
