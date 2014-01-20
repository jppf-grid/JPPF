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

/**
 * Represents a point on the screen, with its color.
 * @author Laurent Cohen
 */
public class FractalPoint implements Serializable {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Coordinates and RGB color of this point.
   */
  public int x, y, rgb;

  /**
   * Initialize this point with the specified parameters.
   * @param x x coordinate.
   * @param y y coordinate.
   * @param rgb rgb color.
   */
  public FractalPoint(final int x, final int y, final int rgb) {
    this.x = x;
    this.y = y;
    this.rgb = rgb;
  }

  /**
   * Apply a scale factor to this point.
   * @param scaleFactor the factor to apply as a double.
   * @return this point.
   */
  public FractalPoint scale(final double scaleFactor) {
    x *= scaleFactor;
    y *= scaleFactor;
    return this;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[');
    sb.append("x=").append(x);
    sb.append(", y=").append(y);
    sb.append(", rgb=").append(rgb);
    sb.append(']');
    return sb.toString();
  }
}
