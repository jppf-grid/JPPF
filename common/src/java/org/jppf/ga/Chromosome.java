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

import java.io.Serializable;

/**
 * Interface for a chromosome.
 * @author Laurent Cohen
 */
public interface Chromosome extends Comparable<Chromosome>, Serializable {
  /**
   * Get the number of genes.
   * @return the number of genes as an int.
   */
  int getSize();

  /**
   * Get the genes of this chromosome.
   * @return an array of <code>Gene</code> instances.
   */
  Gene[] getGenes();

  /**
   * Get the fitness of this chromosome.
   * @return the fitness as a double value.
   */
  double getFitness();

  /**
   * Set the fitness of this chromosome.
   * @param fitness the fitness as a double value.
   */
  void setFitness(double fitness);

  /**
   * Compute the fitness of this chromosome.
   * @return the fitness as a double value.
   */
  double computeFitness();

  /**
   * Perform the mutation of this chromosome.
   * @return the mutated chromosome.
   */
  Chromosome mutate();

  /**
   * Perform the crossover of this chromosome with one of its mates.
   * @param mate the chromosome to crossover with.
   * @param position determines where the crossover should occur in the gene sequence.
   * @return an array of 2 descendants.
   */
  Chromosome crossover(Chromosome mate, int position);

  /**
   * @return whether this chromosome is valid.
   */
  public boolean isValid();
}
