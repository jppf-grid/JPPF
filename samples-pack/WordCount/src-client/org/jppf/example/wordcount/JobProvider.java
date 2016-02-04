/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.example.wordcount;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.client.JPPFJob;
import org.jppf.client.utils.AbstractJPPFJobStream;
import org.jppf.node.protocol.Task;
import org.jppf.utils.StringUtils;

/**
 * This job provider builds a sequence of JPPF jobs based on a {@link DataReader} which
 * reads a Wikipedia database in XML format. Each task in the jobs is made of a configurable
 * number of Wikipedia articles which are then processed on the grid.
 * <p>This job provider is designed to produce a stream of jobs that can be used in a {@code for} loop:
 * <pre> DataReader reader = new DataReader("wikipedia-en-db.xml");
 * // reads parameters from the JPPF configuration
 * Params params = new Params();
 * JobProvider provider = new JobProvider(reader, params);
 * for (JPPFJob job: provider) {
 *   doSomething(job);
 * }</pre>
 * @author Laurent Cohen
 */
public class JobProvider extends AbstractJPPFJobStream {
  /**
   * The object that reads articles from a file.
   */
  private final DataReader reader;
  /**
   * The configuration parameters extracted from the configuration file.
   */
  private final Parameters params;
  /**
   * Processes and merges the results asynchronously in a single thread.
   */
  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  /**
   * Count of tasks whose results have been processed.
   */
  private final AtomicInteger totalTasksProcessed = new AtomicInteger(0);
  /**
   * Count of articles read from the file.
   */
  private int totalArticles = 0;
  /**
   * The total cumulated length of all articles.
   */
  private long totalArticlesLength = 0L;
  /**
   * The size of the smallest article.
   */
  private long minArticleLength = Long.MAX_VALUE;
  /**
   * The size of the biggest article.
   */
  private long maxArticleLength = 0L;
  /**
   * Count of articles read from the file.
   */
  private final AtomicInteger totalRedirects = new AtomicInteger(0);
  /**
   * Holds the merged results.
   */
  private final Map<String, Long> mergedResults = new HashMap<>();

  /**
   * Initialize this job provider with the specified application parameters.
   * @param concurrencyLimit the maximum number of jobs submitted concurrently.
   * @param params the parameters to use.
   */
  public JobProvider(final int concurrencyLimit, final Parameters params) {
    super(concurrencyLimit);
    this.params = params;
    reader = new DataReader(params.dataFile);
  }

  @Override
  public boolean hasNext() {
    return !reader.isClosed();
  }

  @Override
  public void close() {
    if (reader != null) reader.close();
    executor.shutdown();
    try {
      while (!executor.awaitTermination(1L, TimeUnit.MILLISECONDS));
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected JPPFJob createNextJob() {
    int taskCount  = 0;
    int totalJobArticles = 0;
    long totalLength = 0L;
    JPPFJob job = new JPPFJob();
    job.setName(String.format("WordCount-%04d", (getJobCount() + 1)));
    // job can be sent over this number of channels in parallel
    job.getClientSLA().setMaxChannels(params.nbChannels);
    try {
      // build up to {params.nbTasks} tasks
      while (!reader.isClosed() && (taskCount < params.nbTasks)) {
        int articleCount = 0;
        String article = null;
        List<String> list = new ArrayList<>(params.nbArticles);
        // read up to {params.nbArticles} articles for each task
        while (!reader.isClosed() && (articleCount < params.nbArticles)) {
          article = reader.nextArticle();
          if (article == null) break;
          list.add(article);
          articleCount++;
          int len = article.length();
          totalLength+= len;
          if (len < minArticleLength) minArticleLength = len;
          if (len > maxArticleLength) maxArticleLength = len;
        }
        if (articleCount > 0) {
          job.add(new WordCountTask(list));
          totalJobArticles += articleCount;
          taskCount++;
        }
      }
      if (taskCount > 0) {
        totalArticles += totalJobArticles;
        totalArticlesLength += totalLength;
        // set the job start timestamp
        job.getMetadata().setParameter("startTime", System.nanoTime());
        return job;
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  protected void processResults(final JPPFJob job) {
    executor.execute(new MergeResultsTask(job));
  }

  /**
   * Get the total number of executed tasks whose results have been received.
   * @return an int value.
   */
  public int getTotalTasksProcessed() {
    return totalTasksProcessed.get();
  }

  /**
   * Get the total number of articles read form the data file.
   * @return an int value.
   */
  public int getTotalArticles() {
    return totalArticles;
  }

  /**
   * Get the number of redirect found in the data file.
   * @return an int value.
   */
  public int getTotalRedirects() {
    return totalRedirects.get();
  }

  /**
   * Get the average of an article, computed over all the articles that were processed.
   * @return the average length indouble precision, or -1 if no article was processed.
   */
  public double getAverageArticleLength() {
    return (totalArticles > 0) ? (double) totalArticlesLength / (double) totalArticles : -1d;
  }

  /**
   * Get the size of the smallest article.
   * @return the size as a long value.
   */
  public long getMinArticleLength() {
    return minArticleLength;
  }

  /**
   * Get the size of the biggest article.
   * @return the size as a long value.
   */
  public long getMaxArticleLength() {
    return maxArticleLength;
  }

  /**
   * Get the merged results from all executed tasks.
   * @return  a mapping of words to their respective count.
   */
  public Map<String, Long> getMergedResults() {
    return mergedResults;
  }

  /**
   * This class merges the results received from a set of tasks into the aggregated map of word counts.
   */
  private class MergeResultsTask implements Runnable {
    /**
     * The job whose results are to be merged.
     */
    private final JPPFJob job;

    /**
     * Initialize with the specified set of tasks.
     * @param job the job to process.
     */
    public MergeResultsTask(final JPPFJob job) {
      this.job = job;
    }

    @Override
    public void run() {
      long jobRedirects = 0L;
      long jobArticles = 0L;
      for (Task<?> task: job.getAllResults()) {
        WordCountTask wTask = (WordCountTask) task;
        int nbRedirects = wTask.getNbRedirects();
        if (nbRedirects > 0) totalRedirects.addAndGet(nbRedirects);
        jobRedirects += nbRedirects;
        jobArticles += wTask.getNbArticles();
        Map<String, Long> map = wTask.getResult();
        if (map == null) continue;
        task.setResult(null); // to free some memory asap
        for (Map.Entry<String, Long> entry: map.entrySet()) {
          Long count = mergedResults.get(entry.getKey());
          long n = entry.getValue() + (count == null ? 0L : count);
          mergedResults.put(entry.getKey(), n);
        }
      }
      totalTasksProcessed.addAndGet(job.executedTaskCount());
      // job completion time in millis
      long jobCompletionTime = (System.nanoTime() - (Long) job.getMetadata().getParameter("startTime")) / 1_000_000L;
      System.out.printf("processed results of job '%s' - %,4d tasks, %,6d articles, including %,5d redirects. Completion time: %s%n",
        job.getName(), job.executedTaskCount(), jobArticles, jobRedirects, StringUtils.toStringDuration(jobCompletionTime));
    }
  }
}
