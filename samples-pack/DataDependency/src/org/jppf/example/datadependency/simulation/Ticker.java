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

package org.jppf.example.datadependency.simulation;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.example.datadependency.model.MarketData;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This class simulates a market ticker by generating random updates at random intervals.
 * @author Laurent Cohen
 */
public class Ticker extends ThreadSynchronization implements Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(Ticker.class);
  /**
   * Debug enabled flag.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Market data to use.
   */
  private List<MarketData> marketData = new ArrayList<>();
  /**
   * The minimum tick interval in milliseconds.
   */
  private int minInterval = 1000;
  /**
   * The maximum tick interval in milliseconds.
   */
  private int maxInterval = 1000;
  /**
   * maximum number of events
   */
  private int maxEvents = 1;
  /**
   * List of listeners to this ticker.
   */
  private List<TickerListener> listeners = new Vector<>();
  /**
   * Executes the event notifications.
   */
  private ExecutorService notificationExecutor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("Ticker"));
  /**
   * The count of generated events.
   */
  private AtomicInteger eventCount = new AtomicInteger(0);
  /**
   * Source of randomly generated data.
   */
  private DataFactory dataFactory = null;

  /**
   * Initialize this Ticker with the specified parameters.
   * @param marketData the market data to use.
   * @param minInterval the minimum tick interval in milliseconds.
   * @param maxInterval the maximum tick interval in milliseconds.
   * @param maxEvents the maximum number of events to generate.
   * @param dataFactory the random number generator used by this ticker.
   */
  public Ticker(final List<MarketData> marketData, final int minInterval, final int maxInterval, final int maxEvents, final DataFactory dataFactory)
  {
    this.marketData = marketData;
    this.minInterval = minInterval;
    this.maxInterval = maxInterval;
    this.maxEvents = maxEvents;
    this.dataFactory = dataFactory;
  }

  /**
   * Run the ticker.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    if (debugEnabled) log.debug("starting ticker");
    int size = marketData.size();
    eventCount.set(0);
    while (!isStopped())
    {
      try
      {
        // compute a random interval int the [min, max] range
        //int n = minInterval + random.nextInt(maxInterval - minInterval + 1);
        int n = dataFactory.getRandomInt(minInterval, maxInterval);
        Thread.sleep(n);
        // choose some market data randomly and emit a ticker event for it
        n = dataFactory.getRandomInt(size);
        fireTickerEvent(marketData.get(n));
        eventCount.incrementAndGet();
        if ((maxEvents > 0) && (eventCount.get() >= maxEvents)) setStopped(true);
      }
      catch(Exception e)
      {
        System.out.println(e.getMessage());
        log.error(e.getMessage(), e);
      }
    }
    notificationExecutor.shutdown();
    if (debugEnabled) log.debug("ticker stopped");
  }

  /**
   * Add a listener to this ticker's list of listeners.
   * @param listener the listener to add.
   */
  public void addTickerListener(final TickerListener listener)
  {
    listeners.add(listener);
  }

  /**
   * Remove a listener from this ticker's list of listeners.
   * @param listener the listener to remove.
   */
  public void removeTickerListener(final TickerListener listener)
  {
    listeners.remove(listener);
  }

  /**
   * Fire an event to notify that the market data with the specified identifier was updated.
   * @param marketData the market data that was updated.
   */
  private void fireTickerEvent(final MarketData marketData)
  {
    notificationExecutor.submit(new NotificationTask(marketData));
  }

  /**
   * Get the count of generated events.
   * @return the event count as an int.
   */
  public int getEventCount()
  {
    return eventCount.get();
  }

  /**
   * Instances of this class notify listeners of data updates.
   */
  public class NotificationTask implements Runnable
  {
    /**
     * The data that was updated.
     */
    private MarketData marketData = null;

    /**
     * Initialize this task with the specified market data.
     * @param marketData the market data that was updated.
     */
    public NotificationTask(final MarketData marketData)
    {
      this.marketData = marketData;
    }

    /**
     * Execute this task.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      TickerEvent event = new TickerEvent(marketData);
      for (TickerListener l: listeners) l.marketDataUpdated(event);
      if (debugEnabled) log.debug("fired update event for " + event.getMarketData().getId());
    }
  }
}
