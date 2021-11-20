/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.ga;

/**
 * Selector interface.
 * @author Laurent Cohen
 */
public interface Selector {
  /**
   * Select the specified number of chromosomes among the specified population.
   * @param population the population from which to select.
   * @param nbSelect the number of chromosomes to select.
   * @return an array of the selected chromosomes.
   */
  Chromosome[] select(Chromosome[] population, int nbSelect);
}
