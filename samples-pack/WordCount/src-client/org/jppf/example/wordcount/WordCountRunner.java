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
package org.jppf.example.wordcount;

import java.io.*;
import java.text.NumberFormat;
import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Runner class for the &quot;Word Count &quot; example.
 * @author Laurent Cohen
 */
public class WordCountRunner {
  /**
   * Logger for this class.
   */
  static Logger log = LoggerFactory.getLogger(WordCountRunner.class);

  /**
   * Entry point for this class.
   * @param args not used.
   */
  public static void main(final String...args) {
    SubmitQueue queue = null;
    JobProvider jobProvider = null;
    NumberFormat nf = createFormatter();
    try {
      Parameters params = new Parameters();
      nf.setGroupingUsed(true);
      JPPFConfiguration.getProperties().setProperty("jppf.pool.size", String.valueOf(params.nbChannels));
      queue = new SubmitQueue(params.jobCapacity);
      System.out.println("Processing '" + params.dataFile + "' with " + nf.format(params.nbArticles) +
          " articles per task, " + nf.format(params.nbTasks) + " tasks per job, nb channels = " + params.nbChannels);
      jobProvider = new JobProvider(params);
      boolean end = false;
      long start = System.nanoTime();
      JPPFJob job;
      // read the wikipedia file and build a job according to the configuration parameters
      while ((job = jobProvider.nextJob()) != null) {
        // this call may block if the job capacity is reached
        queue.submit(job);
      }
      Object lock = new Object();
      // wait until all job results have been processed
      while (jobProvider.getTotalTasksProcessed() < jobProvider.getTotalTasksSent()) {
        synchronized(lock) {
          lock.wait(1L);
        }
      }
      long elapsed = (System.nanoTime() - start) / 1000000L;
      System.out.println("processed " + jobProvider.getJobCount() + " jobs for " + nf.format(jobProvider.getTotalArticles() - jobProvider.getTotalRedirects()) +
          " articles in " + StringUtils.toStringDuration(elapsed) + " (" + nf.format(elapsed) + " ms), total redirects = " + nf.format(jobProvider.getTotalRedirects()));

      // group the words with equal count and sort by descending count then ascending word within each group,
      // then write the results to the specified file
      writeResults(sortResults(jobProvider.getMergedResults()), "WordCountResults.txt");

    } catch(Exception e) {
      e.printStackTrace();
    } finally {
      if (queue != null) queue.stop();
      if (jobProvider != null) jobProvider.close();
    }
  }

  /**
   * Sort the results according to word count.
   * @param mergedResults the merge results to sort.
   * @return a map of long values to collections of text words.
   */
  private static CollectionMap<Long, String> sortResults(final Map<String, Long> mergedResults) {
    System.out.println("sorting " + mergedResults.size() + " results ...");
    long start = System.nanoTime();
    Comparator<Long> comp = new Comparator<Long>() {
      @Override
      public int compare(final Long o1, final Long o2) {
        if (o1 == null) return o2 == null ? 0 : -1;
        if (o2 == null) return o1 == null ? 0 : 1;
        return o2.compareTo(o1);
      }
    };
    // using a multimap (JPPF implementation) makes code a lot simpler
    CollectionMap<Long, String> result = new SortedSetSortedMap<>(comp);
    for (Map.Entry<String, Long> entry: mergedResults.entrySet()) result.putValue(entry.getValue(), entry.getKey());
    long elapsed = (System.nanoTime() - start) / 1000000L;
    NumberFormat nf = createFormatter();
    System.out.println("results sorted in " + StringUtils.toStringDuration(elapsed) + " (" + nf.format(elapsed) + " ms)");
    return result;
  }

  /**
   * Write the results to a file.
   * @param results the word count mappings to write.
   * @param destFile the path to the destination file.
   * @throws Exception if any error occurs.
   */
  private static void writeResults(final CollectionMap<Long, String> results, final String destFile) throws Exception {
    String filename = "WordCountResults.txt";
    System.out.println("writing results to '" + filename + "' ...");
    long start = System.nanoTime();
    NumberFormat nf = createFormatter();
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
      for (Map.Entry<Long, Collection<String>> entry: results.entrySet()) {
        long n = entry.getKey();
        for (String word: entry.getValue()) {
          writer.write(StringUtils.padRight(word, ' ', 26, false));
          writer.write(": ");
          writer.write(StringUtils.padLeft(nf.format(n), ' ', 12));
          writer.write('\n');
        }
      }
      writer.flush();
    }
    long elapsed = (System.nanoTime() - start) / 1000000L;
    System.out.println("results written in " + StringUtils.toStringDuration(elapsed) + " (" + nf.format(elapsed) + " ms)");
  }

  /**
   * Utility method to create a number formatter.
   * @return a {@link NumberFormat} instance.
   */
  public static NumberFormat createFormatter() {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setGroupingUsed(true);
    return nf;
  }
}
