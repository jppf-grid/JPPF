/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.ui.monitoring.data.*;
import org.jppf.utils.collections.*;

/**
 * A cache for charts data, implemented as a singleton.
 * @author Laurent Cohen
 */
public final class ChartDataCache {
  /**
   * The singleton instance of this cache.
   */
  private static final ChartDataCache instance = new ChartDataCache();
  /**
   * Mapping of fields to their collection of values.
   */
  private final CollectionMap<Fields, Double> map = new LinkedListHashMap<>();
  /**
   * Usage counts for the fields.
   */
  private final Map<Fields, AtomicInteger> fieldCount = new HashMap<>();
  
  /**
   * Instantiation not permitted.
   */
  private ChartDataCache() {
  }

  /**
   * @return the singleton instance of this class.
   */
  public static ChartDataCache getInstance() {
    return instance;
  }

  /**
   * Add the specified fields.
   * @param fields the fields to add..
   * @param handler holds the updated data.
   */
  public synchronized void addFields(final Fields[] fields, final StatsHandler handler) {
    final List<Fields> added = new ArrayList<>(fields.length);
    for (final Fields field: fields) {
      final AtomicInteger count = fieldCount.get(field);
      if (count == null) {
        fieldCount.put(field, new AtomicInteger(1));
        added.add(field);
      } else {
        count.incrementAndGet();
      }
    }
    final int count = Math.min(handler.getRolloverPosition(), handler.getStatsCount());
    if (count > 0) {
      for (int i=0; i<count; i++) {
        final Map<Fields, Double> valueMap = handler.getDoubleValues(i);
        for (final Fields field: added) map.putValue(field, valueMap.get(field));
      }
    } else {
      for (final Fields field: added) map.putValue(field, 0d);
    }
  }

  /**
   * Remove fields that are no longer used.
   * @param fields the fields to add.
   */
  public synchronized void removeFields(final Fields[] fields) {
    for (final Fields field: fields) {
      final AtomicInteger count = fieldCount.get(field);
      if (count == null) continue;
      final int n = count.decrementAndGet();
      if (n <= 0) {
        fieldCount.remove(field);
        map.removeKey(field);
      }
    }
  }

  /**
   * Update the values of the fields.
   * @param handler holds the updated data.
   */
  public synchronized void update(final StatsHandler handler) {
    final Map<Fields, Double> valueMap = handler.getLatestDoubleValues();
    final int count = Math.min(handler.getStatsCount(), handler.getRolloverPosition());
    for (final Map.Entry<Fields, Collection<Double>> entry: map.entrySet()) {
      final Fields field = entry.getKey();
      final LinkedList<Double> values = (LinkedList<Double>) entry.getValue();
      values.offer(valueMap.get(field));
      while (values.size() > count) values.poll();
      while (values.size() < count) values.addFirst(0d);
    }
  }

  /**
   * Get the available data for the specified fields. 
   * @param fields the fields for which to get data.
   * @return a mapping of fields to the associated data.
   */
  public synchronized Map<Fields, List<Double>> getData(final Fields[] fields) {
    final Map<Fields, List<Double>> result = new HashMap<>(fields.length);
    int maxLength = 0;
    for (final Fields field: fields) {
      final List<Double> values = (List<Double>) map.getValues(field);
      final int n = (values == null) ? 0 : values.size();
      if (n > maxLength) maxLength = n;
    }
    if (maxLength <= 0) maxLength = 1;
    for (final Fields field: fields) {
      List<Double> values = (List<Double>) map.getValues(field);
      if (values == null) {
        values = new LinkedList<>();
        for (int i=0; i<maxLength; i++) values.add(0d);
        map.addValues(field, values);
      } else {
        final int n = values.size();
        if (n < maxLength) {
          for (int i=0; i<maxLength - n; i++) values.add(0d);
        }
      }
      result.put(field, new LinkedList<>(values));
    }
    return result;
  }
}
