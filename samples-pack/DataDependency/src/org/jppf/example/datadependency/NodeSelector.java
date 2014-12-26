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

import com.hazelcast.core.Hazelcast;

/**
 * This class is used to determine the association between nodes and trades,
 * and distribute the trades among the nodes using a Hazelcast map for each node.
 * @author Laurent Cohen
 */
public class NodeSelector
{
  /**
   * Mapping of tradeId to corresponding node uuid.
   */
  private Map<String, String> tradeToNodeMap = new Hashtable<>();
  /**
   * Distribution of trade sets among the nodes.
   */
  private Map<String, Map<String, Trade>> nodeToHazelcastMap = new Hashtable<>();
  /**
   * The list of node ids.
   */
  private List<String> nodeIdList = null;

  /**
   * Initialize this node selector and distribute the trades among the nodes.
   * @param trades the list of trades to distribute among the nodes.
   * @param nodeIdList the list of node ids.
   */
  public NodeSelector(final List<Trade> trades, final List<String> nodeIdList)
  {
    System.out.println("populating the trades");
    this.nodeIdList = nodeIdList;
    final int nbNodes = nodeIdList.size();
    final Trade[] tradeArray = trades.toArray(new Trade[0]);
    //for (int i=0; i<tradeArray.length; i++) tradeToNodeMap.put(tradeArray[i].getId(), nodeIdList.get(i % nbNodes));
    ExecutorService executor = Executors.newFixedThreadPool(nbNodes);
    List<Future<?>> futures = new ArrayList<>();
    for (int i=0; i<nbNodes; i++)
    {
      String nodeId = nodeIdList.get(i);
      Map<String, Trade> hazelcastMap = Hazelcast.getMap(ModelConstants.TRADE_MAP_PREFIX + nodeId);
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
  public String getNodeId(final String tradeId)
  {
    return tradeToNodeMap.get(tradeId);
  }

  /**
   * Get the ids of all participating nodes.
   * @return  a list of string ids.
   */
  public List<String> getAllNodeIds()
  {
    return nodeIdList;
  }

  /**
   * Get the trades porcessed by the specified node.
   * @param nodeId the uuid pf the node.
   * @return a mapping of trade ids to the corresponding trade objects.
   */
  public Map<String, Trade> getTradesForNode(final String nodeId)
  {
    return nodeToHazelcastMap.get(nodeId);
  }

  /**
   * Populate the distributed map of trades for a given node.
   */
  public class PopulateTradesTask implements Runnable
  {
    /**
     * The trades to distribute.
     */
    private final Trade[] tradeArray;
    /**
     * The distributed map to populate.
     */
    private final Map<String, Trade> hazelcastMap;
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
    public PopulateTradesTask(final Trade[] tradeArray, final int offset, final int nbNodes, final Map<String, Trade> hazelcastMap)
    {
      this.tradeArray = tradeArray;
      this.offset = offset;
      this.hazelcastMap = hazelcastMap;
      this.nbNodes = nbNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void run()
    {
      String nodeId = nodeIdList.get(offset);
      hazelcastMap.clear();
      for (int i=offset; i<tradeArray.length; i+= nbNodes)
      {
        hazelcastMap.put(tradeArray[i].getId(), tradeArray[i]);
        tradeToNodeMap.put(tradeArray[i].getId(), nodeId);
      }
    }
  }
}
