/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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

package sample.nbody;

import org.jppf.server.protocol.JPPFTask;

/**
 * 
 * @author Laurent Cohen
 */
public class NBodyTask extends JPPFTask
{
	/**
	 * The bodies for whihc this task computes the position.
	 */
	private NBody[] bodies = null;

	/**
	 * Initialize this task with the specified parameters.
	 * @param bodies the bodies to handle.
	 */
	public NBodyTask(NBody[] bodies)
	{
		this.bodies = bodies;
	}

	/**
	 * Perform the calculations.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			Vector2d[] positions = (Vector2d[]) getDataProvider().getValue("positions");
			double qp_qp = (Double) getDataProvider().getValue("qp_qp");
			double qp_b = (Double) getDataProvider().getValue("qp_b");
			double dt = (Double) getDataProvider().getValue("dt");

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
			setException(e);
		}
	}

	/**
	 * Compute the distance between 2 points represented by vectors. 
	 * @param v1 the first vector.
	 * @param v2 the second vector.
	 * @return the distance computed as sqrt((v2.x-v1.x)^2 + (v2.y-v1.y)^2).
	 */
	private double distance(Vector2d v1, Vector2d v2)
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
