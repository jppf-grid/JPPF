/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.example.common.AbstractFractalConfiguration;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.node.protocol.Task;
import org.jppf.ui.options.factory.OptionsHandler;
import org.jppf.utils.DateTimeUtils;
import org.jppf.utils.FileUtils;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.StringUtils;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract runner class for the fractals sample application.
 * @author Laurent Cohen
 */
public abstract class AbstractRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractRunner.class);
  /**
   * JPPF client used to submit execution requests.
   */
  protected static JPPFClient jppfClient = null;
  /**
   * Performs the submission of computations to JPPF.
   */
  protected final ExecutorService executor;
  /**
   * A reference to the window displayed while waiting for the end of the computation.
   */
  protected JWindow window = null;
  /**
   * The option holding the image in the UI.
   */
  protected final boolean uiMode;
  /**
   * The panel holding the image in the UI.
   */
  protected ImagePanel imagePanel;
  /**
   * The name associated to this runner, corresponds to the type of fractals.
   */
  protected final String name;
  /**
   * Holds the recorded events.
   */
  protected final List<AbstractFractalConfiguration> records = new ArrayList<>();
  /**
   * Determines whether recording is currently on.
   */
  protected boolean recording = false;
  /**
   * Counting semaphore used to block threads wanting to submit a job, when there are more than a given number.
   */
  protected Semaphore semaphore;
  /**
   * Count of submitted jobs.
   */
  private final AtomicInteger jobCount = new AtomicInteger(0);
  /**
   * Temporary reference to the cursor of the currently focused component.
   */
  private Cursor cursor;
  /**
   * Temporary reference to the currently focused component.
   */
  private Component focusOwner;

  /**
   * Initialize this runner.
   * @param name the name associated to this runner, corresponds to the type of fractals.
   */
  public AbstractRunner(final String name) {
    this(name, 1, false);
  }

  /**
   * Initialize this runner.
   * @param name the name associated to this runner, corresponds to the type of fractals.
   * @param jobCapacity the maximum number of JPPF jobs that can be submitted at any given time.
   */
  public AbstractRunner(final String name, final int jobCapacity) {
    this(name, jobCapacity, false);
  }

  /**
   * Initialize this runner.
   * @param name the name associated to this runner, corresponds to the type of fractals.
   * @param uiMode whether this runner should update the gui while calculating, or display a progress bar, etc.
   */
  public AbstractRunner(final String name, final boolean uiMode) {
    this(name, 1, uiMode);
  }

  /**
   * Initialize this runner.
   * @param name the name associated to this runner, corresponds to the type of fractals.
   * @param jobCapacity the maximum number of JPPF jobs that can be submitted at any given time.
   * @param uiMode the option holding the image in the UI.
   */
  public AbstractRunner(final String name, final int jobCapacity, final boolean uiMode) {
    this.name = name;
    semaphore = new Semaphore(jobCapacity);
    executor = new ThreadPoolExecutor(jobCapacity, jobCapacity, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new JPPFThreadFactory(name + "-runner")) {
      @Override
      protected void afterExecute(final Runnable r, final Throwable t) {
        super.afterExecute(r, t);
        semaphore.release();
      }
    };
    if (jppfClient == null) jppfClient = new JPPFClient();
    this.uiMode = uiMode;
  }

  /**
   * Submit a fractal image computation.
   * @param config holds the fractal algorithm parameters required for the computation.
   * @return a future which returns a {@link BufferedImage} as the computation result.
   * @throws Exception if an error is raised during the execution.
   */
  public Future<GeneratedImage> submitExecution(final AbstractFractalConfiguration config) throws Exception {
    return submitExecution(0, config, 0L);
  }

  /**
   * Submit a fractal image computation.
   * @param id the image identifier.
   * @param config holds the fractal algorithm parameters required for the computation.
   * @return a future which returns a {@link BufferedImage} as the computation result.
   * @throws Exception if an error is raised during the execution.
   */
  public Future<GeneratedImage> submitExecution(final int id, final AbstractFractalConfiguration config) throws Exception {
    return submitExecution(id, config, 0L);
  }

  /**
   * Submit a fractal image computation.
   * @param id the image identifier.
   * @param config holds the fractal algorithm parameters required for the computation.
   * @param wait how long to wait in ms after the image is updated. This is used by the {@link #replay()} method.
   * @return a future which returns a {@link BufferedImage} as the computation result.
   * @throws Exception if an error is raised during the execution.
   */
  public Future<GeneratedImage> submitExecution(final int id, final AbstractFractalConfiguration config, final long wait) throws Exception {
    semaphore.acquire();
    if (recording) addRecord(config);
    final FractalExecution exec = new FractalExecution(id, config, wait);
    return executor.submit(exec);
  }

  /**
   * Execute for the specified number of iterations.
   * @param id the image identifier, used as job priority.
   * @param cfg holds the fractal algorithm parameters required for the computation.
   * @return a generated image that can be displayed in a UI or saved as a file.
   * @throws Exception if an error is raised during the execution.
   */
  public BufferedImage computeFractal(final int id, final AbstractFractalConfiguration cfg) throws Exception {
    final JPPFJob job = new JPPFJob();
    job.getSLA().setPriority(id);
    job.setName(name + " fractal " + jobCount.incrementAndGet());
    final DataProvider dp = new MemoryMapDataProvider();
    dp.setParameter("config", cfg);
    job.setDataProvider(dp);
    final long start = System.nanoTime();
    final List<Task<?>> results = submitJob(job, cfg);
    final long elapsed = DateTimeUtils.elapsedFrom(start);
    if (log.isDebugEnabled()) log.debug("Computation performed in " + StringUtils.toStringDuration(elapsed));
    final BufferedImage image = generateImage(results, cfg);
    if (JPPFConfiguration.getProperties().getBoolean("jppf.fractals.autosave.enabled", true)) saveImage(image, "png", "data/" + name + ".png");
    return image;
  }

  /**
   * Submit a JPPF job to compute the fractal image.
   * @param job the job to submit for execution.
   * @param cfg holds the fractal algorithm parameters required for the computation.
   * @return a list of {@link Task}s.
   * @throws Exception if an error is raised during the execution.
   */
  protected abstract List<Task<?>> submitJob(final JPPFJob job, final AbstractFractalConfiguration cfg) throws Exception;

  /**
   * Generate an actual image from the job results.
   * @param taskList the job results as a list of executed tasks.
   * @param config the configuration parameters for the fractal algorithm.
   * @return a {@link BufferedImage}> instance.
   * @throws Exception if an error is raised during the image generation.
   */
  protected abstract BufferedImage generateImage(final List<Task<?>> taskList, final AbstractFractalConfiguration config) throws Exception;

  /**
   * Save the specified image to a file.
   * @param image the image to store.
   * @param format the format, eg "jpeg", "png", etc.
   * @param filename the path to the file to save to.
   */
  private static void saveImage(final BufferedImage image, final String format, final String filename) {
    try {
      ImageIO.write(image, format, new File(filename));
    } catch(final Exception e) {
      log.error(e.getMessage(), e);
    }
  }

  /**
   * Close the JPPF client and release other resources.
   */
  public void dispose() {
    closeJPPFClient();
    if (executor != null) executor.shutdownNow();
  }

  /**
   * Close the JPPF client.
   */
  public static synchronized void closeJPPFClient() {
    if (jppfClient != null) {
      jppfClient.close();
      jppfClient = null;
    }
  }

  /**
   * Close the JPPF client.
   * @return a {@link JPPFClient} instance.
   */
  public static synchronized JPPFClient getJPPFClient() {
    return jppfClient;
  }

  /**
   * Creates a window that pops up during the computation.
   * The window contains a progress bar.
   */
  public void createOrDisplayWaitWindow() {
    final JFrame frame = (JFrame) OptionsHandler.getMainWindow();
    if (window == null) {
      final Runnable r = new Runnable() {
        @Override
        public void run() {
          final JProgressBar progressBar = new JProgressBar();
          progressBar.setIndeterminate(true);
          final Font font = progressBar.getFont();
          final Font f = new Font(font.getName(), Font.BOLD, 14);
          progressBar.setFont(f);
          progressBar.setString("Calculating, please wait ...");
          progressBar.setStringPainted(true);
          window = new JWindow(frame);
          window.getContentPane().add(progressBar);
          window.getContentPane().setBackground(Color.white);
        }
      };
      SwingUtilities.invokeLater(r);
    }
    final Runnable r2 = new Runnable() {
      @Override
      public void run() {
        focusOwner = frame.getFocusOwner();
        if (focusOwner != null) {
          cursor = focusOwner.getCursor();
          focusOwner.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        }
        final Dimension d = frame.getSize();
        final Point p = frame.getLocationOnScreen();
        final int w = 300;
        final int h = 60;
        window.setBounds(p.x+(d.width-w)/2, p.y+(d.height-h)/2, w, h);
        window.setVisible(true);
        window.toFront();
        window.repaint();
      }
    };
    SwingUtilities.invokeLater(r2);
  }

  /**
   * Close the wait window and release the resources it uses.
   */
  public void hideWaitWindow() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        if (window != null) {
          window.setVisible(false);
          if (focusOwner != null) {
            focusOwner.setCursor(cursor);
            cursor = null;
            focusOwner = null;
          }
        }
      }
    });
  }

  /**
   * Task for submitting the computation from a separate thread.
   * The goal is to avoid doing the calculations in the AWT event thread.
   */
  public class FractalExecution implements Callable<GeneratedImage> {
    /**
     * The algorithm parameters.
     */
    private final AbstractFractalConfiguration config;
    /**
     * How long to wait in ms after the image is updated.
     */
    private final long wait;
    /**
     * Image identifier.
     */
    private final int id;

    /**
     * Initialize this task with the specified parameters.
     * @param id the image identifier.
     * @param config the algorithm parameters.
     * @param wait how long to wait in ms after the image is updated.
     */
    public FractalExecution(final int id, final AbstractFractalConfiguration config, final long wait) {
      this.config = config;
      this.wait = wait;
      this.id = id;
    }

    @Override
    public GeneratedImage call() throws Exception {
      try {
        BufferedImage image = null;
        if (uiMode) createOrDisplayWaitWindow();
        try {
          final long start = System.nanoTime();
          image = computeFractal(id, config);
          final long elapsed = DateTimeUtils.elapsedFrom(start);
          if (wait > 0L) {
            Thread.sleep(Math.max(1L, wait - elapsed));
            final ImagePanel panel = getImagePanel();
            final BufferedImage img = image;
            if (panel != null) {
              log.info("updating image id=" + id);
              SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                  panel.setImage(img);
                  panel.repaint();
                }
              });
            }
          }
        } catch(final Exception e) {
          log.error(e.getMessage(), e);
        } finally {
          if (uiMode) hideWaitWindow();
        }
        return new GeneratedImage(id, image);
      } finally {
        semaphore.release();
      }
    }
  }

  /**
   * Get the events that have been recorded so far.
   * @return a list of <code>AbstractFractalConfiguration</code> objects.
   */
  public List<AbstractFractalConfiguration> getRecords() {
    return records;
  }

  /**
   * Add an event to the recorded events.
   * @param config a <code>AbstractFractalConfiguration</code>.
   */
  public void addRecord(final AbstractFractalConfiguration config) {
    records.add(config);
  }

  /**
   * Clear all recorded events.
   */
  public void clearRecords() {
    records.clear();
  }

  /**
   * Determine whether recording is currently on.
   * @return <code>true</code> if recording is on, <code>false</code> otherwise.
   */
  public boolean isRecording() {
    return recording;
  }

  /**
   * Specify whether recording is currently on.
   * @param recording <code>true</code> to set recording on, <code>false</code> otherwise.
   */
  public void setRecording(final boolean recording) {
    this.recording = recording;
  }

  /**
   * Replay all recorded events.
   */
  public void replay() {
    final boolean temp = recording;
    final Semaphore oldSemaphore = semaphore;
    try {
      recording = false;
      semaphore = new Semaphore(records.size());
      for (AbstractFractalConfiguration config: records) {
        try {
          submitExecution(0, config, 2000L);
        } catch (final Exception e) {
          e.printStackTrace();
        }
      }
    } finally {
      recording = temp;
      semaphore = oldSemaphore;
    }
  }

  /**
   * Save all records to file.
   * @param filename the name of the file.
   */
  public void saveRecords(final String filename) {
    try {
      final StringBuilder sb = new StringBuilder();
      int count = 0;
      for (AbstractFractalConfiguration config: records) {
        if (count > 0) sb.append('\n');
        sb.append(config.toCSV());
        count++;
      }
      FileUtils.writeTextFile(filename, sb.toString());
    } catch (final Exception e) {
      e.printStackTrace();

    }
  }

  /**
   * Open and load a records file.
   * @param filename the name of the file.
   */
  public abstract void loadRecords(final String filename);

  /**
   * Get the panel holding the image in the UI.
   * @return an {@link ImagePanel} instance.
   */
  public ImagePanel getImagePanel() {
    return imagePanel;
  }

  /**
   * Set the panel holding the image in the UI.
   * @param imagePanel an {@link ImagePanel} instance.
   */
  public void setImagePanel(final ImagePanel imagePanel) {
    this.imagePanel = imagePanel;
  }
}
