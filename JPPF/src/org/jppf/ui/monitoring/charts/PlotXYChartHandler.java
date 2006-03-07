/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.ui.monitoring.charts;

import java.awt.BasicStroke;
import java.util.Map;
import org.jfree.chart.*;
import org.jfree.chart.labels.XYSeriesLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.*;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.utils.StringUtils;

/**
 * Instances of this class are used to create and update line charts with an horizontal orientation.
 * @author Laurent Cohen
 */
public class PlotXYChartHandler implements ChartHandler
{
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;

	/**
	 * Initialize this chart handler with a specified stats formatter.
	 * @param statsHandler the stats formatter that provides the data.
	 */
	public PlotXYChartHandler(StatsHandler statsHandler)
	{
		this.statsHandler = statsHandler;
	}

	/**
	 * Create a plot XY chart based on a chart configuration.
	 * @param config holds the configuration parameters for the chart created, modified by this method.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#createChart(org.jppf.ui.monitoring.charts.ChartConfiguration)
	 */
	public ChartConfiguration createChart(ChartConfiguration config)
	{
		XYSeriesCollection ds = createDataset(config);
		String s = config.name;
		if (config.unit != null) s += " ("+config.unit+")";
		JFreeChart chart = ChartFactory.createXYLineChart(s, null, null, ds, PlotOrientation.VERTICAL, true, true, false);
		XYPlot plot = chart.getXYPlot();
		XYItemRenderer rend = plot.getRenderer();
		rend.setSeriesVisibleInLegend(true);
		rend.setLegendItemLabelGenerator(new LegendLabelGenerator());
		rend.setStroke(new BasicStroke(2f));
		config.chart = chart;
		return config;
	}

	/**
	 * Create and populate a dataset with the values of the specified fields.
	 * @param config the names of the fields whose values populate the dataset.
	 * @return a <code>DefaultCategoryDataset</code> instance.
	 */
	private XYSeriesCollection createDataset(ChartConfiguration config)
	{
		XYSeriesCollection ds = new XYSeriesCollection();
		for (String key: config.fields)
		{
			XYSeries series = new XYSeries(key);
			ds.addSeries(series);
			series.setMaximumItemCount(statsHandler.getRolloverPosition());
		}
		config.dataset = ds;
		populateDataset(config);
		return ds;
	}
	
	/**
	 * Populate a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to populate.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#populateDataset(org.jppf.ui.monitoring.charts.ChartConfiguration)
	 */
	public ChartConfiguration populateDataset(ChartConfiguration config)
	{
		XYSeriesCollection dataset = (XYSeriesCollection) config.dataset;
		for (int i=0; i<dataset.getSeriesCount(); i++)
		{
			String key = (String) dataset.getSeriesKey(i);
			XYSeries series = dataset.getSeries(i);
			int start = Math.max(0, statsHandler.getTickCount() - statsHandler.getStatsCount());
			for (int j=0; j<statsHandler.getStatsCount(); j++)
			{
				Map<String, Double> valueMap = statsHandler.getDoubleValues(j);
				series.add(start + j, valueMap.get(key));
			}
		}
		return config;
	}

	/**
	 * Update a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to update.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#updateDataset(org.jppf.ui.monitoring.charts.ChartConfiguration)
	 */
	public ChartConfiguration updateDataset(ChartConfiguration config)
	{
		XYSeriesCollection dataset = (XYSeriesCollection) config.dataset;
		Map<String, Double> valueMap = statsHandler.getLatestDoubleValues();
		for (int i=0; i<dataset.getSeriesCount(); i++)
		{
			XYSeries series = dataset.getSeries(i);
			String key = (String) series.getKey();
			series.add(statsHandler.getTickCount(), valueMap.get(key));
		}
		return config;
	}

	/**
	 * A label generator that builds value labels with a specified precision and unit. 
	 */
	public class LegendLabelGenerator implements XYSeriesLabelGenerator
	{
		/**
		 * Generate a label for a value of a specified dataset at the specified row and column.
		 * @param dataset the dataset that contains the value to format.
		 * @param seriesIndex the data series to create a label for.
		 * @return a string containing the formatted value.
		 */
		public String generateLabel(XYDataset dataset, int seriesIndex)
		{
			String key = (String) dataset.getSeriesKey(seriesIndex);
			return StringUtils.shortenLabel(key);
		}
	}
}
