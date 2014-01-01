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

package org.jppf.example.fractals.mandelbrot;

import org.jppf.example.fractals.AbstractFractalConfiguration;

/**
 * Instances of this class represent the set of parameters for the Mandelbrot algorithm, based on
 * the <a href="http://en.wikipedia.org/wiki/Mandelbrot_set">Mandlebrot set article</a> on Wikipedia.
 * @author Laurent Cohen
 */
public class MandelbrotConfiguration extends AbstractFractalConfiguration
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * diameter of the image.
   */
  public double d;
  /**
   * x coordinate of the image center.
   */
  public double x;
  /**
   * y coordinate of the image center.
   */
  public double y;
  /**
   * lower bound for real part of &quot;c&quot;.
   */
  public double r_lower;
  /**
   * upper bound for real part of &quot;c&quot;.
   */
  public double r_upper;
  /**
   * lower bound for imaginary part of &quot;c&quot;.
   */
  public double i_lower;
  /**
   * upper bound for imaginary part of &quot;c&quot;.
   */
  public double i_upper;
  /**
   * max number of iterations to escape the mandelbrot set.
   */
  public int maxIterations;

  /**
   * Initialize this configuration with the specified parameters.
   * This constructor is used for Mandelbrot fractals.
   * @param xcenter image center x coordinate.
   * @param ycenter image center y coordinate.
   * @param diameter image diameter.
   * @param nmax number of iterations.
   */
  public MandelbrotConfiguration(final double xcenter, final double ycenter, final double diameter, final int nmax) {
    super();
    d = diameter;
    x = xcenter;
    y = ycenter;
    double r = d/2;
    r_lower = x - r;
    r_upper = x + r;
    i_lower = y - r;
    i_upper = y + r;
    maxIterations = nmax;
  }

  /**
   * Initialize this configuration from csv values.
   * @param csv the values expressed as a CSV string.
   */
  public MandelbrotConfiguration(final String csv)
  {
    super(csv);
  }

  @Override
  public String toCSV() {
    StringBuilder sb = new StringBuilder().append(width).append(',').append(height).append(',');
    sb.append(d).append(',').append(x).append(',').append(y).append(',').append(maxIterations);
    return sb.toString();
  }

  @Override
  public MandelbrotConfiguration fromCSV(final String csv) {
    String[] tokens = csv.split(",");
    try {
      width = Integer.valueOf(tokens[0]);
      height = Integer.valueOf(tokens[1]);
      d = Double.valueOf(tokens[2]);
      x = Double.valueOf(tokens[3]);
      y = Double.valueOf(tokens[4]);
      maxIterations = Integer.valueOf(tokens[5]);
      double r = d/2d;
      r_lower = x - r;
      r_upper = x + r;
      i_lower = y - r;
      i_upper = y + r;
    } catch(Exception e) {
      e.printStackTrace();
    }
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("width=").append(width);
    sb.append(", height=").append(height);
    sb.append(", d=").append(d);
    sb.append(", x=").append(x);
    sb.append(", y=").append(y);
    sb.append(", maxIterations=").append(maxIterations);
    sb.append(']');
    return sb.toString();
  }
}
