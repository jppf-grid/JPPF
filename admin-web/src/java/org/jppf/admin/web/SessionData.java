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

package org.jppf.admin.web;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.admin.web.topology.TopologyTreeData;
import org.jppf.ui.treetable.TreeViewType;

/**
 *
 * @author Laurent Cohen
 */
public class SessionData {
  /**
   * COunts instances of this class.
   */
  private static final AtomicLong instanceCount = new AtomicLong(0L);
  /**
   * Holds the data for each type of table tree view.
   */
  private transient EnumMap<TreeViewType, TableTreeData> dataMap = new EnumMap<>(TreeViewType.class);
  /**
   * A JPPF-internal id for this session data.
   */
  private final long instanceNumber;

  /**
   * 
   */
  SessionData() {
    instanceNumber = instanceCount.incrementAndGet();
  }

  /**
   * @param instanceNumber .
   */
  SessionData(final long instanceNumber) {
   this.instanceNumber = instanceNumber;
  }

  /**
   *
   *@param type the type of view to retrieve the data for.
   * @return the data elements for the grid topology.
   */
  public TableTreeData getData(final TreeViewType type) {
    TableTreeData data = getDataMap().get(type);
    if (data == null) {
      switch(type) {
        case TOPOLOGY:
          data = new TopologyTreeData();
          break;

        case HEALTH:
          data = new TableTreeData(type);
          break;

        case JOBS:
          data = new TableTreeData(type);
          break;
      }
    }
    dataMap.put(type, data);
    return data;
  }

  /**
   * @return the amapping of view type to table tree data.
   */
  private EnumMap<TreeViewType, TableTreeData> getDataMap() {
    if (dataMap == null) dataMap = new EnumMap<>(TreeViewType.class);
    return dataMap;
  }

  /**
   * @return a JPPF-internal id for this session data.
   * @exclude
   */
  public long getId() {
    return instanceNumber;
  }
}
