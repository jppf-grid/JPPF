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
package org.jppf.example.fractals.lyapunov;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import org.jppf.client.JPPFJob;
import org.jppf.example.fractals.*;
import org.jppf.node.protocol.Task;
import org.slf4j.*;

/**
 * Runner class for the Lyapunov and Mandelbrot fractals sample application.
 * @author Laurent Cohen
 */
public class LyapunovRunner extends AbstractRunner
{
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(LyapunovRunner.class);

  /**
   * This constructor is to be used when not embedded in a GUI.
   */
  public LyapunovRunner() {
    super("lyapunov");
  }

  /**
   * Initialize this runner.
   * @param option the option holding the image in the UI.
   */
  public LyapunovRunner(final boolean option) {
    super("lyapunov", option);
  }

  @Override
  protected synchronized List<Task<?>> submitJob(final JPPFJob job, final AbstractFractalConfiguration cfg) throws Exception
  {
    LyapunovConfiguration config = (LyapunovConfiguration) cfg;
    int nbTask = config.width;
    log.info("Executing " + nbTask + " tasks");
    for (int i=0; i<nbTask; i++) job.add(new LyapunovTask(i));
    List<Task<?>> results = jppfClient.submitJob(job);
    return results;
  }

  @Override
  public BufferedImage generateImage(final List<Task<?>> taskList, final AbstractFractalConfiguration cfg) throws Exception
  {
    LyapunovConfiguration config = (LyapunovConfiguration) cfg;
    double min = 0d;
    double max = 0d;
    // compute the min and max lambda
    for (int j=0; j<config.width; j++)
    {
      LyapunovTask task = (LyapunovTask) taskList.get(j);
      double[] values = (double[]) task.getResult();
      for (int i=0; i<config.height; i++)
      {
        if (values[i] > max) max = values[i];
        if (values[i] < min) min = values[i];
      }
    }

    BufferedImage image = new BufferedImage(config.width, config.height, BufferedImage.TYPE_INT_RGB);
    for (int j=0; j<config.width; j++)
    {
      LyapunovTask task = (LyapunovTask) taskList.get(j);
      double[] values = (double[]) task.getResult();
      for (int i=0; i<config.height; i++)
      {
        int rgb = computeLyapunovRGB(values[i], min, max);
        image.setRGB(j, config.height - i - 1, rgb);
      }
    }
    ImageIO.write(image, "jpeg", new File("data/lyapunov.jpg"));
    return image;
  }

  /**
   * Compute the color as an RGB integer value.
   * @param lambda the lambda value to convert into a color rgb value.
   * @param min the minimum lambda value found.
   * @param max the maximum lambda value found.
   * @return an RGB value represented as an int.
   */
  private int computeLyapunovRGB(final double lambda, final double min, final double max)
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
      rgb_f[0] = 1d - Math.pow(lambda/min, 2d/3d);
      rgb_f[1] = 1d - Math.pow(lambda/min, 1d/3d);
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

  @Override
  public void loadRecords(final String filename)
  {
  }
}
