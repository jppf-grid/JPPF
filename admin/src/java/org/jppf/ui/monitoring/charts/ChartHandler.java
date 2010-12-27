/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import org.jppf.ui.monitoring.charts.config.ChartConfiguration;


/**
 * Common interface for all chart handlers, which configure, create, initially poppulate and update various
 * types of charts.
 * @author Laurent Cohen
 */
public interface ChartHandler
{
	/**
	 * Create a chart based on a chart configuration.
	 * @param config holds the configuration parameters for the chart created, modified by this method.
	 * @return a <code>ChartConfiguration</code> instance.
	 */
	ChartConfiguration createChart(ChartConfiguration config);
	/**
	 * Populate a dataset based on a chart configuration.
	 * @param config the chart configuration containing the dataset to populate.
	 * @return a <code>ChartConfiguration</code> instance.
	 */
	ChartConfiguration populateDataset(ChartConfiguration config);
	/**
	 * Update a dataset based on a chart configuration..
	 * @param config the chart configuration containing the dataset to update.
	 * @return a <code>ChartConfiguration</code> instance.
	 */
	ChartConfiguration updateDataset(ChartConfiguration config);
}
