/*
 * JPPF.
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

import java.io.Serializable;

/**
 * Representation of a body through its position, velocity and acceleration.
 * @author Laurent Cohen
 */
public class NBody implements Serializable
{
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
	 * Initialize this body with the spceified position. 
	 * @param number the body's order number.
	 * @param position the body's position.
	 */
	public NBody(int number, Vector2d position)
	{
		this.number = number;
		this.pos = position;
	}
}
