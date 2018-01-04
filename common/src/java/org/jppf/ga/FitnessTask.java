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
public class FitnessTask extends AbstractTask<Double> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * Chromosome whose fitness to compute.
   */
  private Chromosome c;

  /**
   *
   * @param c hromosome whose fitness to compute.
   */
  public FitnessTask(final Chromosome c) {
    if (c == null) throw new NullPointerException();
    this.c = c;
  }

  @Override
  public void run() {
    setResult(c.computeFitness());
    c = null; // do not serialize/deserialize after execution
  }
}
