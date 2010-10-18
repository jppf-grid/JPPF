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

import org.jppf.example.datadependency.model.Trade;

/**
 * This class is used to determine the association between nodes and trades.
 * @author Laurent Cohen
 */
public class NodeSelector
{
	/**
	 * Distribution of trade sets among the nodes.
	 */
	private List<Set<String>> tradeSets = new ArrayList<Set<String>>();
	/**
	 * The list of node ids.
	 */
	private List<Integer> idList = null;

	/**
	 * Initialize this node selector.
	 * @param trades the list of trades to distribute among the nodes.
	 * @param idList the list of node ids.
	 */
	public NodeSelector(List<Trade> trades, List<Integer> idList)
	{
		this.idList = idList;
		int nbNodes = idList.size();
		for (int i=0; i<nbNodes; i++) tradeSets.add(new HashSet<String>());
		for (int i=0; i<trades.size(); i++)
		{
			int n = i % nbNodes;
			tradeSets.get(n).add(trades.get(i).getId());
		}
	}

	/**
	 * Determine the id of the node where the specified trade was initialized.
	 * @param trade the trade for which to determine a processing node.
	 * @return the id of the node as an int.
	 */
	public int getNodeId(Trade trade)
	{
		return getNodeId(trade.getId());
	}

	/**
	 * Determine the id of the node where the specified trade was initialized.
	 * @param tradeId the id of the trade for which to determine a processing node.
	 * @return the id of the node as an int.
	 */
	public int getNodeId(String tradeId)
	{
		for (int i=0; i<tradeSets.size(); i++)
		{
			if (tradeSets.get(i).contains(tradeId)) return idList.get(i);
		}
		return -1;
	}
}
