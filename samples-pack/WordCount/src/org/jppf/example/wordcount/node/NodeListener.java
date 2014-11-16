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
import org.jppf.utils.ExceptionUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeListener implements NodeLifeCycleListener, NodeLifeCycleErrorHandler {
  /**
   * Path to the dictionary file in the server's classpath.
   */
  private static final String DICTIONARY_PATH = "dictonary_en_US.txt";
  /**
   * The dictionary of words to count.
   */
  private static Set<String> dictionary = null;

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event) {
    try {
      // load the dictionary from the server
      loadDictonary();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event) {
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event) {
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
    while ((initialIndex < tasks.size()) && (reduced == null)) {
      reduced = (Map<String, Long>) tasks.get(initialIndex).getResult();
      if (reduced == null) initialIndex++;
    }
    if (reduced == null) return;
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
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
  }

  @Override
  public void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t) {
    System.out.println("error on listener " + listener + ", event type=" + event.getType() + " : " + ExceptionUtils.getStackTrace(t));
  }

  /**
   * Load the dictionary form the server's cclasspath.
   * @throws Exception if any error occurs.
   */
  private void loadDictonary() throws Exception {
    BufferedReader reader = null;
    try {
      InputStream is = getClass().getClassLoader().getResourceAsStream(DICTIONARY_PATH);
      if (is == null) throw new RuntimeException("could not find '" + DICTIONARY_PATH + "'");
      dictionary = new HashSet<>();
      reader = new BufferedReader(new InputStreamReader(is));
      String s = "";
      while (s != null) {
        s = reader.readLine();
        if (s!= null) dictionary.add(s.trim().toLowerCase());
      }
      System.out.println("loaded dictionary: " + dictionary.size() + " entries");
    } finally {
      if (reader != null) reader.close();
    }
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
