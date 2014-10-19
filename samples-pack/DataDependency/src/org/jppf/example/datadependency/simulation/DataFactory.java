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

import java.util.List;

import org.jppf.example.datadependency.model.*;

/**
 * Interface for all classes used to generate random data for the simulation.
 * The random number generator uses a gaussian distribution.
 * @author Laurent Cohen
 */
public interface DataFactory
{
  /**
   * Generate a random number in the range [0, value[.
   * @param value the maximum random value (exclusive).
   * @return a pseudo-random number in the specified range.
   */
  int getRandomInt(int value);

  /**
   * Generate a random number in the specified range.
   * @param min the minimum random value.
   * @param max the maximum random value.
   * @return a pseudo-random number in the specified range.
   */
  int getRandomInt(int min, int max);

  /**
   * Generate the specified number of data market objects.
   * Each generated object has an id in the format &quot;Dn&quot; where <i>n</i> is a sequence number.
   * @param n the number of objects to generate.
   * @return a list of <code>MarketData</code> instances.
   */
  List<MarketData> generateDataMarketObjects(int n);

  /**
   * Generate a list of trade objects with their dependencies.
   * Each generated object has an id in the format &quot;Tn&quot; where <i>n</i> is a sequence number.
   * The dependencies are randomly chosen from the specified list of data market objects.
   * and their number varies between the specified min and max values.
   * @param nbTrades the number of trade objects to generate.
   * @param dataList the list of market data objects to create the dependencies from.
   * @param minData the minimum number of dependencies per trade (inclusive).
   * @param maxData the maximum number of dependencies per trade (inclusive).
   * @return a list of <code>Trade</code> instances.
   */
  List<Trade> generateTradeObjects(int nbTrades, List<MarketData> dataList, int minData, int maxData);

}
