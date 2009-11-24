/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

/**
 * Type-safe enumeration of all available types of charts.
 * @author Laurent Cohen
 */
public enum ChartType
{
	/**
	 * Chart type definition for a 3D bar chart.
	 */
	CHART_3DBAR("3D bar chart"),
	/**
	 * Chart type definition for a plot XY chart.
	 */
	CHART_PLOTXY("Plot XY chart"),
	/**
	 * Chart type definition for a plot XY chart.
	 */
	CHART_AREA("Area chart"),
	/**
	 * Chart type definition for a plot XY chart.
	 */
	CHART_3DPIE("3D pie chart"),
	/**
	 * Chart type definition for a plot XY chart.
	 */
	CHART_RING("Ring chart"),
	/**
	 * Chart type definition for a plot XY chart.
	 */
	CHART_DIFFERENCE("Difference chart");
	
	/**
	 * An english-like name for this enum type.
	 */
	private String name = null;

	/**
	 * Initialize this enum type with a nice display name.
	 * @param name the namew as a string.
	 */
	ChartType(String name)
	{
		this.name = name;
	}

	/**
	 * Get a nice display name for this enum type.
	 * @return the name as a string.
	 * @see java.lang.Enum#toString()
	 */
	public String toString()
	{
		return name;
	}
}
