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
 * This class provides a step function to compute the transitions
 * between two mandelbrot parameter sets.
 */
public abstract class AbstractStepVector {
  /**
   * The relevant values in a Mandelbrot parameter set:
   * x and y of the center, diameter and max number of escape iterations.
   */
  protected double x, y, d, n;

  /**
   * Default constructor provided for subclasses.
   */
  protected AbstractStepVector() {
  }

  /**
   * Initialize this step vector with the specified parameter sets.
   * @param cfg1 the start config.
   * @param cfg2 the end config.
   * @param nbFrames the number of steps.
   */
  protected AbstractStepVector(final MandelbrotConfiguration cfg1, final MandelbrotConfiguration cfg2, final int nbFrames) {
    x = (cfg2.x - cfg1.x) / nbFrames;
    y = (cfg2.y - cfg1.y) / nbFrames;
    d = (cfg2.d - cfg1.d) / nbFrames;
    n = (double) (cfg2.maxIterations - cfg1.maxIterations) / (double) nbFrames;
  }

  /**
   * Get the value to add to the starting x coordinate of the center.
   * @param step the current frame number.
   * @return a double value.
   */
  public double getX(final int step) {
    return stepFunction(step) * x;
  }

  /**
   * Get the value to add to the starting y coordinate of the center.
   * @param step the current frame number.
   * @return a double value.
   */
  public double getY(final int step) {
    return stepFunction(step) * y;
  }

  /**
   * Get the value to add to the diameter of the image.
   * @param step the current frame number.
   * @return a double value.
   */
  public double getD(final int step) {
    return stepFunction(step) * d;
  }

  /**
   * Get the value to add to the starting number of iterations.
   * @param step the current frame number.
   * @return a double value.
   */
  public double getN(final int step) {
    return stepFunction(step) * n;
  }

  /**
   * This is the step function, which provides a visual smoothness
   * when transitioning from a parameter set into another.
   * @param step the frame number within a transitin.
   * @return an increment to add to the previous parmater value.
   */
  protected abstract double stepFunction(double step);
}
