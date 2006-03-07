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
