/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package org.jppf.server.scheduler.bundle.impl;

import java.util.*;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.jppf.server.scheduler.bundle.*;

/**
 * Simulation of a node to test a bundler.
 * @author Laurent Cohen
 */
public class NodeSimulator
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(NodeSimulator.class);
	/**
	 * Determines whether debugging level is set for logging.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * 
	 */
	private static int nbNodes = 1;
	/**
	 * 
	 */
	private static ExecutorService threadPool = null;
	/**
	 * Static simulated latency.
	 */
	private double latency = 0L;
	/**
	 * Dynamic overhead expressed as milliseconds per megabyte of data transferred.
	 */
	private double dynamicOverhead = 0.0d;
	/**
	 * Relative speed of the node.
	 */
	private double speed = 0d;

	/**
	 * Initialize this simulator with the specified parameters.
	 * @param latency static simulated latency.
	 * @param dynamicOverhead dynamic overhead expressed as milliseconds per megabyte of data transferred.
	 * @param speed relative speed of the node.
	 */
	public NodeSimulator(double latency, double dynamicOverhead, double speed)
	{
		this.latency = latency;
		this.dynamicOverhead = dynamicOverhead;
		this.speed = speed;
		log.info("initializing with "+latency+", "+dynamicOverhead+", "+speed);
	}

	/**
	 * Simulate the execution of a set of tasks.
	 * @param bundler the bundler to send the resulting statistics to.
	 * @param sizeMB the size in megabytes of the data that is transferred.
	 * @param nbTasks the number of simulated tasks.
	 * @param timePerTask the simulated execution time for each task.
	 */
	public void run(Bundler bundler, double sizeMB, int nbTasks, long timePerTask)
	{
		try
		{
			long start = System.currentTimeMillis();
			long a = (long) latency;
			int b = (int) ((latency - a) * 1e6);
			Thread.sleep(a, b);
			Thread.sleep((long) (sizeMB * dynamicOverhead));
			for (int i=0; i<nbTasks; i++) Thread.sleep((long) (timePerTask/speed));
			long elapsed = System.currentTimeMillis() - start;
			bundler.feedback(nbTasks, elapsed);
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Entry point to test this class.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			Random rand = new Random(System.currentTimeMillis());
			Bundler initialBundler = BundlerFactory.createBundler();

			nbNodes = 2;
			int maxTasks = 300;
			double size = 1.5d;
			double dataProviderSize = 0.75d;
			long timePerTask = 10;
			int nbIter = 20;

			System.out.println("Starting simulation with nbNodes="+nbNodes+", maxTasks="+maxTasks+", size="+size+", dataProviderSize="+dataProviderSize+
				", timePerTask="+timePerTask+", nbIter="+nbIter);
			threadPool = Executors.newFixedThreadPool(nbNodes);
			Bundler[] bundlers = new Bundler[nbNodes];
			NodeSimulator[] nodes = new NodeSimulator[nbNodes];
			for (int i=0; i<nbNodes; i++)
			{
				bundlers[i] = initialBundler.copy();
			}
			/*
			for (int i=0; i<nbNodes; i++)
			{
				double r = rand.nextDouble();
				double l = 0.25 + 5 * r;
				double o = 0.4 + r * 5;
				double speed = 0.5 + rand.nextDouble() / 2;
				nodes[i] = new NodeSimulator(l, o, speed);
			}
			*/
			nodes[0] = new NodeSimulator(0.25, 0.5, 1);
			nodes[1] = new NodeSimulator(2, 10, 0.5);
			
			for (int i=0; i<nbIter; i++)
			{
				long start = System.currentTimeMillis();
				int pending = maxTasks;
				while (pending > 0)
				{
					List<Integer> list = new ArrayList<Integer>();
					for (int j=0; j<nbNodes; j++)
					{
						int n = bundlers[j].getBundleSize();
						if (n > pending) n = pending;
						list.add(n);
						pending -= n;
						if (pending <= 0) break;
					}
					if (debugEnabled) log.debug("Iteration #"+i+" : list = "+list);
					List<Future<?>> futures = new ArrayList<Future<?>>();
					for (int j=0; j<list.size(); j++)
					{
						int n = list.get(j);
						double s = n * (size - dataProviderSize) / maxTasks;
						futures.add(threadPool.submit(new SimulatorTask(bundlers[j], s, n, timePerTask, nodes[j])));
					}
					for (Future<?> f: futures) f.get();
				}
				long elapsed = System.currentTimeMillis() - start;
				System.out.println("Iteration #" + i + " performed in " + elapsed + " ms");
			}
			threadPool.shutdownNow();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * .
	 */
	public static class SimulatorTask implements Runnable
	{
		/**
		 * 
		 */
		private Bundler bundler = null;
		/**
		 * 
		 */
		private double sizeMB = 0;
		/**
		 * 
		 */
		private int nbTasks = 0;
		/**
		 * 
		 */
		private long timePerTask = 0;
		/**
		 * 
		 */
		private NodeSimulator simulator = null;

		/**
		 * Simulate the execution of a set of tasks.
		 * @param bundler the bundler to send the resulting statistics to.
		 * @param sizeMB the size in megabytes of the data that is transferred.
		 * @param nbTasks the number of simulated tasks.
		 * @param timePerTask the simulated execution time for each task.
		 * @param simulator the node simulator.
		 */
		public SimulatorTask(Bundler bundler, double sizeMB, int nbTasks, long timePerTask, NodeSimulator simulator)
		{
			this.bundler = bundler;
			this.sizeMB = sizeMB;
			this.nbTasks = nbTasks;
			this.timePerTask = timePerTask;
			this.simulator = simulator;
		}

		/**
		 * Run this task.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			simulator.run(bundler, sizeMB, nbTasks, timePerTask);
		}
	}
}
