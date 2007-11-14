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

import java.io.Serializable;

/**
 * Instances of this class represent the set of parameters for either
 * the Lyapunov or Mandelbrot algorithms.<br>
 * The default values are given for a Lyapunov parameter set that results in an image
 * called &quot;Zircon City&quot;. You can see an example of it
 * <a href="http://en.wikipedia.org/wiki/Image:Lyapunov-fractal.png"> on the Wikipedia web site</a>.
 * @author Laurent Cohen
 */
public class FractalConfiguration implements Serializable
{
	/**
	 * Lyapunov: lower bound for &quot;a&quot;.<br>
	 * Mandelbrot: lower bound for real part of &quot;c&quot;.
	 */
	public double amin = 3.4;
	/**
	 * Lyapunov: upper bound for &quot;a&quot;.<br>
	 * Mandelbrot: upper bound for real part of &quot;c&quot;.
	 */
	public double amax = 4;
	/**
	 * Lyapunov: lower bound for &quot;b&quot;.<br>
	 * Mandelbrot: lower bound for imaginary part of &quot;c&quot;.
	 */
	public double bmin = 2.5;
	/**
	 * Lyapunov: upper bound for &quot;b&quot;.<br>
	 * Mandelbrot: upper bound for imaginary part of &quot;c&quot;.
	 */
	public double bmax = 3.4;
	/**
	 * Lyapunov: image height.<br>
	 * Mandelbrot: image width.
	 */
	public int asize = 768;
	/**
	 * Lyapunov: image width.<br>
	 * Mandelbrot: image height.
	 */
	public int bsize = 1024;
	/**
	 * Lyapunov: image width.<br>
	 * Mandelbrot: image height.
	 */
	public int nmax = 1000;
	/**
	 * The sequence to use in the Lyapunov algorithm.
	 */
	public boolean[] sequence = transformSequence("BBBBBBAAAAAA");

	/**
	 * Initialize this configuration with default parameters. 
	 */
	public FractalConfiguration()
	{
	}

	/**
	 * Initialize this configuration with the specified parameters.<br>
	 * This constructor is used for Lyapunov fractals.
	 * @param amin lower bound for a.
	 * @param amax upper bound for a.
	 * @param bmin lower bound for b.
	 * @param bmax upper bound for b.
	 * @param asize image height.
	 * @param bsize image width
	 * @param nmax definition (also called N).
	 * @param seq sequence of As and Bs, used only for Lyapunov algorithm.
	 */
	public FractalConfiguration(double amin, double amax, double bmin, double bmax,
		int asize, int bsize, int nmax, String seq)
	{
		this.amin = amin;
		this.amax = amax;
		this.bmin = bmin;
		this.bmax = bmax;
		this.asize = asize;
		this.bsize = bsize;
		this.nmax = nmax;
		this.sequence = transformSequence(seq);
	}

	/**
	 * Initialize this configuration with the specified parameters. 
	 * This constructor is used for Mandelbrot fractals.
	 * @param xcenter image center x coordinate.
	 * @param ycenter image center y coordinate.
	 * @param diameter image diameter.
	 * @param asize image width.
	 * @param bsize image height
	 * @param nmax number of iterations.
	 */
	public FractalConfiguration(double xcenter, double ycenter, double diameter, int asize, int bsize, int nmax)
	{
		double r = diameter/2;
		this.amin = xcenter - r;
		this.amax = xcenter + r;
		this.bmin = ycenter - r;
		this.bmax = ycenter + r;
		this.asize = asize;
		this.bsize = bsize;
		this.nmax = nmax;
	}

	/**
	 * Transforms a sequence of As and Bs into an array of booleans, true corresponding to A and false to B.
	 * @param seq sequence of As and Bs.
	 * @return an array of boolean values, of same length as the initial sequence.
	 */
	public static boolean[] transformSequence(String seq)
	{
		boolean[] booleanSequence = null;
		if (seq != null)
		{
			booleanSequence = new boolean[seq.length()];
			for (int i=0; i<seq.length(); i++)
			{
				booleanSequence[i] = (seq.charAt(i) == 'A') || (seq.charAt(i) == 'a');
			}
		}
		return booleanSequence;
	}
}
