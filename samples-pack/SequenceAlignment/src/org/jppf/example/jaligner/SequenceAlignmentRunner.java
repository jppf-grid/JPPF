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

package org.jppf.example.jaligner;

import jaligner.Sequence;
import jaligner.matrix.MatrixLoader;
import jaligner.util.SequenceParser;

import java.awt.*;
import java.io.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.task.storage.DataProvider;
import org.jppf.task.storage.MemoryMapDataProvider;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.options.Option;
import org.jppf.utils.FileUtils;
import org.jppf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Example of a searching a sequence in a database, that has the highest alignment score
 * with a sequence given as input.
 * @author Laurent Cohen
 */
public class SequenceAlignmentRunner
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SequenceAlignmentRunner.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The JPPF client.
   */
  private static JPPFClient client = new JPPFClient();

  /**
   * Performs the submission of computations to JPPF.
   */
  private static ExecutorService executor = Executors.newFixedThreadPool(1);
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
   * Run the sample.
   * @param args not used.
   */
  public static void main(final String[] args)
  {
    try
    {
      long start = System.currentTimeMillis();
      System.out.println("Running example...");
      String s = FileUtils.readTextFile("data/TargetSequence.txt");
      doPerform(s, "PAM120", "data/ecoli.aa");
      System.exit(0);
    }
    catch (Exception e)
    {
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
  public static void perform(final String targetSequence, final String matrix, final String dbPath, final Option option) throws Exception
  {
    SequenceAlignmentRunner.option = option;
    createOrDisplayWaitWindow();
    AlignmentExecution exec = new AlignmentExecution(targetSequence, matrix, dbPath);
    executor.submit(exec);
  }

  /**
   * Run the sample.
   * @param targetSequence the sequence to compare those in the database with.
   * @param matrix the name of the substitution matrix to use in the alignments.
   * @param dbPath the path to the database of sequences.
   * @return the task with the maximum score.
   * @throws Exception if the computation failed.
   */
  public static SequenceAlignmentTask doPerform(final String targetSequence, final String matrix, final String dbPath) throws Exception
  {
    long start = System.currentTimeMillis();
    //System.out.println("Target sequence:\n" + targetSequence);
    Sequence target = SequenceParser.parse(targetSequence);
    DataProvider dp = new MemoryMapDataProvider();
    dp.setParameter(SequenceAlignmentTask.TARGET_SEQUENCE, target);
    dp.setParameter(SequenceAlignmentTask.SCORING_MATRIX, MatrixLoader.load(matrix));
    JPPFJob job = new JPPFJob();
    job.setDataProvider(dp);
    System.out.println("Indexing sequence database...");
    String idx = dbPath+".idx";
    int nb = DatabaseHandler.generateIndex(dbPath, idx, null);
    System.out.println(""+nb+" sequences indexed");
    int n = 0;
    DatabaseHandler dh = new DatabaseHandler(dbPath, idx, null);
    boolean end = false;
    while (!end)
    {
      String s = dh.nextSequence();
      if (s == null) end = true;
      else job.add(new SequenceAlignmentTask(s, ++n));
    }
    long start2 = System.currentTimeMillis();
    //taskList = client.submit(taskList, dp);
    AlignmentResultCollector collector = new AlignmentResultCollector(job.getJobTasks().size());
    job.setBlocking(false);
    job.addJobListener(collector);
    client.submitJob(job);
    List<Task<?>> results = collector.awaitResults();
    long elapsed2 = System.currentTimeMillis() - start2;
    float maxScore = 0;
    SequenceAlignmentTask maxTask = null;
    for (Task<?> t: results)
    {
      SequenceAlignmentTask task = (SequenceAlignmentTask) t;
      if (task.getThrowable() != null)
      {
        String msg = "Exception in task #"+task.getNumber()+ ", sequence:\n"+task.getSequence();
        log.info(msg, task.getThrowable());
      }
      float score = (Float) task.getResult();
      if (score > maxScore)
      {
        maxScore = score;
        maxTask = task;
      }
    }
    long elapsed = System.currentTimeMillis() - start;
    log.info("max score is "+maxScore+" for sequence #"+maxTask.getNumber()+" :\n" + maxTask.getSequence());
    log.info("Total time = " + StringUtils.toStringDuration(elapsed) +
        ", calculation time = " + StringUtils.toStringDuration(elapsed2));
    hideWaitWindow();
    return maxTask;
  }

  /**
   * Load a sequence from the specified file path.
   * @param path location of the sequence.
   * @return sequence the sequence read form the file.
   * @throws IOException if an error occurs when reading the file.
   */
  private static String loadSampleSequence(final String path) throws IOException
  {
    InputStream is = null;
    StringBuilder buffer = new StringBuilder();
    try
    {
      is = SequenceAlignmentRunner.class.getClassLoader().getResourceAsStream(path);
      if (is == null) is = new BufferedInputStream(new FileInputStream(path));

      int ch;
      while ((ch = is.read()) != -1)
      {
        buffer.append((char) ch);
      }
    }
    finally
    {
      if (is != null) is.close();
    }
    return buffer.toString();
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
      progressBar = new JProgressBar();
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
      @Override
      public void run()
      {
        Dimension d = window.getOwner().getSize();
        Point p = window.getOwner().getLocationOnScreen();
        int w = 300;
        int h = 60;
        window.setBounds(p.x+(d.width-w)/2, p.y+(d.height-h)/2, w, h);
        progressBar.setValue(0);
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
      @Override
      public void run()
      {
        window.setVisible(false);
      }
    });
  }

  /**
   * Update the progress value of the progress bar.
   * @param n the new value to set.
   */
  public static void updateProgress(final int n)
  {
    if (progressBar != null) progressBar.setValue(n);
  }

  /**
   * Task for submitting the computation from a separate thread.
   * The goal is to avoid doing the calculations in the AWT event thread.
   */
  public static class AlignmentExecution implements Runnable
  {
    /**
     * the sequence to compare those in the database with.
     */
    private String targetSequence = null;
    /**
     * the name of the substitution matrix to use in the alignments.
     */
    private String matrix = null;
    /**
     * the path to the database of sequences.
     */
    private String dbPath = null;
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
    public AlignmentExecution(final String targetSequence, final String matrix, final String dbPath)
    {
      this.targetSequence = targetSequence;
      this.matrix = matrix;
      this.dbPath = dbPath;
    }

    /**
     * Perform the submission of the computation.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
      try
      {
        task = doPerform(targetSequence, matrix, dbPath);
        if (task != null)
        {
          ((AbstractOption) option.findFirstWithName("/resultSequenceText")).setValue(task.getSequence());
          ((AbstractOption) option.findFirstWithName("/score")).setValue(task.getResult());
        }
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }

    /**
     * Get the task that produced the maximum score.
     * @return an <code>SequenceAlignmentTask</code> instance.
     */
    public SequenceAlignmentTask getImage()
    {
      return task;
    }
  }
}
