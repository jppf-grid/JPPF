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
package org.jppf.example.wordcount;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import org.jppf.client.JPPFClient;
import org.jppf.client.JPPFConnectionPool;
import org.jppf.client.JPPFJob;
import org.jppf.utils.TimeMarker;
import org.jppf.utils.collections.CollectionMap;
import org.jppf.utils.collections.SortedSetSortedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    final Parameters params = new Parameters();
    TimeMarker marker = null;
    JobProvider provider = null;
    try (final JPPFClient client = new JPPFClient();
        // the auto-close of jobProvider will wait until all results are merged
        JobProvider jobProvider = new JobProvider(params.jobCapacity, params)) {
      provider = jobProvider;
      System.out.printf(Locale.US, "Processing '%s' with %,d articles per task, %,d tasks per job, nb channels = %d, max concurrent jobs = %d%n",
        params.dataFile, params.nbArticles, params.nbTasks, params.nbChannels, params.jobCapacity);
      final JPPFConnectionPool pool = client.awaitActiveConnectionPool();
      // set the pool size to the desired number of connections
      pool.setSize(params.jobCapacity);
      marker = new TimeMarker().start();
      // read the wikipedia file and build jobs according to the configuration parameters
      for (final JPPFJob job:  jobProvider) {
        if (job != null) client.submitAsync(job);
      }
      // wait until all job results have been processed
      while (jobProvider.hasPendingJob()) Thread.sleep(10L);

    } catch(final Exception e) {
      e.printStackTrace();
    }
    try {
      // group the words with equal count and sort by descending count then ascending word within each group,
      // then write the results to the specified file
      writeResults(sortResults(provider.getMergedResults()), "WordCountResults.txt");
      // print execution statistics
      System.out.printf(Locale.US, "processed %,d jobs for %,d articles in %s; total redirects = %,d; average article length = %,.0f; min = %,d; max = %,d%n",
        provider.getJobCount(), (provider.getTotalArticles() - provider.getTotalRedirects()), marker.stop().getLastElapsedAsString(), provider.getTotalRedirects(),
        provider.getAverageArticleLength(), provider.getMinArticleLength(), provider.getMaxArticleLength());
    } catch(final Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Sort the results according to word count.
   * @param mergedResults the merge results to sort.
   * @return a map of long values to collections of text words.
   */
  private static CollectionMap<Long, String> sortResults(final Map<String, Long> mergedResults) {
    System.out.printf("sorting %s results ... ", mergedResults.size());
    final TimeMarker marker = new TimeMarker().start();
    final Comparator<Long> comp = new Comparator<Long>() {
      @Override
      public int compare(final Long o1, final Long o2) {
        if (o1 == null) return o2 == null ? 0 : -1;
        else if (o2 == null) return 1;
        return o2.compareTo(o1);
      }
    };
    // using a multimap (JPPF implementation) makes code a lot simpler
    final CollectionMap<Long, String> result = new SortedSetSortedMap<>(comp);
    for (final Map.Entry<String, Long> entry: mergedResults.entrySet()) result.putValue(entry.getValue(), entry.getKey());
    System.out.printf("done in %s%n", marker.stop().getLastElapsedAsString());
    return result;
  }

  /**
   * Write the results to a file.
   * @param results the word count mappings to write.
   * @param destFile the path to the destination file.
   * @throws Exception if any error occurs.
   */
  private static void writeResults(final CollectionMap<Long, String> results, final String destFile) throws Exception {
    final String filename = "WordCountResults.txt";
    System.out.printf("writing results to '%s' ... ", filename);
    final TimeMarker marker = new TimeMarker().start();
    try (final PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(filename)))) {
      for (Map.Entry<Long, Collection<String>> entry: results.entrySet()) {
        final long n = entry.getKey();
        for (final String word: entry.getValue()) {
          writer.printf(Locale.US, "%-26s: %,12d%n", word, n);
        }
      }
    }
    System.out.printf("done in %s%n", marker.stop().getLastElapsedAsString());
  }
}
