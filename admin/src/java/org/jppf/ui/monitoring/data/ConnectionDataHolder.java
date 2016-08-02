/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.ui.monitoring.data;

import java.util.*;

import org.jppf.client.monitoring.topology.TopologyDriver;
import org.jppf.utils.stats.JPPFStatistics;

/**
 * Instances of this class hold and manage statistics data snapshots for a single driver connection.
 * @author Laurent Cohen
 */
public class ConnectionDataHolder {
  /**
   * The list of all snapshots kept in memory. the size of this list is always equal to or less than
   * the rollover position.
   */
  private final List<JPPFStatistics> dataList = new Vector<>();
  /**
   * Cache of the data snapshots fields maps to their corresponding string values.
   */
  private final List<Map<Fields, String>> stringValuesMaps = new Vector<>();
  /**
   * Cache of the data snapshots fields maps to their corresponding double values.
   */
  private final List<Map<Fields, Double>> doubleValuesMaps = new Vector<>();
  /**
   * The topology data associated with the driver connection.
   * @since 5.0
   */
  private TopologyDriver driverData;

  /**
   * Default constructor.
   */
  public ConnectionDataHolder() {
  }

  /**
   * Get the list of statistic snapshots for this connection data holder.
   * @return a list of <code>JPPFStats</code> instances.
   */
  public List<JPPFStatistics> getDataList() {
    return dataList;
  }

  /**
   * Get a cache of the data snapshots fields maps to their corresponding double values.
   * @return a list of maps of field names to double values.
   */
  public List<Map<Fields, Double>> getDoubleValuesMaps() {
    return doubleValuesMaps;
  }

  /**
   * Get a cache of the data snapshots fields maps to their corresponding string values.
   * @return a list of maps of field names to string values.
   */
  public List<Map<Fields, String>> getStringValuesMaps() {
    return stringValuesMaps;
  }

  /**
   * Get the latest data snapshot mapping fields to their corresponding double values.
   * @return a map of field names to double values.
   * @since 5.0
   */
  public Map<Fields, Double> getLatestDoubleValues() {
    synchronized(doubleValuesMaps) {
      return doubleValuesMaps.isEmpty() ? null : doubleValuesMaps.get(doubleValuesMaps.size() - 1);
    }
  }

  /**
   * Get the latest data snapshot mapping fields to their corresponding string values.
   * @return a map of field names to string values.
   * @since 5.0
   */
  public Map<Fields, String> getLatestStringValues() {
    synchronized(stringValuesMaps) {
      return stringValuesMaps.isEmpty() ? null : stringValuesMaps.get(stringValuesMaps.size() - 1);
    }
  }

  /**
   * Get the topology data associated with the driver connection.
   * @return a {@link TopologyDriver} object.
   * @since 5.0
   */
  public synchronized TopologyDriver getDriverData() {
    return driverData;
  }

  /**
   * Set the topology data associated with the driver connection.
   * @param driverData a {@link TopologyDriver} object.
   * @since 5.0
   */
  public synchronized void setDriverData(final TopologyDriver driverData) {
    this.driverData = driverData;
  }
}
