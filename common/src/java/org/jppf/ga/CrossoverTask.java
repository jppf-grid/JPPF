/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

import org.jppf.node.protocol.AbstractTask;

/**
 *
 * @author Laurent Cohen
 */
public class CrossoverTask extends AbstractTask<Chromosome> {
  /**
   * The chromosomes to crossover.
   */
  Chromosome c1,c2;
  /**
   * Index of the resulting chromosme in the population
   */
  public int index;
  /**
   * Crossover position.
   */
  public int pos;

  /**
   *
   * @param c1 first chromosome to crossover.
   * @param c2 second chromosome to crossover.
   * @param index index of the resulting chromosme in the population
   * @param pos crossover position.
   */
  public CrossoverTask(final Chromosome c1, final Chromosome c2, final int index, final int pos) {
    this.c1 = c1;
    this.c2 = c2;
    this.index = index;
    this.pos = pos;
  }

  @Override
  public void run() {
    setResult(c1.crossover(c2, pos));
    c1 = c2 = null;
  }
}
