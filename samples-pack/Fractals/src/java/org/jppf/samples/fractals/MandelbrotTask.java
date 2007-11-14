/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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

package org.jppf.samples.fractals;

import org.jppf.server.protocol.JPPFTask;

/**
 * Instances of this task compute the Mandelbrot algorithm (number of iterations to escape the 
 * Mandelbrot set) for each point of a line in the resulting image.
 * @author Laurent Cohen
 */
public class MandelbrotTask extends JPPFTask
{
	/**
	 * The line number, for which to compute the escape value for each point in the line. 
	 */
	private int b = -1;

	/**
	 * Initialize this task with the specified line number.
	 * @param b the line number as an int value.
	 */
	public MandelbrotTask(int b)
	{
		this.b = b;
	}

	/**
	 * Execute the task.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			// send a start notification
			fireNotification("starting line "+(b+1));
			// retrieve the configuration from the data provider
			FractalConfiguration config =
				(FractalConfiguration) getDataProvider().getValue("config");
			int[] iter = new int[config.asize];
			double bval = config.bmin +
				(double) b * (config.bmax - config.bmin) / (double) config.bsize; 
			double astep = (config.amax - config.amin) / (double) config.asize;
			double aval = config.amin;
			for (int i=0; i<config.asize; i++)
			{
				double x = aval;
				double y = bval;
				int iteration = 0;
				boolean escaped = false;
				while (!escaped && (iteration < config.nmax))
				{
					double x1 = x*x - y*y + aval;
					y = 2*x*y + bval;
					x = x1;
					if (x*x + y*y > 4) escaped = true;
					iteration++;
				}
				iter[i] = iteration;
				aval += astep;
			}
			// set the results
			setResult(iter);
			// send an end notification
			fireNotification("completed line "+(b+1));
		}
		catch(Exception e)
		{
			setException(e);
		}
	}
}
