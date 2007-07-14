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
