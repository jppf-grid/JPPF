/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.ui.monitoring.charts;

import java.awt.*;
import java.util.Map;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.ThermometerPlot;
import org.jfree.data.general.DefaultValueDataset;
import org.jfree.ui.RectangleInsets;
import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;

/**
 * Instances of this class are used to create and update thermometer charts.
 * @author Laurent Cohen
 */
public class ThermometerChartHandler implements ChartHandler
{
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;

	/**
	 * Initialize this chart handler with a specified stats formatter.
	 * @param statsHandler the stats formatter that provides the data.
	 */
	public ThermometerChartHandler(StatsHandler statsHandler)
	{
		this.statsHandler = statsHandler;
	}

	/**
	 * Create a plot XY chart based on a chart configuration.
	 * @param config holds the configuration parameters for the chart created, modified by this method.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#createChart(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration createChart(ChartConfiguration config)
	{
		DefaultValueDataset ds = createDataset(config);
    ThermometerPlot plot = new ThermometerPlot(ds);
    JFreeChart chart = new JFreeChart(config.name, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
    plot.setInsets(new RectangleInsets(5D, 5D, 5D, 5D));
    plot.setThermometerStroke(new BasicStroke(2.0F));
    plot.setThermometerPaint(Color.blue);
    if (config.unit != null) plot.setUnits(config.unit);
    else plot.setUnits(ThermometerPlot.NONE);
    plot.setUpperBound(500);
    plot.setSubrangeInfo(ThermometerPlot.NORMAL, 0, 249);
    plot.setSubrangeInfo(ThermometerPlot.WARNING, 250, 399);
    plot.setSubrangeInfo(ThermometerPlot.CRITICAL, 400, 500);
		config.chart = chart;
		return config;
	}

	/**
	 * Create and populate a dataset with the values of the specified fields.
	 * @param config the names of the fields whose values populate the dataset.
	 * @return a <code>DefaultValueDataset</code> instance.
	 */
	private DefaultValueDataset createDataset(ChartConfiguration config)
	{
		DefaultValueDataset ds = new DefaultValueDataset();
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
	public ChartConfiguration populateDataset(ChartConfiguration config)
	{
		return updateDataset(config);
	}

	/**
	 * Update a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to update.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#updateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration updateDataset(ChartConfiguration config)
	{
		DefaultValueDataset dataset = (DefaultValueDataset) config.dataset;
		Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
		dataset.setValue(valueMap.get(config.fields[0]));
		return config;
	}
}
