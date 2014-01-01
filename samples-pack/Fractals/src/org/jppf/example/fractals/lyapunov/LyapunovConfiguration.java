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

package org.jppf.example.fractals.lyapunov;

import org.jppf.example.fractals.AbstractFractalConfiguration;

/**
 * Instances of this class represent the set of parameters for either
 * the Lyapunov or Mandelbrot algorithms.<br>
 * The default values are given for a Lyapunov parameter set that results in an image
 * called &quot;Zircon City&quot;. You can see an example of it
 * <a href="http://en.wikipedia.org/wiki/Image:Lyapunov-fractal.png"> on the Wikipedia web site</a>.
 * @author Laurent Cohen
 */
public class LyapunovConfiguration extends AbstractFractalConfiguration
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Lyapunov: lower bound for &quot;a&quot;.<br>
   */
  public double amin = 3.4;
  /**
   * Lyapunov: upper bound for &quot;a&quot;.<br>
   */
  public double amax = 4;
  /**
   * Lyapunov: lower bound for &quot;b&quot;.<br>
   */
  public double bmin = 2.5;
  /**
   * Lyapunov: upper bound for &quot;b&quot;.<br>
   */
  public double bmax = 3.4;
  /**
   * Lyapunov: ???.<br>
   */
  public int nmax = 1000;
  /**
   * The sequence to use in the Lyapunov algorithm.
   */
  public boolean[] sequence = transformSequence("BBBBBBAAAAAA");

  /**
   * Initialize this configuration with default parameters.
   */
  public LyapunovConfiguration()
  {
  }

  /**
   * Initialize this configuration with the specified parameters.<br>
   * This constructor is used for Lyapunov fractals.
   * @param amin lower bound for a.
   * @param amax upper bound for a.
   * @param bmin lower bound for b.
   * @param bmax upper bound for b.
   * @param asize image height.
   * @param bsize image width
   * @param nmax definition (also called N).
   * @param seq sequence of As and Bs, used only for Lyapunov algorithm.
   */
  public LyapunovConfiguration(final double amin, final double amax, final double bmin, final double bmax,
      final int asize, final int bsize, final int nmax, final String seq)
  {
    this.amin = amin;
    this.amax = amax;
    this.bmin = bmin;
    this.bmax = bmax;
    this.height = asize;
    this.width = bsize;
    this.nmax = nmax;
    this.sequence = transformSequence(seq);
  }

  /**
   * Transforms a sequence of As and Bs into an array of booleans, true corresponding to A and false to B.
   * @param seq sequence of As and Bs.
   * @return an array of boolean values, of same length as the initial sequence.
   */
  public static boolean[] transformSequence(final String seq)
  {
    boolean[] booleanSequence = null;
    if (seq != null)
    {
      booleanSequence = new boolean[seq.length()];
      for (int i=0; i<seq.length(); i++)
      {
        booleanSequence[i] = (seq.charAt(i) == 'A') || (seq.charAt(i) == 'a');
      }
    }
    return booleanSequence;
  }

  @Override
  public String toCSV()
  {
    return null;
  }

  @Override
  public LyapunovConfiguration fromCSV(final String csv)
  {
    return this;
  }
}
