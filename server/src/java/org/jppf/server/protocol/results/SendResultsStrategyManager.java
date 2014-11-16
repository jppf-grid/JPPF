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

package org.jppf.server.protocol.results;

import java.util.*;

/**
 * This class manages the loading and use of strategies for sending results back to the JPPF clients.
 * @author Laurent Cohen
 * @exclude
 */
public class SendResultsStrategyManager
{
  /**
   * A mapping of strategy names to {@link SendResultsStrategy} instances.
   */
  private static Map<String, SendResultsStrategy> strategyMap = initializeMap();
  /**
   * The default strategy to use when a strategy with a given name cannot be found.
   */
  private static final SendResultsStrategy DEFAULT_STRATEGY = strategyMap.get(SendResultsStrategyConstants.NODE_RESULTS);

  /**
   * Initialize the map of available strategies.
   * @return a mapping of strategy names to {@link SendResultsStrategy} instances.
   */
  private static Map<String, SendResultsStrategy> initializeMap()
  {
    Map<String, SendResultsStrategy> map = new HashMap<>();
    map.put(SendResultsStrategyConstants.ALL_RESULTS, new SendResultsStrategy.SendAllResultsStrategy());
    map.put(SendResultsStrategyConstants.NODE_RESULTS, new SendResultsStrategy.SendNodeResultsStrategy());
    return map;
  }

  /**
   * Get a strategy from its name.
   * If no strategy with this name is found, then {@link #DEFAULT_STRATEGY} is returned.
   * @param name the name of the strtaegy to find.
   * @return a {@link SendResultsStrategy} instance.
   */
  public static SendResultsStrategy getStrategy(final String name)
  {
    if (name == null) return DEFAULT_STRATEGY;
    SendResultsStrategy strategy = strategyMap.get(name);
    return strategy != null ? strategy : DEFAULT_STRATEGY;
  }
}
