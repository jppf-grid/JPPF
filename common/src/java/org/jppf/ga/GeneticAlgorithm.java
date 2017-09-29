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

import java.util.*;
import java.util.concurrent.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 *
 * @author Laurent Cohen
 */
public class GeneticAlgorithm implements AutoCloseable {
  
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(GeneticAlgorithm.class);
  /**
   *
   */
  static ExecutorService executor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("GA"));
  /**
   * The genetic algorithm's population.
   */
  protected Chromosome[] population;
  /**
   * The count of generations (epochs).
   */
  protected int generationCount;
  /**
   * The selector.
   */
  private Selector selector;
  /**
   * The number of chromosomes to select.
   */
  protected int nbSelect;
  /**
   * Probability of corssover for each chromosome.
   */
  protected double crossoverProbability = 1d;
  /**
   *
   */
  protected Random random = new Random(System.nanoTime());
  /**
   * How often to display the latest state.
   */
  public int outputFrequency = 1;
  /**
   *
   */
  protected int nbToKeep;
  /**
   *
   */
  protected double nbToKeepPct;

  /**
   * 
   * @param population the inital population.
   * @param nbToKeep the number of chromosomes to keep at each generation.
   * @param nbSelect the number of chromosomes to select at each generation.
   * @param tournamentSize the tournament selector size.
   * @param crossoverProbability probability of crossover for each chromosome.
   */
  public GeneticAlgorithm(final Chromosome[] population, final int nbToKeep, final int nbSelect, final int tournamentSize, final double crossoverProbability) {
    this.population = population;
    this.nbSelect = nbSelect;
    this.crossoverProbability = crossoverProbability;
    selector = new TournamentSelector(tournamentSize);
    this.nbToKeep = nbToKeep;
    nbToKeepPct = (double) nbToKeep / (double) population.length;
  }

  /**
   *
   * @param maxGenerations max number of generations (epochs) to run for.
   * @return the chromosome with the best fitness score.
   */
  public Chromosome run(final int maxGenerations) {
    Chromosome best = null;
    try {
      double bestFitness = Double.NEGATIVE_INFINITY;
      generationCount = 0;
      computeFitness(population);
      testValid();
      long start = System.nanoTime();
      while (generationCount < maxGenerations) {
        Arrays.sort(population);
        Chromosome[] pop = selector.select(population, nbSelect);
        population = performCrossover(pop);
        if (population[0] == null) throw new NullPointerException();
        population = performMutation(population, nbSelect);
        if (population[0] == null) throw new NullPointerException();
        computeFitness(population);
        if (generationCount % outputFrequency == 0) {
          long elapsed = (System.nanoTime() - start) / 1_000_000L;
          displayStats(generationCount, population, elapsed);
          start = System.nanoTime();
        }
        generationCount++;

        for (Chromosome c: population) {
          if (c.getFitness() > bestFitness) {
            best = c;
            bestFitness = c.getFitness();
          }
        }
        if (shouldStop(best)) break;
        postEpoch(best);
      }
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return best;
  }

  /**
   * @return the current population size.
   */
  public int getCurrentPopulationSize() {
    return this.population.length;
  }

  /**
   * Callback invoked at the end of each epoch. Intended to be overriden in subclasses.
   * @param best the chromosome with the best fitness score.
   */
  protected void postEpoch(@SuppressWarnings("unused") final Chromosome best) {
  }

  /**
   * 
   * @param best the chromosome with the best fitness score.
   * @return whether to stop the algorithm.
   */
  protected boolean shouldStop(@SuppressWarnings("unused") final Chromosome best) {
    return false;
  }

  /**
   * Compute the fitness scores for the specified population.
   * @param pop the chromosomes for which to compute the fitness.
   */
  protected void computeFitness(final Chromosome[] pop) {
    CompletionService<Runnable> cs = new ExecutorCompletionService<>(executor);
    for (final Chromosome c: pop) {
      Runnable r = new Runnable() {
        @Override
        public void run() {
          c.computeFitness();
        }
      };
      cs.submit(r, null);
    }
    try {
      for (int i=0; i<pop.length; i++) cs.take();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Perform the crossover operation for the specified population.
   * @param pop the chromosomes to crossover.
   * @return the new population obtained by corssover.
   */
  public Chromosome[] performCrossover(final Chromosome[] pop) {
    int populationSize = population.length;
    final Chromosome[] newPop = new Chromosome[populationSize];
    for (int i=0; i <nbToKeep; i++) newPop[i] = population[i];
    CompletionService<Runnable> cs = new ExecutorCompletionService<>(executor);
    int totalSUbmitted = 0;
    for  (int count=nbToKeep; count<populationSize; count++) {
      final int n1 = random.nextInt(pop.length);
      if (random.nextDouble() < crossoverProbability) {
        final int cnt = count;
        totalSUbmitted++;
        Runnable r = new Runnable() {
          @Override
          public void run() {
            int n2;
            while ((n2 = random.nextInt(pop.length)) == n1);
            //int pos = (pop[n1].getSize() / 2) + (pop[n1].getSize() % 2);
            int pos = 1 + random.nextInt(pop[n1].getSize() - 1);
            newPop[cnt] = pop[n1].crossover(pop[n2], pos);
          }
        };
        cs.submit(r, null);
      } else newPop[count] = pop[n1];
    }
    try {
      for (int i=0; i<totalSUbmitted; i++) cs.take();
    } catch(Exception e) {
      e.printStackTrace();
    }
    return newPop;
  }

  /**
   * Mutate the specified population.
   * @param pop the population to mutate.
   * @param nbSelect the number of chromosmes to mutate.
   * @return the mutated population.
   */
  public Chromosome[] performMutation(final Chromosome[] pop, final int nbSelect) {
    final Chromosome[] newPop = new Chromosome[pop.length];
    CompletionService<Runnable> cs = new ExecutorCompletionService<>(executor);
    for (int i=0; i<nbSelect; i++) {
      final int n = i;
      cs.submit(new Runnable() {
        @Override
        public void run() {
          newPop[n] = pop[n].mutate();
        }
      }, null);
    }
    try {
      for (int i=0; i<nbSelect; i++) cs.take();
    } catch(Exception e) {
      e.printStackTrace();
    }
    System.arraycopy(pop, nbSelect, newPop, nbSelect, pop.length - nbSelect);
    return newPop;
  }

  /**
   * 
   * @param gen .
   * @param pop .
   * @param elapsed .
   */
  public void displayStats(final int gen, final Chromosome[] pop, final long elapsed) {
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    double avg = 0d;
    for (Chromosome c: pop) {
      double fitness = c.getFitness();
      avg += fitness;
      if (fitness < min) min = fitness;
      if (fitness > max) max = fitness;
    }
    avg /= pop.length;
    StringBuilder sb = new StringBuilder("gen=").append(StringUtils.padRight("" + gen, ' ', 5));
    sb.append(", max=").append(StringUtils.padRight("" + max, ' ', 21));
    sb.append(", min=").append(StringUtils.padRight("" + min, ' ', 21));
    sb.append(", avg=").append(StringUtils.padRight("" + avg, ' ', 21));
    sb.append(" (").append(StringUtils.toStringDuration(elapsed)).append(")");
    sb.append(", population=").append(pop.length);
    System.out.println(sb.toString());
  }

  /**
   * 
   * @return whether the population is valid.
   */
  public boolean testValid() {
    for (Chromosome c: population) {
      if (!((AbstractChromosome) c).isValid()) {
        return false;
      }
    }
    return true;
  }

  @Override
  public void close() {
  }

  /**
   * Add new chromosomes to the population.
   * @param toAdd the chromosomes to add.
   * @return the new population.
   */
  public Chromosome[] addToPoulation(final Chromosome[] toAdd) {
    int n = population.length;
    int n2 = toAdd.length;
    Chromosome[] newPop = new Chromosome[n  + n2];
    System.arraycopy(population, 0, newPop, 0, n);
    System.arraycopy(toAdd, 0, newPop, n, n2);
    population = newPop;
    this.nbToKeep = (int) (n * nbToKeepPct / 100d);
    this.nbSelect = n - this.nbToKeep;
    return population;
  }

  /**
   * Get the current number of generations.
   * @return the generation count as an int.
   */
  public int getGenerationCount() {
    return generationCount;
  }
}
