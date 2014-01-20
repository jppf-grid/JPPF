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


/**
 * Utility class to generate trade and market data objects for simulation.
 * The random number generator uses a uniform distribution.
 * @author Laurent Cohen
 */
public class UniformDataFactory extends AbstractDataFactory
{
  /**
   * Generate a random number in the range [0, value[.
   * @param value the maximum random value (exclusive).
   * @return a pseudo-random number in the specified range.
   * @see org.jppf.example.datadependency.simulation.DataFactory#getRandomInt(int)
   */
  @Override
  public int getRandomInt(final int value)
  {
    synchronized(random)
    {
      return random.nextInt(value);
    }
  }
}
