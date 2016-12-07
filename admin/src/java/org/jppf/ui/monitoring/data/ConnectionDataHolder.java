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
import org.jppf.management.diagnostics.HealthSnapshot;
import org.jppf.utils.stats.JPPFStatistics;

/**
 * Instances of this class hold and manage statistics data snapshots for a single driver connection.
 * @author Laurent Cohen
 */
public class ConnectionDataHolder {
  /**
   * Cache of the data snapshots fields maps to their corresponding double values.
   */
  private final LinkedList<Map<Fields, Double>> doubleValuesMaps = new LinkedList<>();
  /**
   * The topology data associated with the driver connection.
   * @since 5.0
   */
  private final TopologyDriver driver;
  /**
   * Formats the statistics.
   */
  private final StatsTransformer statsFormatter;
  /**
   * The maximum size of the snapshots lists.
   */
  private int capacity;
  /**
   * The size of the snapshots lists.
   */
  private int size;

  /**
   * Default constructor.
   * @param capacity the maximum number of snapshots held by this data holder.
   * @param driver a the drivr for which to get the statistics.
   */
  public ConnectionDataHolder(final int capacity, final TopologyDriver driver) {
    this.driver = driver;
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be greater than 0");
    this.capacity = capacity;
    this.statsFormatter = new StatsTransformer();
  }

  /**
   * Get the size of the list of statistic snapshots.
   * @return the number of snapshots current held.
   */
  public synchronized int getSize() {
    return size;
  }

  /**
   * Get a cache of the data snapshots fields maps to their corresponding double values.
   * @return a list of maps of field names to double values.
   */
  public synchronized List<Map<Fields, Double>> getDoubleValuesMaps() {
    return doubleValuesMaps;
  }

  /**
   * Get the latest data snapshot mapping fields to their corresponding double values.
   * @return a map of field names to double values.
   * @since 5.0
   */
  public synchronized Map<Fields, Double> getLatestDoubleValues() {
    return doubleValuesMaps.getLast();
  }

  /**
   * Get the data snapshot mapping fields to their corresponding double values, at the specified position.
   * @param pos the position of the values to get in the list of snapshots.
   * @return a map of field names to double values if it exists, {@code null} otherwise.
   */
  public synchronized Map<Fields, Double> getDoubleValuesAt(final int pos) {
    if (doubleValuesMaps.isEmpty()) return null;
    if ((pos < 0) || (pos >= getSize())) return null;
    return doubleValuesMaps.get(pos);
  }

  /**
   * Get the topology data associated with the driver connection.
   * @return a {@link TopologyDriver} object.
   * @since 5.0
   */
  public TopologyDriver getDriver() {
    return driver;
  }

  /**
   *
   * @param stats the data snapshot to map.
   * @param snapshot the health data snapshot to map.
   */
  public synchronized void update(final JPPFStatistics stats, final HealthSnapshot snapshot) {
    while (size >= capacity) removeFirst();
    doubleValuesMaps.add(statsFormatter.formatDoubleValues(stats, snapshot));
    size++;
  }

  /**
   * Remove the first snapshot in all the lists.
   */
  private synchronized void removeFirst() {
    doubleValuesMaps.removeFirst();
    size--;
  }

  /**
   * @return the maximum size of the snapshots lists.
   */
  public synchronized int getCapacity() {
    return capacity;
  }

  /**
   * Set the maximum size of the snapshots lists.
   * @param capacity the size to set.
   */
  public synchronized void setCapacity(final int capacity) {
    if (capacity <= 0) throw new IllegalArgumentException("capacity must be greater than 0");
    this.capacity = capacity;
    while (size > capacity) removeFirst();
  }

  /**
   * Cleanup the resources used by this object.
   */
  public synchronized void close() {
    doubleValuesMaps.clear();
  }
}
