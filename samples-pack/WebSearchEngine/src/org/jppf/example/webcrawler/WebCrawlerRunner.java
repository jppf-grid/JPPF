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

package org.jppf.example.webcrawler;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFJob;
import org.jppf.node.protocol.Task;
import org.jppf.ui.options.AbstractOption;
import org.jppf.ui.options.Option;
import org.jppf.utils.JPPFConfiguration;
import org.jppf.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Example of a workflow to visit a web ste starting from a given page, and matching the visited
 * pages with a user-specified search query.
 * @author Laurent Cohen
 */
public class WebCrawlerRunner
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(WebCrawlerRunner.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The JPPF client.
   */
  private static JPPFClient client = null;
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
   * Count of visited urls.
   */
  private static int urlCount = 0;

  /**
   * Run the sample.
   * @param url the start url, specified in the UI.
   * @param query the search query specified by the user.
   * @param depth the links depth to search.
   * @param option an option used as an entry point to the UI.
   * @throws Exception if the computation failed.
   */
  public static void perform(final String url, final String query, final int depth, final Option option) throws Exception
  {
    synchronized(WebCrawlerRunner.class)
    {
      if (client == null) client = new JPPFClient();
    }
    init();
    WebCrawlerRunner.option = option;
    urlCount = 0;
    createOrDisplayWaitWindow();
    CrawlExecution exec = new CrawlExecution(url, query, depth);
    executor.submit(exec);
  }

  /**
   * Run the sample.
   * @param urls the list of urls to crawl.
   * @param query the name of the substitution matrix to use in the alignments.
   * @param doSearch determines whether the search should also be done.
   * @return a list of URLs to visit.
   * @throws Exception if the computation failed.
   */
  public static List<Task<?>> doPerform(final Collection<String> urls, final String query, final boolean doSearch) throws Exception
  {
    int n = 0;
    JPPFJob job = new JPPFJob();
    for (String url: urls)
    {
      try
      {
      job.add(new CrawlerTask(url, query, ++n, doSearch));
      }
      catch(Throwable t)
      {
        log.error(t.getMessage(), t);
        if (t instanceof Exception) throw (Exception) t;
        else if (t instanceof Error) throw (Error) t;
      }
    }
    if (job.getJobTasks() == null) return new ArrayList<>();
    job.addJobListener(new CrawlerResultCollector());
    job.setBlocking(false);
    client.submitJob(job);
    return job.awaitResults();
  }

  /**
   * Initialize http connection settings from the configuration file.
   */
  private static void init()
  {
    int n = JPPFConfiguration.getProperties().getInt("http.socket.timeout", 3000);
    JPPFHttpDefaultParamsFactory.setSocketTimeout(n);
    n = JPPFConfiguration.getProperties().getInt("http.method.retry-handler.retries", 2);
    JPPFHttpDefaultParamsFactory.setMaxConnectionRetries(n);
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
      @Override
      public void run()
      {
        Dimension d = window.getOwner().getSize();
        Point p = window.getOwner().getLocationOnScreen();
        int w = 300;
        int h = 60;
        window.setBounds(p.x+(d.width-w)/2, p.y+(d.height-h)/2, w, h);
        updateProgress(0);
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
  public static synchronized void updateProgress(final int n)
  {
    urlCount += n;
    if (progressBar != null)
    {
      progressBar.setString("Visited urls: " + urlCount);
    }
  }

  /**
   * Task for submitting the computation from a separate thread.
   * The goal is to avoid doing the calculations in the AWT event thread.
   */
  public static class CrawlExecution implements Runnable
  {
    /**
     * the sequence to compare those in the database with.
     */
    private String url = null;
    /**
     * the name of the substitution matrix to use in the alignments.
     */
    private String query = null;
    /**
     * the search depth from the start url.
     */
    private int depth = 0;

    /**
     * Initialize this task with the specified parameters.
     * @param url the sequence to compare those in the database with.
     * @param query the name of the substitution matrix to use in the alignments.
     * @param depth the path to the database of sequences.
     */
    public CrawlExecution(final String url, final String query, final int depth)
    {
      this.url = url;
      this.query = query;
      this.depth = depth;
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
        long start = System.currentTimeMillis();
        Set<LinkMatch> results = new TreeSet<>(new LinkMatch.Comparator());
        Set<String> toSearch = new HashSet<>();
        toSearch.add(url);
        List<String> temp = new ArrayList<>();
        temp.add(url);
        for (int i=0; i<=depth; i++)
        {
          boolean doSearch = (i >= depth);
          List<Task<?>> tasks = doPerform(doSearch ? toSearch : temp, query, doSearch);
          temp.clear();
          for (Task<?> t: tasks)
          {
            CrawlerTask task = (CrawlerTask) t;
            if (task.getThrowable() != null)
            {
              String msg = "Exception in task #"+task.getNumber()+ ", url: "+ task.getUrl();
              log.info(msg, task.getThrowable());
              continue;
            }
            Collection<LinkMatch> matchList = task.getMatchedLinks();
            for (LinkMatch lm: matchList)
            {
              if (!results.contains(lm)) results.add(lm);
            }
            Collection<String> urlList = task.getToVisit();
            for (String s: urlList)
            {
              if (!toSearch.contains(s))
              {
                toSearch.add(s);
                temp.add(s);
              }
            }
          }
        }
        StringBuilder sb = new StringBuilder();
        for (LinkMatch lm: results)
        {
          sb.append(StringUtils.padLeft(""+lm.relevance, ' ', 6)).append("     ");
          sb.append(lm.url).append('\n');
        }
        ((AbstractOption) option.findFirstWithName("/resultText")).setValue(sb.toString());
        long elapsed = System.currentTimeMillis() - start;
        hideWaitWindow();
        log.info("Computation done in " + elapsed + " ms");
      }
      catch(Exception e)
      {
        log.error(e.getMessage(), e);
      }
    }
  }
}
