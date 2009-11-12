/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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
package org.jppf.samples.fractals;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.concurrent.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import org.apache.commons.logging.*;
import org.jppf.client.*;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.ui.options.Option;
import org.jppf.utils.StringUtils;

/**
 * Runner class for the Lyapunov and Mandelbrot fractals sample application.
 * @author Laurent Cohen
 */
public class FractalRunner
{
	/**
	 * Logger for this class.
	 */
	static Log log = LogFactory.getLog(FractalRunner.class);
	/**
	 * JPPF client used to submit execution requests.
	 */
	private static JPPFClient jppfClient = null;
	/**
	 * Performs the submission of computations to JPPF.
	 */
	private static ExecutorService executor = Executors.newFixedThreadPool(1);
	/**
	 * A reference to the window displayed while waiting for the end of the computation.
	 */
	private static JWindow window = null;
	/**
	 * The option holding the image in the UI.
	 */
	private static Option option = null;

	/**
	 * Execute for the specified number of iterations.
	 * @param doMandelbrot determines whether to generate a Mandelbrot (true) or Lyapunov (false) fractal.
	 * @param config holds the fractal alogrithm parameters required for the computation.
	 * @param option the option holding the image.
	 * @throws Exception if an error is raised during the execution.
	 */
	public static synchronized void perform(boolean doMandelbrot, FractalConfiguration config, Option option)
		throws Exception
	{
			FractalRunner.option = option;
			createOrDisplayWaitWindow();
			FractalExecution exec = new FractalExecution(doMandelbrot, config);
			executor.submit(exec);
	}

	/**
	 * Execute for the specified number of iterations.
	 * @param doMandelbrot determines whether to generate a Mandelbrot (true) or Lyapunov (false) fractal.
	 * @param config holds the fractal alogrithm parameters required for the computation.
	 * @return a generated image that can be displayed in a UI or saved as a file.
	 * @throws Exception if an error is raised during the execution.
	 */
	public static synchronized Image doPerform(boolean doMandelbrot, FractalConfiguration config) throws Exception
	{
		if (jppfClient == null) jppfClient = new JPPFClient();
		/*
		if (doMandelbrot)
		{
			config = new FractalConfiguration(-0.7, 0, 3.0769, 1600, 1200, 100);
			//config = new FractalConfiguration(-0.759856, 0.125547, 0.051579, 1024, 768, 300);
			//config = new FractalConfiguration(-0.7435669, 0.1314023, 0.0022878, 1024, 768, 150);
			//config = new FractalConfiguration(-0.7436447860, 0.1318252536, 0.0000029336, 1024, 768, 2500);
		}
		else
		{
			config = new FractalConfiguration(3.4, 4, 2.5, 3.4, 768, 1024, 400, "BBBBBBAAAAAA");
		}
		*/
		int nbTask = config.bsize;
		log.info("Executing " + nbTask + " tasks");
		DataProvider dp = new MemoryMapDataProvider();
		dp.setValue("config", config);
		JPPFJob job = new JPPFJob(dp);
		job.setId("Mandelbrot fractal");
		long start = System.currentTimeMillis();
		for (int i=0; i<nbTask; i++) job.addTask(doMandelbrot ? new MandelbrotTask(i) : new LyapunovTask(i));
		// submit the tasks for execution
		List<JPPFTask> results = jppfClient.submit(job);
		long elapsed = System.currentTimeMillis() - start;
		log.info("Computation performed in "+StringUtils.toStringDuration(elapsed));
		//JPPFStats stats = jppfClient.requestStatistics();
		//if (stats != null) log.info("End statistics :\n"+stats.toString());
		
		final Image image = doMandelbrot ? generateMandelbrotImage(results, config) : generateLyapunovImage(results, config);
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				ImagePanel panel = (ImagePanel) option.getUIComponent();
				panel.setImage(image);
				panel.setVisible(false);
				panel.setVisible(true);
			}
		});

		hideWaitWindow();
		return image;
	}

	/**
	 * Generate an actual image from the computed data.
	 * @param taskList the list of tasks to execute.
	 * @param config the configuration parameters for the Lyupanov algorithm.
	 * @return an <code>Image</code> instance.
	 * @throws Exception if an error is raised during the image generation.
	 */
	public static Image generateLyapunovImage(List<JPPFTask> taskList, FractalConfiguration config) throws Exception
	{
		double min = 0d;
		double max = 0d;

		// compute the min and max lambda
		for (int j=0; j<config.bsize; j++)
		{
			LyapunovTask task = (LyapunovTask) taskList.get(j);
			double[] values = (double[]) task.getResult();
			for (int i=0; i<config.asize; i++)
			{
				if (values[i] > max) max = values[i];
				if (values[i] < min) min = values[i];
			}
		}

		BufferedImage image = new BufferedImage(config.bsize, config.asize, BufferedImage.TYPE_INT_RGB);
		for (int j=0; j<config.bsize; j++)
		{
			LyapunovTask task = (LyapunovTask) taskList.get(j);
			double[] values = (double[]) task.getResult();
			for (int i=0; i<config.asize; i++)
			{
				int rgb = computeLyapunovRGB(values[i], min, max);
				image.setRGB(j, config.asize - i - 1, rgb);
			}
		}
		ImageIO.write(image, "jpeg", new File("data/lyapunov.jpg"));
		return image;
	}

	/**
	 * Generate an actual image from the computed data.
	 * @param taskList the list of tasks to execute.
	 * @param config the cofniguration parameters for the Lyupanov algorithm.
	 * @return an <code>Image</code> instance.
	 * @throws Exception if an error is raised during the image generation.
	 */
	public static Image generateMandelbrotImage(List<JPPFTask> taskList, FractalConfiguration config) throws Exception
	{
		int max = config.nmax;

		BufferedImage image = new BufferedImage(config.asize, config.bsize, BufferedImage.TYPE_INT_RGB);
		for (int j=0; j<config.bsize; j++)
		{
			MandelbrotTask task = (MandelbrotTask) taskList.get(j);
			int[] values = (int[]) task.getResult();
			for (int i=0; i<config.asize; i++)
			{
				int rgb = computeMandelbrotRGB(values[i], max);
				image.setRGB(i, config.bsize - j - 1, rgb);
			}
		}
		try
		{
			ImageIO.write(image, "jpeg", new File("data/mandelbrot.jpg"));
			//writeImage(image, "data/mandelbrot.jpg");
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
		return image;
	}

	/**
	 * Compute the color as an RGB integer value.
	 * @param lambda the lambda value to convert into a color rgb value.
	 * @param min the minimum lambda value found.
	 * @param max the maximum lambda value found.
	 * @return an RGB value represented as an int.
	 */
	private static int computeLyapunovRGB(double lambda, double min, double max)
	{
		double[] rgb_f = new double[3];
		if (lambda > 0)
		{
      rgb_f[0] = 0d;
      rgb_f[1] = 0d;
      rgb_f[2] = lambda/max;
    }
		else
		{
      rgb_f[0] = 1d - Math.pow(lambda/min, 2d/3.0d);
      rgb_f[1] = 1d - Math.pow(lambda/min, 1d/3.0d);
      rgb_f[2] = 0d;
    }
		int result = 0;
		for (int i=0; i<3; i++)
		{
			int n = (int) (rgb_f[i]*255d);
			n = n < 0 ? 0 : (n > 255 ? 255 : n);
			result = 256 * result + n;
		}
    return result;
	}

	/**
	 * Compute the color as an RGB integer value.
	 * @param value the number of escape iterations to convert into a color rgb value.
	 * @param max the maximum lambda value found.
	 * @return an RGB value represented as an int.
	 */
	private static int computeMandelbrotRGB(int value, int max)
	{
		int[] rgb = new int[3];
		if (value >= max)
		{
	    rgb[0] = 0;
	    rgb[1] = 0;
	    rgb[2] = 0;
		}
		else
		{
			long n = (16L * 16L * 16L * value) / max;
	    rgb[1] = 16 * (int) (n % 16);
	    n /= 16;
	    rgb[2] = 16 * (int) (n % 16);
	    n /= 16;
	    rgb[0] = 16 * (int) (n % 16);
		}
		int n = rgb[0];
		n = 256 * n + rgb[1];
		n = 256 * n +rgb[2];
    return n;
	}

	/**
	 * Close the JPPF client.
	 */
	public static void closeJPPFClient()
	{
		if (jppfClient != null) jppfClient.close();
	}

	/**
	 * Creates a window that pops up during the computation.
	 * The window contains a progress bar.
	 */
	public static void createOrDisplayWaitWindow()
	{
		if (window == null)
		{
			Frame frame = null;
			for (Frame f: Frame.getFrames())
			{
				if (f.isVisible()) frame = f;
			}
			JProgressBar progressBar = new JProgressBar();
			progressBar.setIndeterminate(true);
			Font font = progressBar.getFont();
			Font f = new Font(font.getName(), Font.BOLD, 14);
			progressBar.setFont(f);
			progressBar.setString("Calculating, please wait ...");
			progressBar.setStringPainted(true);
			window = new JWindow(frame);
			window.getContentPane().add(progressBar);
			window.getContentPane().setBackground(Color.white);
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				Dimension d = window.getOwner().getSize();
				Point p = window.getOwner().getLocationOnScreen();
				int w = 300;
				int h = 60;
				window.setBounds(p.x+(d.width-w)/2, p.y+(d.height-h)/2, w, h);
				window.setVisible(true);
			}
		});
	}

	/**
	 * Close the wait window and release the resources it uses.
	 */
	public static void hideWaitWindow()
	{
		//if (window.isVisible()) window.dispose();
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				window.setVisible(false);
			}
		});
	}

	/**
	 * Task for submitting the computation from a separate thread.
	 * The goal is to avoid doing the calculations in the AWT event thread.
	 */
	public static class FractalExecution implements Runnable
	{
		/**
		 * Determines the type of fractal to generate.
		 */
		private boolean doMandelbrot = true;
		/**
		 * The algorithm parameters.
		 */
		private FractalConfiguration config = null;
		/**
		 * The generated image;
		 */
		private Image image = null;

		/**
		 * Initializer this task with the specified parameters.
		 * @param doMandelbrot determines the type of fractal to generate.
		 * @param config the algorithm parameters.
		 */
		public FractalExecution(boolean doMandelbrot, FractalConfiguration config)
		{
			this.doMandelbrot = doMandelbrot;
			this.config = config;
		}

		/**
		 * Perform the submission of the computation.
		 * @see java.lang.Runnable#run()
		 */
		public void run()
		{
			try
			{
				image = doPerform(doMandelbrot, config);
			}
			catch(Exception e)
			{
				log.error(e.getMessage(), e);
			}
		}

		/**
		 * Get the generated image;
		 * @return an <code>Image</code> instance.
		 */
		public Image getImage()
		{
			return image;
		}
	}
}
