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

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.StatsHandler;

/**
 * Instances of this class are used to create and update ring charts.
 * @author Laurent Cohen
 */
public class RingChartHandler extends Pie3DChartHandler
{
  /**
   * Initialize this chart handler with a specified stats formatter.
   * @param statsHandler the stats formatter that provides the data.
   */
  public RingChartHandler(final StatsHandler statsHandler)
  {
    super(statsHandler);
  }

  /**
   * Create a ring chart based on a chart configuration.
   * @param config holds the configuration parameters for the chart created, modified by this method.
   * @return a <code>ChartConfiguration</code> instance.
   * @see org.jppf.ui.monitoring.charts.ChartHandler#createChart(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
   */
  @Override
  public ChartConfiguration createChart(final ChartConfiguration config)
  {
    Object ds = createDataset(config);
    //JFreeChart chart = ChartFactory.createRingChart(config.name, ds, false, true, Locale.getDefault());
    Class[] classes = { String.class, getClass0("org.jfree.data.general.PieDataset"), Boolean.TYPE, Boolean.TYPE, Boolean.TYPE};
    Object chart = invokeMethod(getClass0("org.jfree.chart.ChartFactory"), null, "createRingChart", classes,
        config.name, ds, false, true, false);
    invokeMethod(getClass0("org.jfree.chart.JFreeChart"), chart, "setBackgroundPaint", Color.WHITE );
    config.chart = chart;
    return config;
  }
}
