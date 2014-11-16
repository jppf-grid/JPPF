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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.client.*;
import org.jppf.example.datadependency.model.MarketData;
import org.jppf.example.datadependency.model.Trade;
import org.jppf.example.datadependency.simulation.*;
import org.jppf.management.JMXDriverConnectionWrapper;
import org.jppf.management.JPPFManagementInfo;
import org.jppf.node.policy.Equal;
import org.jppf.node.policy.ExecutionPolicy;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractTradeUpdater implements TickerListener, Runnable
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractTradeUpdater.class);
  /**
   * Debug enabled flag.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The JPPF client, handles all communications with the server.
   */
  protected static JPPFClient jppfClient =  null;
  /**
   * The JPPF configuration.
   */
  protected TypedProperties config = JPPFConfiguration.getProperties();
  /**
   * Handles the distributed market data.
   */
  protected MarketDataHandler marketDataHandler = new MarketDataHandler();
  /**
   * The node selector.
   */
  protected NodeSelector nodeSelector = null;
  /**
   * The generated list of market data objects.
   */
  protected List<MarketData> marketDataList = null;
  /**
   * Associations between market data and trades.
   */
  protected SortedMap<String, Set<String>> dataToTradeMap = new TreeMap<>();
  /**
   * Executes the job submissions.
   */
  protected ExecutorService jobExecutor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("JobExecutor"));
  /**
   * Executes the results collection and processing.
   */
  protected ExecutorService resultsExecutor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("ResultsExecutor"));
  /**
   * Count of submitted jobs - used as part of their id.
   */
  protected AtomicLong jobCount = new AtomicLong(0);
  /**
   * Collects statistics about the execution.
   */
  protected StatsCollector statsCollector = new StatsCollector();
  /**
   * Source of randomly generated data.
   */
  protected DataFactory dataFactory = null;
  /**
   * Minimum task duration.
   */
  protected int minTaskDuration = config.getInt("minTaskDuration", 1);
  /**
   * Maximum task duration.
   */
  protected int maxTaskDuration = config.getInt("maxTaskDuration", 1);

  /**
   * Default constructor.
   */
  public AbstractTradeUpdater()
  {
  }

  /**
   * Generate and initialize data used in the simulation.
   * @throws Exception if any error occurs.
   */
  protected void initializeData() throws Exception
  {
    String s = config.getString("dataFactoryImpl", "uniform");
    if (s.equalsIgnoreCase("gaussian")) dataFactory = new GaussianDataFactory();
    else dataFactory = new UniformDataFactory();
    // generate random market data
    marketDataList = dataFactory.generateDataMarketObjects(config.getInt("nbMarketData", 10));
    // generate random trades
    List<Trade> tradeList = dataFactory.generateTradeObjects(config.getInt("nbTrades", 10), marketDataList,
        config.getInt("minDataPerTrade", 1), config.getInt("maxDataPerTrade", 5));
    // initialize the nodes: collect their id and send them the current market data
    marketDataHandler.populateMarketData(marketDataList);
    List<String> idList = getNodeIds();
    nodeSelector = new NodeSelector(tradeList, idList);
    for (Trade t: tradeList)
    {
      for (String marketDataId: t.getDataDependencies())
      {
        Set<String> tradeSet = dataToTradeMap.get(marketDataId);
        if (tradeSet == null)
        {
          tradeSet = new HashSet<>();
          dataToTradeMap.put(marketDataId, tradeSet);
        }
        tradeSet.add(t.getId());
      }
    }
  }

  /**
   * Get the uuids of all nodes using the management APIs.
   * @return the ids as a list of strings.
   * @throws Exception if any error occurs.
   */
  protected List<String> getNodeIds() throws Exception
  {
    JPPFClientConnection c = jppfClient.getClientConnection();
    JMXDriverConnectionWrapper driver = c.getConnectionPool().getJmxConnection();
    while (!driver.isConnected()) Thread.sleep(1L);
    Collection<JPPFManagementInfo> nodesInfo = driver.nodesInformation();
    List<String> idList = new ArrayList<>();
    for (JPPFManagementInfo info: nodesInfo) idList.add(info.getUuid());
    return idList;
  }

  /**
   * Print a message to the log and to the system standard output.
   * @param s the message to print.
   */
  protected void print(final String s)
  {
    System.out.println(s);
    log.info(s);
  }

  /**
   * Task whose role is to submit a number of jobs determined when a market datum is updated.
   */
  public class SubmissionTask implements Runnable
  {
    /**
     * The updated market data.
     */
    MarketData[] marketData = null;
    /**
     * Event notification timestamp.
     */
    long timestamp = System.currentTimeMillis();

    /**
     * Initialize this task with the specified market data.
     * @param marketData the updated market data.
     */
    public SubmissionTask(final MarketData...marketData)
    {
      this.marketData = marketData;
    }

    /**
     * Submits a job.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      try
      {
        if (debugEnabled) log.debug("processing update for " + Arrays.toString(marketData));
        // determine which trades are impacted
        Set<String> tradeIdSet = new HashSet<>();
        for (MarketData md: marketData)
        {
          Set<String> set = dataToTradeMap.get(md.getId());
          if (set != null) tradeIdSet.addAll(set);
        }
        if (tradeIdSet.isEmpty()) return;
        // associate each node with a list of impacted trades
        Map<String, List<String>> nodeMap = new HashMap<>();
        for (String tradeId: tradeIdSet)
        {
          String nodeId = nodeSelector.getNodeId(tradeId);
          List<String> list = nodeMap.get(nodeId);
          if (list == null)
          {
            list = new ArrayList<>();
            nodeMap.put(nodeId, list);
          }
          list.add(tradeId);
        }
        // create a job for each node
        for (final Map.Entry<String, List<String>> entry : nodeMap.entrySet())
        {
          List<String> list = entry.getValue();
          submitOneJobPerNode(entry.getKey(), list);
          //submitOneJobPerTrade(nodeId, list);
        }
      }
      catch(Exception e)
      {
        System.out.println(e.getMessage());
        log.error(e.getMessage(), e);
      }
    }

    /**
     * This method submits one job per node, each job comprising as many tasks
     * as there are trades to recompute on this node.
     * @param nodeId the id of the node on which to execute the job.
     * @param tradeIdList the list of impacted trades to recompute on the node.
     * @throws Exception if any error occurs.
     */
    private void submitOneJobPerNode(final String nodeId, final List<String> tradeIdList) throws Exception
    {
      JPPFJob job = new JPPFJob();
      job.setName("Job (" + jobCount.incrementAndGet() + ")");
      job.setBlocking(false);
      // set an execution policy that forces execution on the node with the specified id
      job.getSLA().setExecutionPolicy(new Equal("jppf.uuid", false, nodeId));
      // create a task for each trade
      for (String tradeId: tradeIdList) job.add(createTask(tradeId));
      jppfClient.submitJob(job);
      resultsExecutor.submit(new ResultCollectionTask(job, timestamp));
    }

    /**
     * This method submits one job per trade, each job comprising a single task
     * that recomputes a single trade.
     * @param nodeId the id of the node on which to execute the job.
     * @param tradeIdList the list of impacted trades to recompute on the node.
     * @throws Exception if any error occurs.
     */
    private void submitOneJobPerTrade(final String nodeId, final List<String> tradeIdList) throws Exception
    {
      ExecutionPolicy policy = new Equal("jppf.uuid", false, nodeId);
      // create a job for each trade
      for (String tradeId: tradeIdList)
      {
        JPPFJob job = new JPPFJob();
        job.setName("[Node id=" + nodeId + "] trade=" + tradeId + " (" + jobCount.incrementAndGet() + ")");
        job.setBlocking(false);
        // set an execution policy that forces execution on the node with the specified id
        job.getSLA().setExecutionPolicy(policy);
        job.add(createTask(tradeId));
        jppfClient.submitJob(job);
        resultsExecutor.submit(new ResultCollectionTask(job, timestamp));
      }
    }

    /**
     * Create a trade update task from the specified trade.
     * @param tradeId the trade t recompute.
     * @return a new <code>TradeUpdateTask</code> instance.
     */
    private TradeUpdateTask createTask(final String tradeId)
    {
      TradeUpdateTask task = new TradeUpdateTask(tradeId);
      task.setTaskDuration(dataFactory.getRandomInt(minTaskDuration, maxTaskDuration));
      return task;
    }
  }

  /**
   * Task whose role is to submit a number of jobs determined when a market datum is updated.
   */
  public class ResultCollectionTask implements Runnable
  {
    /**
     * The job from which to get the results.
     */
    private JPPFJob job;
    /**
     * Event notification timestamp.
     */
    private long timestamp;

    /**
     * Initialize this task with the specified parameter.
     * @param job the job from which to get the results.
     * @param timestamp the event notification timestamp.
     */
    public ResultCollectionTask(final JPPFJob job, final long timestamp)
    {
      this.job = job;
      this.timestamp = timestamp;
    }

    /**
     * Process a set of results.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      try
      {
        List<Task<?>> results = job.awaitResults();
        // do something with the results?
        StringBuilder sb = new StringBuilder("Updated trades: ");
        for (int i=0; i<results.size(); i++)
        {
          if (i > 0) sb.append(", ");
          TradeUpdateTask task = (TradeUpdateTask) results.get(i);
          sb.append(task.getTradeId());
        }
        long time = System.currentTimeMillis() - timestamp;
        sb.append('[').append(time).append(" ms]");
        log.info(sb.toString());
        statsCollector.jobProcessed(results, time);
      }
      catch(Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * Start the jppf client instance.
   */
  public static void openJPPFClient()
  {
    jppfClient = new JPPFClient();
  }

  /**
   * Close the jppf client.
   */
  public static void closeJPPFClient()
  {
    if (jppfClient != null) jppfClient.close();
  }
}
