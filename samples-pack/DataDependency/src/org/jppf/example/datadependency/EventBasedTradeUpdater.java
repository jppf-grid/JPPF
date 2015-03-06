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

import org.jppf.example.datadependency.simulation.*;
import org.slf4j.*;

import com.hazelcast.core.Hazelcast;

/**
 * Instances of this class start a simulated ticker and process
 * the market data updates on a per-event basis.
 * @author Laurent Cohen
 */
public class EventBasedTradeUpdater extends AbstractTradeUpdater
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(EventBasedTradeUpdater.class);
  /**
   * Debug enabled flag.
   */
  private static boolean debugEnabled = log.isDebugEnabled();

  /**
   * Default constructor.
   */
  public EventBasedTradeUpdater()
  {
  }

  /**
   * Main loop.
   */
  @Override
  public void run()
  {
    try
    {
      if (debugEnabled) log.debug("starting trade updater");
      initializeData();
      // start the ticker
      Ticker ticker = new Ticker(marketDataList, config.getInt("minTickerInterval", 50), config.getInt("maxTickerInterval", 1000),
          config.getInt("nbTickerEvents", 0), dataFactory);
      ticker.addTickerListener(marketDataHandler);
      ticker.addTickerListener(this);
      print("starting ticker ...");
      long start = System.currentTimeMillis();
      new Thread(ticker, "ticker thread").start();
      // let it run for the configured time
      Thread.sleep(config.getLong("simulationDuration", 60000L));
      // stop the ticker
      ticker.setStopped(true);
      print("ticker stopped: " + ticker.getEventCount() + " events generated");
      jobExecutor.shutdown();
      while (!jobExecutor.isTerminated()) Thread.sleep(10);
      resultsExecutor.shutdown();
      while (!resultsExecutor.isTerminated()) Thread.sleep(10);
      long elapsed = System.currentTimeMillis() - start;
      statsCollector.setTotalTime(elapsed);
      print(statsCollector.toString());
      marketDataHandler.close();
      Hazelcast.shutdownAll();
    }
    catch(Exception e)
    {
      System.out.println(e.getMessage());
      log.error(e.getMessage(), e);
    }
    System.exit(0);
  }

  /**
   * Called when a piece of market data was updated.
   * @param event encapsulated the market data update.
   * @see org.jppf.example.datadependency.simulation.TickerListener#marketDataUpdated(org.jppf.example.datadependency.simulation.TickerEvent)
   */
  @Override
  public void marketDataUpdated(final TickerEvent event)
  {
    if (jobExecutor.isShutdown()) return;
    if (debugEnabled) log.debug("received update event for " + event.getMarketData().getId());
    jobExecutor.submit(new SubmissionTask(event.getMarketData()));
    statsCollector.dataUpdated();
  }
}
