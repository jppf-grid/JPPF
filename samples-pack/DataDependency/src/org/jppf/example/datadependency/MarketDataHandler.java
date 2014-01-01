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
import java.util.concurrent.*;

import org.jppf.example.datadependency.model.*;
import org.jppf.example.datadependency.simulation.*;
import org.slf4j.*;

import com.hazelcast.core.Hazelcast;

/**
 * 
 * @author Laurent Cohen
 */
public class MarketDataHandler implements TickerListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(MarketDataHandler.class);
  /**
   * Debug enabled flag.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of the market data.
   */
  private static Map<String, MarketData> dataMap;
  /**
   * Executes the market data updates in the Hazelcast cluster.
   */
  private ExecutorService executor = Executors.newFixedThreadPool(1);

  /**
   * Perform the nodes' initializations.
   * @param marketDataList the initial market data to send to each node.
   * @throws Exception if any error is raised.
   */
  public void populateMarketData(final List<MarketData> marketDataList) throws Exception
  {
    System.out.println("populating the market data");
    dataMap = Hazelcast.getMap(ModelConstants.MARKET_DATA_MAP_NAME);
    dataMap.clear();
    //for (MarketData data: marketDataList) dataMap.put(data.getId(), data);
    populateInParallel(marketDataList);
    System.out.println("end of populating the market data");
  }

  /**
   * Populate the distributed market data in parallel to speed up the process.
   * @param marketDataList the list of data to distribute.
   */
  private void populateInParallel(final List<MarketData> marketDataList)
  {
    int nbThreads = 8;
    ExecutorService tmpExecutor = Executors.newFixedThreadPool(nbThreads);
    List<Future<?>> futures = new ArrayList<>();
    for (int i=0; i<nbThreads; i++) futures.add(tmpExecutor.submit(new PopulateTask(i, nbThreads, marketDataList)));
    for (Future<?> f: futures)
    {
      try
      {
        f.get();
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
    tmpExecutor.shutdownNow();
  }

  /**
   * Called when a piece of market data was updated.
   * @param event encapsulated the market data update.
   * @see org.jppf.example.datadependency.simulation.TickerListener#marketDataUpdated(org.jppf.example.datadependency.simulation.TickerEvent)
   */
  @Override
  public void marketDataUpdated(final TickerEvent event)
  {
    executor.submit(new NodesUpdateTask(event.getMarketData()));
  }

  /**
   * Close this node handler and release the resources it uses.
   */
  public void close()
  {
    executor.shutdown();
  }

  /**
   * Performs the nodes updates.
   */
  public class PopulateTask implements Runnable
  {
    /**
     * The offset to use to partition the data.
     */
    private int offset;
    /**
     * The number of partitions.
     */
    private int nbThreads;
    /**
     * The data to populate.
     */
    private List<MarketData> marketDataList;

    /**
     * Initialize this task with the specified parameters.
     * @param offset the offset to use to partition the data.
     * @param nbThreads the number of partitions.
     * @param marketDataList the data to populate.
     */
    public PopulateTask(final int offset, final int nbThreads, final List<MarketData> marketDataList)
    {
      this.offset = offset;
      this.nbThreads = nbThreads;
      this.marketDataList = marketDataList;
    }

    /**
     * Execute this task.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      for (int i=offset; i<marketDataList.size(); i += nbThreads)
      {
        MarketData marketData = marketDataList.get(i);
        dataMap.put(marketData.getId(), marketData);
      }
    }
  }

  /**
   * Performs the nodes updates.
   */
  public static class NodesUpdateTask implements Runnable
  {
    /**
     * The update data to send to the nodes.
     */
    private MarketData data;

    /**
     * Initialize this task with the update market data.
     * @param data the update data.
     */
    public NodesUpdateTask(final MarketData data)
    {
      this.data = data;
    }

    /**
     * Execute this task.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      dataMap.put(data.getId(), data);
    }
  }
}
