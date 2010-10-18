/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.client.*;
import org.jppf.example.datadependency.model.*;
import org.jppf.example.datadependency.simulation.*;
import org.jppf.node.policy.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;
import org.slf4j.*;

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
	 * Handles the nodes notifications and initializations. 
	 */
	protected NodeHandler nodeHandler = new NodeHandler();
	/**
	 * The node selector.
	 */
	protected NodeSelector nodeSelector = null;
	/**
	 * The generated list of market fata objects.
	 */
	protected List<MarketData> marketDataList = null;
	/**
	 * Associations between market data and trades.
	 */
	protected SortedMap<String, Set<Trade>> dataToTradeMap = new TreeMap<String, Set<Trade>>();
	/**
	 * Executes the job submissions.
	 */
	protected ExecutorService jobExecutor = Executors.newFixedThreadPool(1);
	/**
	 * Executes the results collection and processing.
	 */
	protected ExecutorService resultsExecutor = Executors.newFixedThreadPool(1);
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
		List<Integer> idList = nodeHandler.initNodes(marketDataList);
		nodeSelector = new NodeSelector(tradeList, idList);
		for (Trade t: tradeList)
		{
			for (String id: t.getDataDependencies())
			{
				Set<Trade> tradeSet = dataToTradeMap.get(id);
				if (tradeSet == null)
				{
					tradeSet = new HashSet<Trade>();
					dataToTradeMap.put(id, tradeSet);
				}
				tradeSet.add(t);
			}
		}
	}

	/**
	 * Print a message to the log and to the system standard output.
	 * @param s the message to print.
	 */
	protected void print(String s)
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
		public SubmissionTask(MarketData...marketData)
		{
			this.marketData = marketData;
		}
	
		/**
		 * Submits a job.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				if (debugEnabled) log.debug("processing update for " + marketData);
				// determine which trades are impacted
				Set<Trade> tradeSet = new HashSet<Trade>();
				for (MarketData md: marketData) tradeSet.addAll(dataToTradeMap.get(md.getId()));
				if (tradeSet.isEmpty()) return;
				// associate each node with a list of impacted trades
				Map<Integer, List<Trade>> nodeMap = new HashMap<Integer, List<Trade>>();
				for (Trade trade: tradeSet)
				{
					int n = nodeSelector.getNodeId(trade);
					List<Trade> list = nodeMap.get(n);
					if (list == null)
					{
						list = new ArrayList<Trade>();
						nodeMap.put(n, list);
					}
					list.add(trade);
				}
				// create a job for each node
				for (Integer nodeId: nodeMap.keySet())
				{
					List<Trade> list = nodeMap.get(nodeId);
					submitOneJobPerNode(nodeId, list);
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
		 * @param list the list of impacted trades to recompute on the node.
		 * @throws Exception if any error occurs.
		 */
		private void submitOneJobPerNode(int nodeId, List<Trade> list) throws Exception
		{
			JPPFJob job = new JPPFJob();
			job.setId("[Node id=" + nodeId + "] (" + jobCount.incrementAndGet() + ")");
			job.setBlocking(false);
			// set an execution policy that forces execution on the node with the specified id
			job.getJobSLA().setExecutionPolicy(new Equal("id", nodeId));
			JPPFResultCollector collector = new JPPFResultCollector(list.size());
			job.setResultListener(collector);
			// create a task for each trade
			for (Trade t: list) job.addTask(createTask(t));
			jppfClient.submit(job);
			resultsExecutor.submit(new ResultCollectionTask(collector, timestamp));
		}

		/**
		 * This method submits one job per trade, each job comprising a single task
		 * that recomputes a single trade.
		 * @param nodeId the id of the node on which to execute the job.
		 * @param list the list of impacted trades to recompute on the node.
		 * @throws Exception if any error occurs.
		 */
		private void submitOneJobPerTrade(int nodeId, List<Trade> list) throws Exception
		{
			ExecutionPolicy policy = new Equal("id", nodeId);
			// create a job for each trade
			for (Trade trade: list)
			{
				JPPFJob job = new JPPFJob();
				job.setId("[Node id=" + nodeId + "] trade=" + trade.getId() + " (" + jobCount.incrementAndGet() + ")");
				job.setBlocking(false);
				// set an execution policy that forces execution on the node with the specified id
				job.getJobSLA().setExecutionPolicy(policy);
				JPPFResultCollector collector = new JPPFResultCollector(1);
				job.setResultListener(collector);
				job.addTask(createTask(trade));
				jppfClient.submit(job);
				resultsExecutor.submit(new ResultCollectionTask(collector, timestamp));
			}
		}

		/**
		 * Create a trade update task from the specified trade.
		 * @param trade the trade t recompute.
		 * @return a new <code>TradeUpdateTask</code> instance.
		 */
		private TradeUpdateTask createTask(Trade trade)
		{
			TradeUpdateTask task = new TradeUpdateTask(trade);
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
		 * The object from which to get the results.
		 */
		private JPPFResultCollector collector = null;
		/**
		 * Event notification timestamp.
		 */
		private long timestamp = 0L;

		/**
		 * Initialize this task with the specified market data.
		 * @param collector the object from which to get the results.
		 * @param timestamp the event notification timestamp.
		 */
		public ResultCollectionTask(JPPFResultCollector collector, long timestamp)
		{
			this.collector = collector;
			this.timestamp = timestamp;
		}
	
		/**
		 * Process a set of results.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				List<JPPFTask> results = collector.waitForResults();
				// do something with the results?
				StringBuilder sb = new StringBuilder("Updated trades: ");
				for (int i=0; i<results.size(); i++)
				{
					if (i > 0) sb.append(", ");
					TradeUpdateTask task = (TradeUpdateTask) results.get(i);
					sb.append(task.getTrade().getId());
				}
				long time = System.currentTimeMillis() - timestamp;
				/*
				sb.append("[").append(time).append(" ms]");
				log.info(sb.toString());
				*/
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
