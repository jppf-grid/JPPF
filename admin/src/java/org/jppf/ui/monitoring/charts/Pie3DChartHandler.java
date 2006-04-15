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

import java.util.Map;
import org.jfree.chart.*;
import org.jfree.data.general.*;
import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.StatsHandler;

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
	public Pie3DChartHandler(StatsHandler statsHandler)
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
		PieDataset ds = createDataset(config);
		JFreeChart chart = ChartFactory.createPieChart3D(config.name, ds, false, true, false);
		config.chart = chart;
		return config;
	}

	/**
	 * Create and populate a dataset with the values of the specified fields.
	 * @param config the names of the fields whose values populate the dataset.
	 * @return a <code>DefaultCategoryDataset</code> instance.
	 */
	protected PieDataset createDataset(ChartConfiguration config)
	{
		DefaultPieDataset ds = new DefaultPieDataset();
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
		DefaultPieDataset dataset = (DefaultPieDataset) config.dataset;
		Map<String, Double> valueMap = statsHandler.getLatestDoubleValues();
		for (String key: config.fields) dataset.setValue(key, valueMap.get(key));
		return config;
	}
}
