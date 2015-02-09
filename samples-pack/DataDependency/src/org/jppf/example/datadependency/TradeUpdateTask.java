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

package org.jppf.example.datadependency;

import java.util.*;

import org.jppf.example.datadependency.model.*;
import org.jppf.example.datadependency.startup.DataDependencyStartup;
import org.jppf.server.protocol.JPPFTask;
import org.slf4j.*;

/**
 * JPPF task whose role is to recompute a trade when some market data was updated.
 * @author Laurent Cohen
 */
public class TradeUpdateTask extends JPPFTask
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TradeUpdateTask.class);
  /**
   * Debug enabled flag.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The id of the trade to recompute
   */
  private String tradeId = null;
  /**
   * The identifiers for the market data that was updated.
   */
  private String[] marketDataId = null;
  /**
   * Simulated duration of this task.
   */
  private long taskDuration = 1000L;

  /**
   * Initialize this task with the specified trade and ids of updated market data.
   * @param tradeId the id of the trade to recompute.
   * @param marketDataId the identifiers for the market data that was updated.
   */
  public TradeUpdateTask(final String tradeId, final String...marketDataId)
  {
    this.tradeId = tradeId;
    this.marketDataId = marketDataId;
  }

  /**
   * Recompute the trade.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    //String uuid = NodeRunner.getUuid();
    String msg = "updating trade " + tradeId;
    long taskStart = System.currentTimeMillis();
    /*
     */
    Trade trade = DataDependencyStartup.getTrade(tradeId);
    List<MarketData> data = new ArrayList<>();
    for (String id: trade.getDataDependencies()) data.add(DataDependencyStartup.getMarketData(id));
    // perform some dummy cpu-consuming computation
    long elapsed = 0L;
    for (; elapsed < taskDuration; elapsed = System.currentTimeMillis() - taskStart)
    {
      String s = "";
      for (int i=0; i<10; i++) s += "A"+"10";
    }
    msg = "updated trade " + tradeId + " in " + elapsed + " ms";
    //log.info(msg);
    //System.out.println(msg);
    setResult(msg);
  }

  /**
   * Get the trade.
   * @return a trade object.
   */
  public String getTradeId()
  {
    return tradeId;
  }

  /**
   * Get the simulated duration of this task.
   * @return the duration in milliseconds.
   */
  public long getTaskDuration()
  {
    return taskDuration;
  }

  /**
   * Set the simulated duration of this task.
   * @param taskDuration the duration in milliseconds.
   */
  public void setTaskDuration(final long taskDuration)
  {
    this.taskDuration = taskDuration;
  }
}
