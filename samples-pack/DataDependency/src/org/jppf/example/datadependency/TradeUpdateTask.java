/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.example.datadependency;

import java.util.*;

import org.jppf.example.datadependency.model.*;
import org.jppf.example.datadependency.startup.DataDependencyStartup;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.utils.DateTimeUtils;

/**
 * JPPF task whose role is to recompute a trade when some market data was updated.
 * @author Laurent Cohen
 */
public class TradeUpdateTask extends AbstractTask<String> {
  /**
   * The id of the trade to recompute
   */
  private String tradeId = null;
  /**
   * Simulated duration of this task.
   */
  private long taskDuration = 1000L;

  /**
   * Initialize this task with the specified trade and ids of updated market data.
   * @param tradeId the id of the trade to recompute.
   */
  public TradeUpdateTask(final String tradeId) {
    this.tradeId = tradeId;
  }

  /**
   * Recompute the trade.
   */
  @Override
  public void run() {
    String msg = "updating trade " + tradeId;
    final long taskStart = System.nanoTime();
    final Trade trade = DataDependencyStartup.getTrade(tradeId);
    final List<MarketData> data = new ArrayList<>();
    for (final String id : trade.getDataDependencies()) data.add(DataDependencyStartup.getMarketData(id));
    // perform some dummy cpu-consuming computation
    long elapsed = 0L;
    for (; elapsed < taskDuration; elapsed = DateTimeUtils.elapsedFrom(taskStart)) {
      String s = "";
      for (int i = 0; i < 10; i++) s += "A" + "10";
      s.toString();
    }
    msg = "updated trade " + tradeId + " in " + elapsed + " ms";
    setResult(msg);
  }

  /**
   * Get the trade.
   * @return a trade object.
   */
  public String getTradeId() {
    return tradeId;
  }

  /**
   * Get the simulated duration of this task.
   * @return the duration in milliseconds.
   */
  public long getTaskDuration() {
    return taskDuration;
  }

  /**
   * Set the simulated duration of this task.
   * @param taskDuration the duration in milliseconds.
   */
  public void setTaskDuration(final long taskDuration) {
    this.taskDuration = taskDuration;
  }
}
