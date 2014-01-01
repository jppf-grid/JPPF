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

package org.jppf.example.fractals.moviegenerator;

import static org.monte.media.FormatKeys.*;
import static org.monte.media.VideoFormatKeys.*;

import java.awt.image.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.*;

import javax.imageio.ImageIO;

import org.jppf.client.JPPFJob;
import org.jppf.example.fractals.*;
import org.jppf.example.fractals.mandelbrot.*;
import org.jppf.node.protocol.Task;
import org.jppf.scheduling.JPPFSchedule;
import org.jppf.utils.*;
import org.monte.media.*;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

/**
 * This class generates AVI movies based on the code of the Mandelbrot fractal sample,
 * and on recordings produced by the sample. The movie is generated frame by frame.
 * The frames are computed as a number of transition images between each parameters set in the recording and the next.
 * @author Laurent Cohen
 */
public class MovieGenerator {
  /**
   * Writes the AVI file(s).
   */
  private AVIWriter out = null;
  /**
   * Frame rate in frames/s.
   */
  private final int frameRate;
  /**
   * Duration of each transition in seconds.
   */
  private final int transitionTime;
  /**
   * The runner which submits fractal computation jobs to the JPPF grid.
   */
  private final AbstractRunner runner;
  /**
   * Performs the submission of computations to JPPF.
   */
  protected final ExecutorService executor;

  /**
   * Process the command-line parameters and generate a movie accordingly.
   * @param args the command-line argument to use.
   */
  public static void main(final String[] args) {
    MovieGenerator generator = null;
    try {
      Map<String, Object> map = new CLIHandler().processArguments(args);
      String inputFile = (String) map.get("-i");
      String outputFile = (String) map.get("-o");
      if (!outputFile.toLowerCase().endsWith(".avi")) outputFile = outputFile + ".avi";
      int frameRate = (Integer) map.get("-f");
      int transitionTime = (Integer) map.get("-t");
      generator = new MovieGenerator(inputFile, outputFile, frameRate, transitionTime);
      generator.generate();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (generator != null) generator.dispose();
    }
  }

  /**
   * Initialize this movie genrator with the specified parameters.
   * @param inputFile path to the csv file used as input.
   * @param outputFile path to the mivie file to generate.
   * @param frameRate number of frames per second.
   * @param transitionTime the transition time in second from one input record to the next.
   * @throws Exception if any error occurs.
   */
  public MovieGenerator(final String inputFile, final String outputFile, final int frameRate, final int transitionTime) throws Exception {
    this.frameRate = frameRate;
    this.transitionTime = transitionTime;
    int maxJobs = JPPFConfiguration.getProperties().getInt("jppf.fractals.concurrent.jobs", 1);
    executor = Executors.newSingleThreadExecutor(new JPPFThreadFactory("MovieGenerator"));
    runner = new MandelbrotRunner(maxJobs) {
      @Override
      protected List<Task<?>> submitJob(final JPPFJob job, final AbstractFractalConfiguration cfg) throws Exception {
        // override SLA settings to ensure a job won't get stuck if a node fails
        TypedProperties config = JPPFConfiguration.getProperties();
        int maxExpirations = config.getInt("jppf.fractals.dispatch.max.timeouts", 1);
        long timeout = config.getLong("jppf.fractals.dispatch.timeout", 15000L);
        job.getSLA().setMaxDispatchExpirations(maxExpirations);
        job.getSLA().setDispatchExpirationSchedule(new JPPFSchedule(timeout));
        return super.submitJob(job, cfg);
      }
    };
    runner.loadRecords(inputFile);
    if (runner.getRecords().size() < 2) throw new IllegalStateException("There must be at least 2 records to perform at least one transition");
    // many players and transcoders do not recognize the AVI-PNG format, so we use AVI-JPEG instead
    initAviStream(new File(outputFile), new Format(EncodingKey, ENCODING_AVI_MJPG, DepthKey, 24, QualityKey, 1f), runner.getRecords().get(0));
    //initStream(new File(outputFile), new Format(EncodingKey, ENCODING_AVI_PNG, DepthKey, 24), records.get(0)); // PNG is lossless
  }

  /**
   * Generate the movie.
   * @throws Exception if any error occurs.
   */
  public void generate() throws Exception {
    // -1 because we add the second parameter set as last transition step
    int nbFrames = frameRate * transitionTime - 1;
    List<AbstractFractalConfiguration> records = runner.getRecords();
    DecimalFormat nf = new DecimalFormat("00000");
    // for each record, define the transition between it and the next record as nbFrames images
    for (int i=0; i<records.size()-1; i++) {
      System.out.println("computing " + (nbFrames+1) + " frames for transition #" + nf.format(i+1) + " ...");
      MandelbrotConfiguration cfgFirst = (MandelbrotConfiguration) records.get(i);
      MandelbrotConfiguration cfgLast = (MandelbrotConfiguration) records.get(i+1);
      System.out.println("cfgFirst=" + cfgFirst + ", cfgLast=" + cfgLast);
      AbstractStepVector sv = new NonLinearStepVector(cfgFirst, cfgLast, nbFrames);
      System.out.println("sv=" + sv);
      Queue<Future<Future<GeneratedImage>>> futures = new LinkedBlockingQueue<>();
      for (int j=0; j<nbFrames; j++) {
        MandelbrotConfiguration cfg = new MandelbrotConfiguration(cfgFirst.x  + sv.getX(j), cfgFirst.y + sv.getY(j),
            cfgFirst.d + sv.getD(j), cfgFirst.maxIterations + (int) Math.round(sv.getN(j)));
        cfg.width = cfgFirst.width;
        cfg.height = cfgFirst.height;
        futures.add(executor.submit(new SubmissionTask(nbFrames - j, cfg)));
      }
      futures.add(executor.submit(new SubmissionTask(0, cfgLast)));
      int j = 0;
      Future<Future<GeneratedImage>> future;
      while ((future = futures.poll()) != null) {
        Future<GeneratedImage> f = future.get();
        BufferedImage image = f.get().getImage();
        if (j == 0) ImageIO.write(image, "png", new File("data/frame-0.png")); // for debug
        writeImageToStream(image);
        System.out.println("  transition #" + nf.format(i+1) + " frame #" + nf.format(j+1) + " done");
        j++;
      }
    }
  }

  /**
   * Initialize the AVI writer.
   * @param file the file to write to.
   * @param format the format to configure the writer with.
   * @param cfg provides frame width and height information.
   * @throws IOException if any error occurs.
   */
  private void initAviStream(final File file, final Format format, final AbstractFractalConfiguration cfg) throws IOException {
    // Make the format more specific
    Format fmt  = format.prepend(MediaTypeKey, MediaType.VIDEO, FrameRateKey, new Rational(frameRate, 1), WidthKey, cfg.width, HeightKey, cfg.height);
    // Create the writer
    out = new AVIWriter(file);
    // Add a track to the writer
    out.addTrack(fmt);
    ColorModel cm = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB).getColorModel();
    out.setPalette(0, cm);
  }

  /**
   * Write an image to the AVI stream.
   * @param image the image to write.
   * @throws IOException if any error occurs.
   */
  private void writeImageToStream(final BufferedImage image) throws IOException {
    out.write(0, image, 1);
  }

  /**
   * Close the AVI stream and other resources.
   */
  public void dispose() {
    try {
      if (out != null) out.close();
      if (runner != null) runner.dispose();
      if (executor != null) executor.shutdownNow();
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 
   */
  private class SubmissionTask implements Callable<Future<GeneratedImage>> {
    /**
     * 
     */
    private final AbstractFractalConfiguration cfg;
    /**
     * The image identifier, used as job priority.
     */
    private final int id;

    /**
     * 
     * @param id the image identifier, used as job priority.
     * @param cfg the configuration of the fractal image to compute.
     */
    public SubmissionTask(final int id, final AbstractFractalConfiguration cfg) {
      this.cfg = cfg;
      this.id = id;
    }

    @Override
    public Future<GeneratedImage> call() throws Exception {
      return runner.submitExecution(id, cfg);
    }
  }
}
