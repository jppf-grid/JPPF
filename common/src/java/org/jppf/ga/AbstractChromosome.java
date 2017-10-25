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
 * Common abstract super class for chromosme implementations.
 * @author Laurent Cohen
 */
public abstract class AbstractChromosome implements Chromosome {
  /**
   * The fitness of this chromosme.
   */
  protected double fitness;
  /**
   * The genes of this chromosome.
   */
  protected Gene[] genes;
  /**
   * The size (number of genes) of this chromosome.
   */
  protected int size;
  /**
   * A random number generator.
   */
  protected Random random = new Random(System.nanoTime());

  @Override
  public double getFitness() {
    return fitness;
  }

  @Override
  public void setFitness(final double fitness) {
    this.fitness = fitness;
  }

  @Override
  public Gene[] getGenes() {
    return genes;
  }

  /**
   * Set the genes of this chromosome.
   * @param genes the array of genes to set.
   */
  public void setGenes(final Gene[] genes) {
    this.genes = genes;
  }

  @Override
  public int getSize() {
    return size;
  }

  @Override
  public int compareTo(final Chromosome other) {
    if (fitness > other.getFitness()) return 1;
    if (fitness < other.getFitness()) return -1;
    return 0;
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
