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

package org.jppf.samples.jaligner;

import jaligner.Sequence;
import jaligner.matrix.MatrixLoader;
import jaligner.util.SequenceParser;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

import javax.swing.*;

import org.apache.commons.logging.*;
import org.jppf.client.JPPFClient;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.task.storage.*;
import org.jppf.ui.options.*;
import org.jppf.utils.*;


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
	private static Log log = LogFactory.getLog(SequenceAlignmentRunner.class);
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
	public static void main(String[] args)
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
	 * @param dbPath the path to the databse of sequences.
	 * @param option an option used as an entry point to the UI.
	 * @throws Exception if the computation failed.
	 */
	public static void perform(String targetSequence, String matrix, String dbPath, Option option) throws Exception
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
	 * @param dbPath the path to the databse of sequences.
	 * @return the task wqith the maximum score.
	 * @throws Exception if the computation failed.
	 */
	public static SequenceAlignmentTask doPerform(String targetSequence, String matrix, String dbPath) throws Exception
	{
		long start = System.currentTimeMillis();
		//System.out.println("Target sequence:\n" + targetSequence);
		Sequence target = SequenceParser.parse(targetSequence);
		DataProvider dp = new MemoryMapDataProvider();
		dp.setValue(SequenceAlignmentTask.TARGET_SEQUENCE, target);
		dp.setValue(SequenceAlignmentTask.SCORING_MATRIX, MatrixLoader.load(matrix));

		System.out.println("Indexing sequence database...");
		String idx = dbPath+".idx";
		int nb = DatabaseHandler.generateIndex(dbPath, idx, null);
		System.out.println(""+nb+" sequences indexed");
		int n = 0;
		DatabaseHandler dh = new DatabaseHandler(dbPath, idx, null);
		List<JPPFTask> taskList = new ArrayList<JPPFTask>();
		boolean end = false;
		while (!end)
		{
			String s = dh.nextSequence();
			if (s == null) end = true;
			else taskList.add(new SequenceAlignmentTask(s, ++n));
		}
		long start2 = System.currentTimeMillis();
		//taskList = client.submit(taskList, dp);
		AlignmentResultCollector collector = new AlignmentResultCollector(taskList.size());
		client.submitNonBlocking(taskList, dp, collector);
		taskList = collector.waitForResults();
		long elapsed2 = System.currentTimeMillis() - start2;
		float maxScore = 0;
		SequenceAlignmentTask maxTask = null;
		for (JPPFTask t: taskList)
		{
			SequenceAlignmentTask task = (SequenceAlignmentTask) t;
			if (task.getException() != null)
			{
				String msg = "Exception in task #"+task.getNumber()+ ", sequence:\n"+task.getSequence();
				log.info(msg, task.getException());
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
	 * Load a sequence from the speicfied file path.
	 * @param path location of the sequence.
	 * @return sequence the sequence read form the file.
	 * @throws IOException if an error occrus when reading the file.
	 */
	private static String loadSampleSequence(String path) throws IOException
	{
		InputStream is = null;
		is = SequenceAlignmentRunner.class.getClassLoader().getResourceAsStream(path);
		if (is == null) is = new BufferedInputStream(new FileInputStream(path));
		
		StringBuffer buffer = new StringBuffer();
		int ch;
		while ((ch = is.read()) != -1)
		{
			buffer.append((char) ch);
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
			final Frame frame = Frame.getFrames()[0];
			progressBar = new JProgressBar();
			//progressBar.setIndeterminate(true);
			Font font = progressBar.getFont();
			Font f = new Font(font.getName(), Font.BOLD, 14);
			progressBar.setFont(f);
			progressBar.setString("Calculating, please wait ...");
			progressBar.setStringPainted(true);
			window = new JWindow(frame);
			window.getContentPane().add(progressBar);
			window.getContentPane().setBackground(Color.white);
			Dimension d = frame.getSize();
			int w = 300;
			int h = 60;
			window.setBounds((d.width-w)/2, (d.height-h)/2, w, h);
		}
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
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
	public static void updateProgress(int n)
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
		 * the path to the databse of sequences.
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
		 * @param dbPath the path to the databse of sequences.
		 */
		public AlignmentExecution(String targetSequence, String matrix, String dbPath)
		{
			this.targetSequence = targetSequence;
			this.matrix = matrix;
			this.dbPath = dbPath;
		}

		/**
		 * Perform the submission of the computation.
		 * @see java.lang.Runnable#run()
		 */
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