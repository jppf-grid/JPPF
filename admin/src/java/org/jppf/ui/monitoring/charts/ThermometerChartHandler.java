/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
