/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.example.datadependency.model.*;
import org.jppf.example.datadependency.simulation.*;
import org.jppf.node.policy.Equal;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TradeUpdater implements TickerListener
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(TradeUpdater.class);
	/**
	 * Debug enabled flag.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The JPPF client, handles all communications with the server.
	 */
	private static JPPFClient jppfClient =  null;
	/**
	 * Handles the nodes notifications and initializations. 
	 */
	private NodeHandler nodeHandler = new NodeHandler();
	/**
	 * The node selector.
	 */
	private NodeSelector nodeSelector = null;
	/**
	 * Associations between market data and trades.
	 */
	private SortedMap<String, Set<Trade>> dataToTradeMap = new TreeMap<String, Set<Trade>>();
	/**
	 * Executes the job submissions.
	 */
	private ExecutorService jobExecutor = Executors.newFixedThreadPool(1);
	/**
	 * Executes the results collection and processing.
	 */
	private ExecutorService resultsExecutor = Executors.newFixedThreadPool(1);
	/**
	 * Count of submitted jobs - used as part of their id.
	 */
	private AtomicLong jobCount = new AtomicLong(0);
	/**
	 * Collects statistics about the execution.
	 */
	private StatsCollector statsCollector = new StatsCollector();

	/**
	 * Default constructor.
	 */
	public TradeUpdater()
	{
	}

	/**
	 * Main loop.
	 */
	public void run()
	{
		try
		{
			if (debugEnabled) log.debug("starting trade updater");
			TypedProperties conf = JPPFConfiguration.getProperties();
			DataFactory df = new DataFactory();
			// generate random market data
			List<MarketData> marketDataList = df.generateDataMarketObjects(conf.getInt("nbMarketData", 10));
			// generate random trades
			List<Trade> tradeList = df.generateTradeObjects(conf.getInt("nbTrades", 10), marketDataList,
				conf.getInt("minDataPerTrade", 1), conf.getInt("maxDataPerTrade", 5));
			// initialize the nodes: assign an id to each and send them the current market data
			int nbNodes = nodeHandler.initNodes(marketDataList);
			//
			nodeSelector = new NodeSelector(tradeList, nbNodes);
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
			// start the ticker
			Ticker ticker = new Ticker(marketDataList, conf.getInt("minTickerInterval", 50), conf.getInt("maxTickerInterval", 1000),
				conf.getInt("nbTickerEvents", 0));
			ticker.addTickerListener(nodeHandler);
			ticker.addTickerListener(this);
			if (debugEnabled) log.debug("before starting ticker");
			new Thread(ticker, "ticker thread").start();
			// let it run for one minute
			Thread.sleep(conf.getLong("simulationDuration", 60000L));
			// stop the ticker
			ticker.setStopped(true);
			jobExecutor.shutdown();
			while (jobExecutor.isTerminated()) Thread.sleep(10);
			resultsExecutor.shutdown();
			while (resultsExecutor.isTerminated()) Thread.sleep(10);
			if (debugEnabled) log.debug("after stopping ticker");
			print(statsCollector.toString());
		}
		catch(Exception e)
		{
			System.out.println(e.getMessage());
			log.error(e.getMessage(), e);
		}
		finally
		{
		}
	}

	/**
	 * Called when a piece of market data was updated.
	 * @param event encapsulated the market data update.
	 * @see org.jppf.example.datadependency.simulation.TickerListener#marketDataUpdated(org.jppf.example.datadependency.simulation.TickerEvent)
	 */
	public void marketDataUpdated(TickerEvent event)
	{
		if (jobExecutor.isShutdown()) return;
		statsCollector.dataUpdated();
		if (debugEnabled) log.debug("received update event for " + event.getMarketData().getId());
		jobExecutor.submit(new SubmissionTask(event.getMarketData()));
	}

	/**
	 * Print a message to the log and to the system standard output.
	 * @param s the message to print.
	 */
	private void print(String s)
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
		MarketData marketData = null;
		/**
		 * Event notification timestamp.
		 */
		long timestamp = System.currentTimeMillis();

		/**
		 * Initialize this task with the specified market data.
		 * @param marketData the updated market data.
		 */
		public SubmissionTask(MarketData marketData)
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
				if (debugEnabled) log.debug("processing update for " + marketData.getId());
				// determine which trades are impacted
				Set<Trade> tradeSet = dataToTradeMap.get(marketData.getId());
				if (tradeSet == null) return;
				// associate each node with a list of impacted trades
				Map<Integer, List<Trade>> nodeMap = new HashMap<Integer, List<Trade>>();
				for (Trade t: tradeSet)
				{
					int n = nodeSelector.getNodeId(t);
					List<Trade> list = nodeMap.get(n);
					if (list == null)
					{
						list = new ArrayList<Trade>();
						nodeMap.put(n, list);
					}
					list.add(t);
				}
				// create a job for each node
				for (Integer n: nodeMap.keySet())
				{
					JPPFJob job = new JPPFJob();
					job.setId("[Node id=" + n + "] update of " + marketData.getId() + " (" + jobCount.incrementAndGet() + ")");
					job.setBlocking(false);
					job.getJobSLA().setExecutionPolicy(new Equal("id", n));
					List<Trade> list = nodeMap.get(n);
					JPPFResultCollector collector = new JPPFResultCollector(list.size());
					job.setResultListener(collector);
					// create a task for each trade
					for (Trade t: list) job.addTask(new TradeUpdateTask(t));
					jppfClient.submit(job);
					resultsExecutor.submit(new ResultCollectionTask(collector, timestamp));
				}
			}
			catch(Exception e)
			{
				System.out.println(e.getMessage());
				log.error(e.getMessage(), e);
			}
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
	 * The entry point for this application runner to be run from a Java command line.
	 * @param args by default, we do not use the command line arguments,
	 * however nothing prevents us from using them if need be.
	 */
	public static void main(String...args)
	{
		try
		{
			// create the JPPFClient. This constructor call causes JPPF to read the configuration file
			// and connect with one or multiple JPPF drivers.
			jppfClient = new JPPFClient();

			// create a runner instance.
			TradeUpdater tradeUpdater = new TradeUpdater();
			tradeUpdater.run();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if (jppfClient != null) jppfClient.close();
		}
	}
}
