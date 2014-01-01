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

package org.jppf.example.fractals.moviegenerator;

import org.jppf.example.fractals.mandelbrot.MandelbrotConfiguration;

/**
 * This class provides a linear step function to compute the transitions
 * between two mandelbrot parameter sets.
 */
public class LinearStepVector extends AbstractStepVector {
  /**
   * Initialize this step vector with the specified parameter sets.
   * @param cfg1 the start config.
   * @param cfg2 the end config.
   * @param nbFrames the number of steps.
   */
  public LinearStepVector(final MandelbrotConfiguration cfg1, final MandelbrotConfiguration cfg2, final int nbFrames) {
    super(cfg1, cfg2, nbFrames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("d=").append(d);
    sb.append(", x=").append(x);
    sb.append(", y=").append(y);
    sb.append(", n=").append(n);
    sb.append(']');
    return sb.toString();
  }

  @Override
  protected double stepFunction(final double step) {
    return step;
  }
}
