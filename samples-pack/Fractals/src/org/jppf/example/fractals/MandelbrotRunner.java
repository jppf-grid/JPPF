/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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
package org.jppf.example.fractals;

import java.awt.image.BufferedImage;
import java.io.FileReader;
import java.util.List;

import org.jppf.client.JPPFJob;
import org.jppf.example.common.AbstractFractalConfiguration;
import org.jppf.example.fractals.mandelbrot.*;
import org.jppf.location.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.FileUtils;
import org.slf4j.*;

/**
 * Runner class for the Mandelbrot fractals sample application.
 * @author Laurent Cohen
 */
public class MandelbrotRunner extends AbstractRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(MandelbrotRunner.class);
  /**
   * The android demo apk in memory, so it can be sent with a job to run on an Android node.
   */
  private static final Location androidApk = initApk();

  /**
   * Initialize this runner.
   */
  public MandelbrotRunner() {
    super("mandelbrot");
  }

  /**
   * Initialize this runner.
   * @param option the option holding the image in the UI.
   */
  public MandelbrotRunner(final boolean option) {
    super("mandelbrot", option);
  }

  /**
   * Initialize this runner.
   * @param jobCapacity the maximum number of JPPF jobs that can be submitted at any given time.
   */
  public MandelbrotRunner(final int jobCapacity) {
    super("mandelbrot", jobCapacity);
  }

  @Override
  protected List<Task<?>> submitJob(final JPPFJob job, final AbstractFractalConfiguration cfg) throws Exception {
    int nbTask = cfg.height;
    log.info("Executing " + nbTask + " tasks");
    job.getSLA().getClassPath().add("android-fractal-demo.apk", androidApk);
    job.getMetadata().setParameter("jppf.node.integration.class", "org.jppf.android.demo.FractalEventHandler");
    for (int i=0; i<nbTask; i++) job.add(new MandelbrotTask(i));
    return jppfClient.submitJob(job);
  }

  @Override
  protected BufferedImage generateImage(final List<Task<?>> taskList, final AbstractFractalConfiguration cfg) throws Exception {
    MandelbrotConfiguration config = (MandelbrotConfiguration) cfg;
    int max = config.maxIterations;
    BufferedImage image = new BufferedImage(config.width, config.height, BufferedImage.TYPE_INT_RGB);
    for (int j=0; j<config.height; j++) {
      MandelbrotTask task = (MandelbrotTask) taskList.get(j);
      int[] values = task.getResult();
      int[] colors = task.getColors();
      for (int i=0; i<config.width; i++) image.setRGB(i, config.height - j - 1, colors[i]);
    }
    return image;
  }

  @Override
  public void loadRecords(final String filename) {
    try {
      records.clear();
      List<String> list = FileUtils.textFileAsLines(new FileReader(filename));
      for (String csv: list) records.add(new MandelbrotConfiguration(csv));
    } catch (Exception e) {
    }
  }

  /**
   * Load the android demo apk in memory, so it can be sent with a job to run on an Android node.
   * @return the apk as a {@link Location} object transportable in a job.
   */
  private static Location initApk() {
    try {
      Location file = new FileLocation("data/android-fractal-demo.apk");
      return file.copyTo(new MemoryLocation((int) file.size()));
    } catch(Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }
}
