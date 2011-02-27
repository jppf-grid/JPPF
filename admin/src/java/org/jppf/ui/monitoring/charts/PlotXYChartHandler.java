/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.awt.BasicStroke;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;
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
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#createChart(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration createChart(ChartConfiguration config)
	{
		Object ds = createDataset(config);
		String s = config.name;
		if (config.unit != null) s += " ("+config.unit+")";
		//JFreeChart chart = ChartFactory.createXYLineChart(s, null, null, ds, PlotOrientation.VERTICAL, true, true, false);
		Object chart = invokeMethod(getClass0("org.jfree.chart.ChartFactory"), null, "createXYLineChart",
			s, null, null, ds, getField(getClass0("org.jfree.chart.plot.PlotOrientation"), null, "VERTICAL"), true, true, false);
		//XYPlot plot = chart.getXYPlot();
		Object plot = invokeMethod(getClass0("org.jfree.chart.JFreeChart"), chart, "getXYPlot");
		//XYItemRenderer rend = plot.getRenderer();
		Object rend = invokeMethod(getClass0("org.jfree.chart.plot.XYPlot"), plot, "getRenderer");
		Class rendClass = getClass0("org.jfree.chart.renderer.xy.XYItemRenderer");
		//rend.setBaseSeriesVisibleInLegend(true);
		invokeMethod(rendClass, rend, "setBaseSeriesVisibleInLegend", new Class[] {Boolean.TYPE}, true);
		//rend.setLegendItemLabelGenerator(new LegendLabelGenerator());
		Object labelGenerator = Proxy.newProxyInstance(
			getCurrentClassLoader(), getClasses("org.jfree.chart.labels.XYSeriesLabelGenerator"), new LegendLabelGeneratorInvocationHandler());
		invokeMethod(rendClass, rend, "setLegendItemLabelGenerator", labelGenerator);
		//rend.setBaseStroke(new BasicStroke(2f));
		invokeMethod(rendClass, rend, "setBaseStroke", new BasicStroke(2f));
		config.chart = chart;
		return config;
	}

	/**
	 * Create and populate a dataset with the values of the specified fields.
	 * @param config the names of the fields whose values populate the dataset.
	 * @return a <code>DefaultCategoryDataset</code> instance.
	 */
	private Object createDataset(ChartConfiguration config)
	{
		//XYSeriesCollection ds = new XYSeriesCollection();
		Object ds = newInstance("org.jfree.data.xy.XYSeriesCollection");
		for (Fields key: config.fields)
		{
			//XYSeries series = new XYSeries(key);
			Object series = invokeConstructor(getClass0("org.jfree.data.xy.XYSeries"), new Class[] {Comparable.class}, key);
			//ds.addSeries(series);
			invokeMethod(ds.getClass(), ds, "addSeries", series);
			//series.setMaximumItemCount(statsHandler.getRolloverPosition());
			invokeMethod(series.getClass(), series, "setMaximumItemCount", statsHandler.getRolloverPosition());
		}
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
		//XYSeriesCollection ds= (XYSeriesCollection) config.dataset;
		Object ds = config.dataset;
		//List list = ds.getSeries();
		List list = (List) invokeMethod(ds.getClass(), ds, "getSeries");
		for (Object o: list)
		{
			//((XYSeries) o).clear();
			invokeMethod(o.getClass(), o, "clear");
		}
		//for (int i=0; i<ds.getSeriesCount(); i++)
		for (int i=0; i<(Integer) invokeMethod(ds.getClass(), ds, "getSeriesCount"); i++)
		{
			//Fields key = (Fields) ds.getSeriesKey(i);
			Fields key = (Fields) invokeMethod(ds.getClass(), ds, "getSeriesKey", i);
			//XYSeries series = ds.getSeries(i);
			Object series = invokeMethod(ds.getClass(), ds, "getSeries", new Class[] {Integer.TYPE}, i);
			int start = Math.max(0, statsHandler.getTickCount() - statsHandler.getStatsCount());
			for (int j=0; j<statsHandler.getStatsCount(); j++)
			{
				Map<Fields, Double> valueMap = statsHandler.getDoubleValues(j);
				//series.add(start + j, valueMap.get(key));
				invokeMethod(series.getClass(), series, "add", new Class[] { Double.TYPE, Number.class}, start + j, valueMap.get(key));
			}
		}
		return config;
	}

	/**
	 * Update a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to update.
	 * @return a <code>ChartConfiguration</code> instance.
	 * @see org.jppf.ui.monitoring.charts.ChartHandler#updateDataset(org.jppf.ui.monitoring.charts.config.ChartConfiguration)
	 */
	public ChartConfiguration updateDataset(ChartConfiguration config)
	{
		//XYSeriesCollection ds = (XYSeriesCollection) config.dataset;
		Object ds = config.dataset;
		Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
		//for (int i=0; i<ds.getSeriesCount(); i++)
		for (int i=0; i<(Integer) invokeMethod(ds.getClass(), ds, "getSeriesCount"); i++)
		{
			//XYSeries series = ds.getSeries(i);
			Object series = invokeMethod(ds.getClass(), ds, "getSeries", new Class[] {Integer.TYPE}, i);
			//Fields key = (Fields) series.getKey();
			Fields key = (Fields) invokeMethod(series.getClass(), series, "getKey");
			//series.add(statsHandler.getTickCount(), valueMap.get(key));
			invokeMethod(series.getClass(), series, "add", new Class[] { Double.TYPE, Number.class}, statsHandler.getTickCount(), valueMap.get(key));
		}
		return config;
	}

	/**
	 * Invocation handler for a dynamic proxy to a <code>org.jppf.ui.monitoring.charts.PlotXYChartHandler.LegendLabelGenerator</code> implementation.
	 */
	public static class LegendLabelGeneratorInvocationHandler implements InvocationHandler
	{
		/**
		 * Invoke a specified method on the specified proxy.
		 * @param proxy the dynamic proxy to invoke the method on.
		 * @param method the method to invoke.
		 * @param args the method parameters values.
		 * @return the result of the method invocation.
		 * @throws Throwable if any error occurs.
		 * @see java.lang.reflect.InvocationHandler#invoke(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
		 */
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
		{
			Fields key = (Fields) invokeMethod(args[0].getClass(), args[0], "getSeriesKey", args[1]);
			return StringUtils.shortenLabel(key.toString());
		}
	}
}
