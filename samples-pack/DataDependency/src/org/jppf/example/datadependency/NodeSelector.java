/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import org.jppf.example.datadependency.model.Trade;

import com.hazelcast.core.Hazelcast;

/**
 * This class is used to determine the association between nodes and trades.
 * @author Laurent Cohen
 */
public class NodeSelector
{
	/**
	 * Mapping of tradeId to corresponding node uuid.
	 */
	private Map<String, String> tradeToNodeMap = new HashMap<String, String>();
	/**
	 * Distribution of trade sets among the nodes.
	 */
	private Map<String, Map<Object, Object>> nodeToHazelcastMap = new HashMap<String, Map<Object, Object>>();
	/**
	 * The list of node ids.
	 */
	private List<String> idList = null;

	/**
	 * Initialize this node selector and distribute the trades among the nodes.
	 * @param trades the list of trades to distribute among the nodes.
	 * @param idList the list of node ids.
	 */
	public NodeSelector(List<Trade> trades, List<String> idList)
	{
		System.out.println("populating the trades");
		this.idList = idList;
		final int nbNodes = idList.size();
		final Trade[] tradeArray = trades.toArray(new Trade[0]);
		for (int i=0; i<tradeArray.length; i++) tradeToNodeMap.put(tradeArray[i].getId(), idList.get(i % nbNodes));
		ExecutorService executor = Executors.newFixedThreadPool(nbNodes);
		List<Future<?>> futures = new ArrayList<Future<?>>();
		for (int i=0; i<nbNodes; i++)
		{
			String nodeId = idList.get(i);
			Map<Object, Object> hazelcastMap = Hazelcast.getMap("trades-" + nodeId);
			nodeToHazelcastMap.put(nodeId, hazelcastMap);
			futures.add(executor.submit(new PopulateTradesTask(tradeArray, i, nbNodes, hazelcastMap)));
		}
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
		executor.shutdownNow();
		System.out.println("end of populating the trades");
	}

	/**
	 * Determine the id of the node where the specified trade was initialized.
	 * @param tradeId the id of the trade for which to determine a processing node.
	 * @return the id of the node as a string.
	 */
	public String getNodeId(String tradeId)
	{
		return tradeToNodeMap.get(tradeId);
	}

	/**
	 * Populate the distributed map of trades for a given node.
	 */
	public static class PopulateTradesTask implements Runnable
	{
		/**
		 * The trades to distribute.
		 */
		private final Trade[] tradeArray;
		/**
		 * The distributed map to populate.
		 */
		private final Map<Object, Object> hazelcastMap;
		/**
		 * The offset of the node in the list of nodes.
		 */
		private final int offset;
		/**
		 * The number of nodes.
		 */
		private final int nbNodes;

		/**
		 * Populate the specified distributes map with the specified trades for the specified node.
		 * @param tradeArray he trades to distribute.
		 * @param offset the offset of the node in the list of nodes.
		 * @param nbNodes the number of nodes.
		 * @param hazelcastMap the distributed map to populate.
		 */
		public PopulateTradesTask(Trade[] tradeArray, int offset, int nbNodes, Map<Object, Object> hazelcastMap)
		{
			this.tradeArray = tradeArray;
			this.offset = offset;
			this.hazelcastMap = hazelcastMap;
			this.nbNodes = nbNodes;
		}

		/**
		 * {@inheritDoc}
		 */
		public void run()
		{
			for (int i=offset; i<tradeArray.length; i+= nbNodes)
			{
				hazelcastMap.put(tradeArray[i].getId(), tradeArray[i]);
			}
		}
	}
}
