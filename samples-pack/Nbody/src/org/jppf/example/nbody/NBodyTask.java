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

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class NBodyTask extends JPPFTask
{
  /**
   * The bodies for which this task computes the position.
   */
  private NBody[] bodies = null;

  /**
   * Initialize this task with the specified parameters.
   * @param bodies the bodies to handle.
   */
  public NBodyTask(final NBody[] bodies)
  {
    this.bodies = bodies;
  }

  /**
   * Perform the calculations.
   * @see java.lang.Runnable#run()
   */
  @Override
  public void run()
  {
    try
    {
      Vector2d[] positions = getDataProvider().getParameter("positions");
      double qp_qp = getDataProvider().getParameter("qp_qp");
      double qp_b = getDataProvider().getParameter("qp_b");
      double dt = getDataProvider().getParameter("dt");

      for (NBody body: bodies)
      {
        Vector2d temp = new Vector2d();
        for (int i=0; i<positions.length; i++)
        {
          if (i == body.number) continue;
          double d = distance(body.pos, positions[i]);
          temp.set(body.pos);
          temp.subtract(positions[i]);
          temp.multiply(qp_qp / (d*d*d));
          body.acceleration.add(temp);
        }

        // Accumulate acceleration on antiproton from magnetic field ==> repulsive force of the trap
        temp.set(body.velocity).multiply(qp_b).rotate270();
        body.acceleration.add(temp);
        // Update antiproton's position and velocity.
        temp.set(body.velocity);
        body.pos.add(temp.multiply(dt));
        temp.set(body.acceleration);
        body.pos.add(temp.multiply(Math.sqrt(dt)/2d));
        temp.set(body.acceleration);
        body.velocity.add(temp.multiply(dt));

        // Clear antiproton's acceleration for the next step.
        body.acceleration.clear();
      }
    }
    catch(Exception e)
    {
      setThrowable(e);
    }
  }

  /**
   * Compute the distance between 2 points represented by vectors.
   * @param v1 the first vector.
   * @param v2 the second vector.
   * @return the distance computed as sqrt((v2.x-v1.x)^2 + (v2.y-v1.y)^2).
   */
  private double distance(final Vector2d v1, final Vector2d v2)
  {
    double dx = v2.x - v1.x;
    double dy = v2.y - v1.y;
    return Math.sqrt(dx*dx + dy*dy);
  }

  /**
   * Get the bodies for which this task computes the position.
   * @return an array of <code>NBody</code> instances.
   */
  public synchronized NBody[] getBodies()
  {
    return bodies;
  }
}
