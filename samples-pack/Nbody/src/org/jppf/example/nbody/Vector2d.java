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

//******************************************************************************
// This Java source file is copyright (C) 2008 by Alan Kaminsky. All rights
// reserved. For further information, contact the author, Alan Kaminsky, at
// ark@cs.rit.edu.
//
// This Java source file is part of the Parallel Java Library ("PJ"). PJ is free
// software; you can redistribute it and/or modify it under the terms of the GNU
// General Public License as published by the Free Software Foundation; either
// version 3 of the License, or (at your option) any later version.
//
// PJ is distributed in the hope that it will be useful, but WITHOUT ANY
// WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
// A PARTICULAR PURPOSE. See the GNU General Public License for more details.
//
// A copy of the GNU General Public License is provided in the file gpl.txt. You
// may also obtain a copy of the GNU General Public License on the World Wide
// Web at http://www.gnu.org/licenses/gpl.html.
//
//******************************************************************************

package org.jppf.example.nbody;

import java.io.Serializable;

/**
 * Representation of a 2-dimensional vector.
 * @author Laurent Cohen
 */
public class Vector2d implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * X coordinate.
   */
  public double x = 0d;
  /**
   * Y coordinate.
   */
  public double y = 0d;

  /**
   * Default constructor.
   */
  public Vector2d()
  {
  }

  /**
   * Initialize this vector with the specified coordinates.
   * @param x x coordinate.
   * @param y y coordinate.
   */
  public Vector2d(final double x, final double y)
  {
    this.x = x;
    this.y = y;
  }

  /**
   * Set the coordinates of another vector to this one.
   * @param other the other vector.
   * @return this vector.
   */
  public Vector2d set(final Vector2d other)
  {
    x = other.x;
    y = other.y;
    return this;
  }

  /**
   * Add the coordinates of another vector to this one.
   * @param other the other vector.
   * @return this vector.
   */
  public Vector2d add(final Vector2d other)
  {
    x += other.x;
    y += other.y;
    return this;
  }

  /**
   * Add the coordinates of another vector from this one.
   * @param other the other vector.
   * @return this vector.
   */
  public Vector2d subtract(final Vector2d other)
  {
    x -= other.x;
    y -= other.y;
    return this;
  }

  /**
   * Multiply the coordinates of this vector by  the specified value.
   * @param value the value to multiply by.
   * @return this vector.
   */
  public Vector2d multiply(final double value)
  {
    x *= value;
    y *= value;
    return this;
  }

  /**
   * Rotate this vector 90 degrees counterclockwise.
   * @return  This vector, rotated.
   */
  public Vector2d rotate90()
  {
    double tmp = this.x;
    this.x = -this.y;
    this.y = tmp;
    return this;
  }

  /**
   * Rotate this vector 180 degrees.
   * @return  This vector, rotated.
   */
  public Vector2d rotate180()
  {
    this.x = -this.x;
    this.y = -this.y;
    return this;
  }

  /**
   * Rotate this vector 270 degrees counterclockwise (90 degrees clockwise).
   * @return  This vector, rotated.
   */
  public Vector2d rotate270()
  {
    double tmp = this.x;
    this.x = this.y;
    this.y = -tmp;
    return this;
  }

  /**
   * Reset the coordinates to zero.
   * @return  This vector, rotated.
   */
  public Vector2d clear()
  {
    this.x = 0d;
    this.y = 0d;
    return this;
  }
}
