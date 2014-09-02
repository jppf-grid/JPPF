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

package org.jppf.example.wordcount.node;

import java.io.*;
import java.util.*;

import org.jppf.node.event.*;
import org.jppf.node.protocol.Task;
import org.jppf.utils.*;

/**
 * This node life cycle listener performs two tasks:
 * <ol>
 * <li>Load the disctionary once the node is connected to the server</li>
 * <li>When a job completes, aggregate the word counts of all its tasks into a single map</li>
 * </ol>
 * @author Laurent Cohen
 */
public class NodeListener extends NodeLifeCycleListenerAdapter implements  NodeLifeCycleErrorHandler {
  /**
   * Path to the dictionary file in the server's classpath.
   */
  private static final String DICTIONARY_PATH = "5desk.txt";
  //private static final String DICTIONARY_PATH = "dictonary_en_US.txt";
  /**
   * The dictionary of words to count.
   */
  private static Set<String> dictionary = null;

  /**
   * This method loads a dictionary from the server classpath.
   * {@inheritDoc}
   */
  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    // load the dictionary from the server
    try (BufferedReader reader = new BufferedReader(FileUtils.getFileReader(DICTIONARY_PATH))) {
      System.out.print("Loading dictionary '" + DICTIONARY_PATH + "' ");
      dictionary = new HashSet<>();
      String s = null;
      int count = 0;
      while ((s = reader.readLine()) != null) {
        count++;
        if (count % 10_000 == 0) System.out.print(".");
        // add the dictionary entry
        dictionary.add(s.trim().toLowerCase());
      }
      System.out.printf(" %d entries\n", dictionary.size());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * This method reduces the word counts for all tasks to a single word count map,
   * so this work doesn't have to be done on the client side. The aggregated map
   * is then set as the result of a single task, while the results of all other tasks
   * are set to <code>null</code>.
   * {@inheritDoc}
   */
  @Override
  @SuppressWarnings("unchecked")
  public void jobEnding(final NodeLifeCycleEvent event) {
    List<Task<?>> tasks = event.getTasks();
    if ((tasks == null) || tasks.isEmpty()) return;

    Map<String, Long> reduced = null;
    int initialIndex = 0;
    // find the first task whose result is not null
    // (the result is be null when all the articles of a task are redirects)
    while ((initialIndex < tasks.size()) && (reduced == null)) {
      reduced = (Map<String, Long>) tasks.get(initialIndex).getResult();
      if (reduced == null) initialIndex++;
    }
    if (reduced == null) return;
    // aggregate the word cunts of the other tasks into the first one found
    for (int i=initialIndex+1; i<tasks.size(); i++) {
      Task<?> task = tasks.get(i);
      Map<String, Long> map = (Map<String, Long>) task.getResult();
      if (map != null) {
        task.setResult(null);
        for (Map.Entry<String, Long> entry: map.entrySet()) {
          Long n = reduced.get(entry.getKey());
          if (n == null) n = entry.getValue();
          else n += entry.getValue();
          reduced.put(entry.getKey(), n);
        }
      }
    }
  }

  @Override
  public void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t) {
    System.out.println("error on listener " + listener + ", event type=" + event.getType() + " : " + ExceptionUtils.getStackTrace(t));
  }

  /**
   * Determines whether the specified word is in the dictionary.
   * @param word the word to check.
   * @return <code>true</code> if the word is in the dictionary, <code>false</code> otherwise.
   */
  public static boolean isInDictionary(final String word) {
    return dictionary.contains(word);
  }
}
