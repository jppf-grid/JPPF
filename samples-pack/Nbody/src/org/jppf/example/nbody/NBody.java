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
 * Representation of a body through its position, velocity and acceleration.
 * @author Laurent Cohen
 */
public class NBody implements Serializable
{
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The total acceleration the body is subjected to.
   */
  public Vector2d acceleration = new Vector2d();
  /**
   * The current velocity of the body.
   */
  public Vector2d velocity = new Vector2d();
  /**
   * The current position of the body.
   */
  public Vector2d pos = null;
  /**
   * Identifier for the body.
   */
  public int number = 0;

  /**
   * Initialize this body with the specified position.
   * @param number the body's order number.
   * @param position the body's position.
   */
  public NBody(final int number, final Vector2d position)
  {
    this.number = number;
    this.pos = position;
  }
}
