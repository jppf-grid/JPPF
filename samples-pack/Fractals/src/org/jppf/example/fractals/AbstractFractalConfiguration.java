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

package org.jppf.example.fractals;

import java.io.Serializable;

import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.TypedProperties;

/**
 * Instances of this class represent the set of parameters for the Mandelbrot algorithm, based on
 * the <a href="http://en.wikipedia.org/wiki/Mandelbrot_set">Mandlebrot set article</a> on Wikipedia.
 * @author Laurent Cohen
 */
public abstract class AbstractFractalConfiguration implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * image width.
   */
  public int width = 0;
  /**
   * image height.
   */
  public int height = 0;

  /**
   * Initialize this configuration.
   */
  public AbstractFractalConfiguration() {
    TypedProperties jppfConfig = JPPFConfiguration.getProperties();
    this.width = jppfConfig.getInt("image.width", 800);
    this.height = jppfConfig.getInt("image.height", 600);
  }

  /**
   * Initialize this configuration from csv values.
   * @param csv the values expressed as a CSV string.
   */
  public AbstractFractalConfiguration(final String csv) {
    fromCSV(csv);
  }

  /**
   * Convert this ocnfiguration to a CSV string.
   * @return a string.
   */
  public abstract String toCSV();

  /**
   * Convert this ocnfiguration to a CSV string.
   * @param csv a csv string.
   * @return this configuration.
   */
  public abstract AbstractFractalConfiguration fromCSV(final String csv);
}
