/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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
package org.jppf.ui.monitoring.charts.config;

import javax.swing.JPanel;

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
   * The dataset associated with the chart.
   */
  public Object dataset = null;
  //public Dataset dataset = null;
  /**
   * The JFreeChart object.
   */
  public Object chart = null;
  //public JFreeChart chart = null;
  /**
   * The chartPanel enclosing the chart.
   */
  public JPanel chartPanel = null;
  //public ChartPanel chartPanel = null;
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
  public ChartConfiguration(final String name, final ChartType type, final String unit, final int precision, final Fields[] fields)
  {
    this.name = name;
    this.type = type;
    this.unit = unit;
    this.precision = precision;
    this.fields = fields;
  }

  /**
   * Create a configuration from another configuration (copy constructor).
   * @param cfg the configuration to copy from.
   */
  public ChartConfiguration(final ChartConfiguration cfg)
  {
    this.name = cfg.name;
    this.type = cfg.type;
    this.unit = cfg.unit;
    this.precision = cfg.precision;
    this.fields = cfg.fields;
  }

  /**
   * Get a string representation of this chart configuration.
   * @return a string containing this configuration's name.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    return name == null ? "no name" : name;
  }
}
