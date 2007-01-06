/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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
package org.jppf.ui.monitoring.charts.config;

import org.jfree.chart.*;
import org.jfree.data.general.Dataset;
import org.jppf.ui.monitoring.charts.ChartType;
import org.jppf.ui.monitoring.data.Fields;

/**
 * Instances of this class represent the configuration elements used to create and update a chart definition.
 * @author Laurent Cohen
 */
public class ChartConfiguration
{
	/**
	 * Name of this configuration. Must be unique.
	 */
	public String name = null;
	/**
	 * Determines the type of the chart, ie bar chart, plot chart, pie, etc.
	 */
	public ChartType type  = null;
	/**
	 * Unit to display on item labels or in the legend or title.
	 */
	public String unit = null;
	/**
	 * Precision of the number to display in items and tooltip labels.
	 */
	public int precision = 0;
	/**
	 * The list of fields charted in this chart.
	 */
	public Fields[] fields = null;
	/**
	 * The dataset associated witht the chart.
	 */
	public Dataset dataset = null;
	/**
	 * The JFreeChart object.
	 */
	public JFreeChart chart = null;
	/**
	 * The chartPanel enclosing the chart.
	 */
	public ChartPanel chartPanel = null;
	/**
	 * Position of the chart in its containing panel.
	 */
	public int position = -1;

	/**
	 * Default constructor.
	 */
	public ChartConfiguration()
	{
	}

	/**
	 * Create a configuration with the specified parameters.
	 * @param name the name of this configuration, must be unique.
	 * @param type determines the type of the chart, ie bar chart, plot chart, pie, etc.
	 * @param unit the unit to display on item labels or in the legend or title.
	 * @param precision the precision of the number to display in items and tooltip labels.
	 * @param fields the list of fields charted in this chart.
	 */
	public ChartConfiguration(String name, ChartType type, String unit, int precision, Fields[] fields)
	{
		this.name = name;
		this.type = type;
		this.unit = unit;
		this.precision = precision;
		this.fields = fields;
	}
	
	/**
	 * Get a string representation of this chart configuration.
	 * @return a string containg this configuration's name.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return name == null ? "no name" : name;
	}
}
