/*
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

import java.awt.Color;
import java.text.NumberFormat;
import java.util.Map;
import org.jfree.chart.*;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.labels.*;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer3D;
import org.jfree.data.category.*;
import org.jfree.ui.TextAnchor;
import org.jppf.ui.monitoring.charts.config.ChartConfiguration;
import org.jppf.ui.monitoring.data.*;

/**
 * Instances of this class are used to create and update 3D bar charts with an horizontal orientation.
 * @author Laurent Cohen
 */
public class Bar3DChartHandler implements ChartHandler
{
	/**
	 * The stats formatter that provides the data.
	 */
	private StatsHandler statsHandler = null;

	/**
	 * Initialize this chart handler with a specified stats formatter.
	 * @param statsHandler the stats formatter that provides the data.
	 */
	public Bar3DChartHandler(StatsHandler statsHandler)
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
		DefaultCategoryDataset ds = createDataset(config);
		JFreeChart chart = ChartFactory.createBarChart3D(null, null, config.name, ds, PlotOrientation.HORIZONTAL, false, true, false);
		CategoryPlot plot = chart.getCategoryPlot();
    plot.setForegroundAlpha(1.0f);
		final CategoryAxis axis = plot.getDomainAxis();
		axis.setTickLabelsVisible(false);
		Color c1 = new Color(255, 255, 0, 224);
		//Color c2 = new Color(128, 128, 255, 26);
		Color c2 = new Color(160, 160, 255);
		plot.setBackgroundPaint(c2);
		plot.setBackgroundAlpha(0.1f);
		BarRenderer3D rend = (BarRenderer3D) plot.getRenderer();
		Color c3 = new Color(255, 255, 192, 255);
		rend.setWallPaint(c3);
		rend.setSeriesPaint(0, c1);
		rend.setItemLabelGenerator(new LabelGenerator(config.unit, config.precision));

    ItemLabelPosition labelPos = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.BOTTOM_CENTER);
		rend.setPositiveItemLabelPosition(labelPos);
    ItemLabelPosition labelPos2 = new ItemLabelPosition(ItemLabelAnchor.CENTER, TextAnchor.BOTTOM_LEFT);
		rend.setPositiveItemLabelPositionFallback(labelPos2);
		rend.setItemLabelsVisible(true);
		config.chart = chart;
		return config;
	}

	/**
	 * Create and populate a dataset with the values of the specified fields.
	 * @param config the names of the fields whose values populate the dataset.
	 * @return a <code>DefaultCategoryDataset</code> instance.
	 */
	private DefaultCategoryDataset createDataset(ChartConfiguration config)
	{
		DefaultCategoryDataset ds = new DefaultCategoryDataset();
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
		((DefaultCategoryDataset) config.dataset).clear();
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
		DefaultCategoryDataset dataset = (DefaultCategoryDataset) config.dataset;
		Map<Fields, Double> valueMap = statsHandler.getLatestDoubleValues();
		for (Fields key: config.fields) dataset.setValue(valueMap.get(key), "0", key);
		return config;
	}

	/**
	 * A label generator that builds value labels with a specified precision and unit. 
	 */
	public class LabelGenerator extends StandardCategoryItemLabelGenerator
	{
		/**
		 * Number format that formats double values in <i>##...##0.00</i> format.
		 */
		private NumberFormat nf = NumberFormat.getInstance();
		/**
		 * Name of the unit to display in the labels.
		 */
		private String unit = null;

		/**
		 * Initialize this label generator by configuring the NumberFormat instance it uses. 
		 * @param unit the unit to display for the values.
		 * @param precision the number of fraction digits to display for the values.
		 */
		public LabelGenerator(String unit, int precision)
		{
			this.unit = unit;
			nf.setGroupingUsed(true);
			nf.setMinimumIntegerDigits(1);
			nf.setMinimumFractionDigits(precision);
			nf.setMaximumFractionDigits(precision);
		}

		/**
		 * Generate a label for a value of a specified dataset at the specified row and column.
		 * @param dataset the dataset that contains the value to format.
		 * @param row the row coordinate of the value in the dataset.
		 * @param col the colummn coordinate of the value in the dataset.
		 * @return a string containing the formatted value.
		 */
		public String generateLabel(CategoryDataset dataset, int row, int col)
		{
			double val = dataset.getValue(row, col).doubleValue();
			Object key = dataset.getColumnKey(col);
			StringBuilder sb = new StringBuilder(""+key).append(" : ").append(nf.format(val));
			if (unit != null) sb.append(" ").append(unit);
			return sb.toString();
		}
	}
}
