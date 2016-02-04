/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.example.fractals;

import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class RunnerFactory
{
  /**
   * Mandelbrot type of fractals.
   */
  public static final String MANDELBROT = "mandelbrot";
  /**
   * Lyapunov type of fractals.
   */
  public static final String LYAPUNOV = "lyapunov";
  /**
   * The map of live runners.
   */
  private static Map<String, AbstractRunner> runners = new Hashtable<>();

  /**
   * Create a runner pf the specified type with the specified option.
   * @param name the type of the fractal runner.
   * @param uiMode whether the runner should update the gui while calculating, or display a progress bar, etc.
   * @return an {@link AbstractRunner} instance.
   */
  public static synchronized AbstractRunner createRunner(final String name, final boolean uiMode) {
    AbstractRunner runner = runners.get(name);
    if (runner == null) {
      switch(name) {
        case MANDELBROT:
          runner = new MandelbrotRunner(uiMode);
          break;
        case LYAPUNOV:
          runner = new LyapunovRunner(uiMode);
          break;
        default: throw new IllegalArgumentException("fractal type '" + name + "' not supported");
      }
      runners.put(name, runner);
    }
    return runner;
  }

  /**
   * Get the runner of the specified type.
   * @param name the type of the fractal runner.
   * @return an {@link AbstractRunner} instance.
   */
  public static synchronized AbstractRunner getRunner(final String name) {
    return runners.get(name);
  }

  /**
   * Release the resources held by the registered runners.
   */
  public static synchronized void dispose() {
    for (AbstractRunner runner: runners.values()) runner.dispose();
  }
}
