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

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this task compute the Lyapunov lambda exponent for each point of a line in
 * the resulting image.
 * @author Laurent Cohen
 */
public class LyapunovTask extends JPPFTask
{
  /**
   * The line number, for which to compute the lambda exponent for each point in the line.
   */
  private int b = -1;
  /**
   * The computed colors for each computed point.
   */
  protected int[] colors = null;

  /**
   * Initialize this task with the specified line number.
   * @param b the line number as an int value.
   */
  public LyapunovTask(final int b)
  {
    this.b = b;
  }

  /**
   * Execute the task.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      // retrieve the configuration from the data provider
      LyapunovConfiguration config = getDataProvider().getParameter("config");
      double[] lambda = new double[config.height];
      double bval = config.bmin +
      b * (config.bmax - config.bmin) / config.width;
      double astep = (config.amax - config.amin) / config.height;
      double aval = config.amin;
      for (int i=0; i<config.height; i++)
      {
        double x = 0.5d;
        int len = config.sequence.length;
        double r = 0d;
        double sum = 0d;
        for (int n=0; n<config.nmax; n++)
        {
          r = config.sequence[n % len] ? aval : bval;
          x = r * x * (1d - x);
          double term = Math.log(Math.abs(r * (1d - 2d * x)));
          sum += term;
        }
        lambda[i] = sum / config.nmax;
        aval += astep;
      }
      // set the results
      setResult(lambda);
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * Get the computed colors for each computed point.
   * @return an array of int values.
   */
  public int[] getColors()
  {
    return colors;
  }
}
