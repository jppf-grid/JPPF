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

package org.jppf.ui.monitoring.data;

import java.util.*;

import org.jppf.server.JPPFStats;

/**
 * 
 * @author Laurent Cohen
 */
public class ConnectionDataHolder
{
	/**
	 * The list of all snapshots kept in memory. the size of this list is alway equal to or less than
	 * the rollover position.
	 */
	private List<JPPFStats> dataList = new Vector<JPPFStats>();
	/**
	 * Cache of the data snapashots fields maps to their corresponding string values.
	 */
	private List<Map<Fields, String>> stringValuesMaps = new Vector<Map<Fields, String>>();
	/**
	 * Cache of the data snapashots fields maps to their corresponding double values.
	 */
	private List<Map<Fields, Double>> doubleValuesMaps = new Vector<Map<Fields, Double>>();

	/**
	 * Get the list of statistic snapshots for this connection data holder.
	 * @return a list of <code>JPPFStats</code> instances.
	 */
	public List<JPPFStats> getDataList()
	{
		return dataList;
	}

	/**
	 * Get a cache of the data snapashots fields maps to their corresponding double values.
	 * @return a list of maps of field names to double values.
	 */
	public List<Map<Fields, Double>> getDoubleValuesMaps()
	{
		return doubleValuesMaps;
	}

	/**
	 * Get a cache of the data snapashots fields maps to their corresponding string values.
	 * @return a list of maps of field names to string values.
	 */
	public List<Map<Fields, String>> getStringValuesMaps()
	{
		return stringValuesMaps;
	}
}
