/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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
 * Representation of a 2-dimensional vector.
 * @author Laurent Cohen
 */
public class Vector2d implements Serializable
{
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
	public Vector2d(double x, double y)
	{
		this.x = x;
		this.y = y;
	}

	/**
	 * Set the coordinates of another vector to this one.
	 * @param other the other vector.
	 * @return this vector.
	 */
	public Vector2d set(Vector2d other)
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
	public Vector2d add(Vector2d other)
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
	public Vector2d subtract(Vector2d other)
	{
		x -= other.x;
		y -= other.y;
		return this;
	}

	/**
	 * Multiply the coordinates of this vector by  the sepecified value.
	 * @param value the value to multiply by.
	 * @return this vector.
	 */
	public Vector2d multiply(double value)
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
