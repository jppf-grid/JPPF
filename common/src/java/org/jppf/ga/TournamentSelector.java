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

import java.util.Random;

/**
 * Tornament slection implemntation.
 * @author Laurent Cohen
 */
public class TournamentSelector implements Selector {
  /**
   * The tournament size.
   */
  private int size = 0;
  /**
   * Random number generator.
   */
  protected Random random = new Random(System.nanoTime());

  /**
   * Initialize this selector with the specified size.
   * @param size the tournament size.
   */
  public TournamentSelector(final int size) {
    this.size = size;
  }

  @Override
  public Chromosome[] select(final Chromosome[] population, final int nbSelect) {
    int length = population.length;
    Chromosome[] result = new Chromosome[nbSelect];
    for (int i=0; i<nbSelect; i++) {
      Chromosome[] temp = new Chromosome[size];
      for (int j=0; j<size; j++) temp[j] = population[random.nextInt(length)];
      double max = -Double.MAX_VALUE;
      Chromosome best = null;
      for (Chromosome c: temp) {
        if (c.getFitness() > max) {
          max = c.getFitness();
          best = c;
        }
      }
      result[i] = best;
    }
    return result;
  }
}
