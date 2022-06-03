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

package org.jppf.example.jaligner;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.DataProvider;
import org.jppf.node.protocol.MemoryMapDataProvider;
import org.jppf.node.protocol.Task;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.options.Option;
import org.jppf.utils.DateTimeUtils;
import org.jppf.utils.FileUtils;
import org.jppf.utils.StringUtils;
import org.jppf.utils.concurrent.JPPFThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jaligner.Sequence;
import jaligner.matrix.MatrixLoader;
import jaligner.util.SequenceParser;


/**
 * Example of a searching a sequence in a database, that has the highest alignment score
 * with a sequence given as input.
 * @author Laurent Cohen
 */
public class SequenceAlignmentRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SequenceAlignmentRunner.class);
  /**
   * The JPPF client.
   */
  private static JPPFClient client = new JPPFClient();
  /**
   * Performs the submission of computations to JPPF.
   */
  private static ExecutorService executor = Executors.newFixedThreadPool(1, new JPPFThreadFactory("SequenceAlignmentRunner"));
  /**
   * A reference to the window displayed while waiting for the end of the computation.
   */
  private static JWindow window = null;
  /**
   * The progress bar displayed in the wait window.
   */
  private static JProgressBar progressBar = null;
  /**
   * Reference to the UI page.
   */
  private static Option option = null;
  /**
   * Jobs sequence number.
   */
  private static final AtomicInteger jobSequence = new AtomicInteger(0);

  /**
   * Run the sample.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      System.out.println("Running example...");
      final String s = FileUtils.readTextFile("data/TargetSequence.txt");
      doPerform(s, "PAM120", "data/ecoli.aa");
      System.exit(0);
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Run the sample.
   * @param targetSequence the sequence to compare those in the database with.
   * @param matrix the name of the substitution matrix to use in the alignments.
   * @param dbPath the path to the database of sequences.
   * @param option an option used as an entry point to the UI.
   * @throws Exception if the computation failed.
   */
  public static void perform(final String targetSequence, final String matrix, final String dbPath, final Option option) throws Exception {
    log.info("performing computation with targetSequence={}, matrix={}, dbPath={}, option={}", targetSequence, matrix, dbPath, option);
    SequenceAlignmentRunner.option = option;
    createOrDisplayWaitWindow();
    final AlignmentExecution exec = new AlignmentExecution(targetSequence, matrix, dbPath);
    executor.execute(exec);
  }

  /**
   * Run the sample.
   * @param targetSequence the sequence to compare those in the database with.
   * @param matrix the name of the substitution matrix to use in the alignments.
   * @param dbPath the path to the database of sequences.
   * @return the task with the maximum score.
   * @throws Exception if the computation failed.
   */
  public static SequenceAlignmentTask doPerform(final String targetSequence, final String matrix, final String dbPath) throws Exception {
    final long start = System.nanoTime();
    final Sequence target = SequenceParser.parse(targetSequence);
    final DataProvider dp = new MemoryMapDataProvider();
    dp.setParameter(SequenceAlignmentTask.TARGET_SEQUENCE, target);
    dp.setParameter(SequenceAlignmentTask.SCORING_MATRIX, MatrixLoader.load(matrix));
    final JPPFJob job = new JPPFJob();
    job.setName("Sequence alignment " + jobSequence.incrementAndGet());
    job.setDataProvider(dp);
    System.out.println("Indexing sequence database...");
    final String idx = dbPath+".idx";
    final int nb = DatabaseHandler.generateIndex(dbPath, idx, null);
    System.out.println("" + nb + " sequences indexed");
    int n = 0;
    final DatabaseHandler dh = new DatabaseHandler(dbPath, idx, null);
    boolean end = false;
    while (!end) {
      final String s = dh.nextSequence();
      if (s == null) end = true;
      else job.add(new SequenceAlignmentTask(s, ++n));
    }
    final long start2 = System.nanoTime();
    final AlignmentJobListener listener = new AlignmentJobListener(job.getJobTasks().size());
    job.addJobListener(listener);
    final List<Task<?>> results = client.submit(job);
    final long elapsed2 = DateTimeUtils.elapsedFrom(start2);
    float maxScore = 0;
    SequenceAlignmentTask maxTask = null;
    for (final Task<?> t: results) {
      final SequenceAlignmentTask task = (SequenceAlignmentTask) t;
      if (task.getThrowable() != null) {
        final String msg = "Exception in task #"+task.getNumber()+ ", sequence:\n"+task.getSequence();
        log.info(msg, task.getThrowable());
      }
      final float score = task.getResult();
      if (score > maxScore) {
        maxScore = score;
        maxTask = task;
      }
    }
    final long elapsed = DateTimeUtils.elapsedFrom(start);
    log.info("max score is "+maxScore+" for sequence #"+maxTask.getNumber()+" :\n" + maxTask.getSequence());
    log.info("Total time = " + StringUtils.toStringDuration(elapsed) + ", calculation time = " + StringUtils.toStringDuration(elapsed2));
    hideWaitWindow();
    return maxTask;
  }

  /**
   * Creates a window that pops up during the computation.
   * The window contains a progress bar.
   */
  public static void createOrDisplayWaitWindow() {
    if (window == null) {
      Frame frame = null;
      for (final Frame f: Frame.getFrames()) {
        if (f.isVisible()) frame = f;
      }
      progressBar = new JProgressBar();
      final Font font = progressBar.getFont();
      final Font f = new Font(font.getName(), Font.BOLD, 14);
      progressBar.setFont(f);
      progressBar.setString("Calculating, please wait ...");
      progressBar.setStringPainted(true);
      window = new JWindow(frame);
      window.getContentPane().add(progressBar);
      window.getContentPane().setBackground(Color.white);
    }
    SwingUtilities.invokeLater(new Runnable()     {
      @Override
      public void run() {
        final Dimension d = window.getOwner().getSize();
        final Point p = window.getOwner().getLocationOnScreen();
        final int w = 300;
        final int h = 60;
        window.setBounds(p.x+(d.width-w)/2, p.y+(d.height-h)/2, w, h);
        progressBar.setValue(0);
        window.setVisible(true);
      }
    });
  }

  /**
   * Close the wait window and release the resources it uses.
   */
  public static void hideWaitWindow() {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        window.setVisible(false);
      }
    });
  }

  /**
   * Update the progress value of the progress bar.
   * @param n the new value to set.
   */
  public static void updateProgress(final int n) {
    if (progressBar != null) progressBar.setValue(n);
  }

  /**
   * Task for submitting the computation from a separate thread.
   * The goal is to avoid doing the calculations in the AWT event thread.
   */
  public static class AlignmentExecution implements Runnable {
    /**
     * the sequence to compare those in the database with.
     */
    private final String targetSequence;
    /**
     * the name of the substitution matrix to use in the alignments.
     */
    private final String matrix;
    /**
     * the path to the database of sequences.
     */
    private final String dbPath;
    /**
     * The task that produced the maximum score.
     */
    private SequenceAlignmentTask task = null;

    /**
     * Initialize this task with the specified parameters.
     * @param targetSequence the sequence to compare those in the database with.
     * @param matrix the name of the substitution matrix to use in the alignments.
     * @param dbPath the path to the database of sequences.
     */
    public AlignmentExecution(final String targetSequence, final String matrix, final String dbPath) {
      this.targetSequence = targetSequence;
      this.matrix = matrix;
      this.dbPath = dbPath;
    }

    /**
     * Perform the submission of the computation.
     */
    @Override
    public void run() {
      try {
        task = doPerform(targetSequence, matrix, dbPath);
        if (task != null) {
          ((AbstractOption) option.findFirstWithName("/resultSequenceText")).setValue(task.getSequence());
          ((AbstractOption) option.findFirstWithName("/score")).setValue(task.getResult());
        }
      } catch(Exception|Error e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
