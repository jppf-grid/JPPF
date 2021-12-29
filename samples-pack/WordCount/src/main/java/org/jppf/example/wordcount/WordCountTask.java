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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jppf.example.wordcount.node.NodeListener;
import org.jppf.node.protocol.AbstractTask;

/**
 * This task builds a word count map for a number of Wikipedia articles it takes as input.
 * @author Laurent Cohen
 */
public class WordCountTask extends AbstractTask<Map<String, Long>> {
  /**
   * The data in this task.
   */
  private List<String> articles = null;
  /**
   * Number of articles in the list.
   */
  private final int nbArticles;
  /**
   * Number of redirects found in the list of articles.
   */
  private int nbRedirects = 0;

  /**
   * Initialize with the specified data size.
   * @param articles the articles to process.
   */
  public WordCountTask(final List<String> articles) {
    this.articles = articles;
    this.nbArticles = (articles == null) ? 0 : articles.size();
  }

  /**
   * Perform the execution of this task.
   */
  @Override
  public void run() {
    Map<String, Long> countMap = null;
    try {
      for (String article: articles) {
        final String text = tagValue(article, "text");
        // skip articles that are mere redirects
        if (text.startsWith("#REDIRECT")) {
          nbRedirects++;
          continue;
        }
        final int limit = text.length() - 1;
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<text.length(); i++) {
          final char c = Character.toLowerCase(text.charAt(i));
          final boolean b = isWordChar(c);
          if (b) sb.append(c);
          if ((i >= limit) || !b) {
            final String key = sb.toString();
            if (isWord(key)) {
              if (countMap == null) {
                countMap = new HashMap<>();
                countMap.put(key, 1L);
              } else {
                final Long count = countMap.get(key);
                final long n = (count == null) ? 1L : count + 1;
                countMap.put(key, n);
              }
            }
            if (sb.length() > 0) sb = new StringBuilder();
          }
        }
      }
      setResult(countMap);
    } finally {
      // we don't need the articles as part of the results.
      // this will make returning the task results much faster.
      articles = null;
    }
  }

  /**
   * Find the content of the specified tag in the specified text.
   * @param text the text to look into.
   * @param tag the tag to look for.
   * @return the tag content as a string.
   */
  private static String tagValue(final String text, final String tag) {
    final String startTag = "<" + tag;
    int idx = text.indexOf(startTag);
    if (idx < 0) return null;
    idx = text.indexOf('>', idx + startTag.length());
    if (idx < 0) return null;
    final String endTag = "</" + tag + ">";
    final int idx2 = text.indexOf(endTag, idx);
    if (idx2 < 0) return null;
    return text.substring(idx + 1, idx2);
  }

  /**
   * Determine whether the specified character is part of a word or to be discarded.
   * @param c the character to examine.
   * @return <code>true</code> if the character is part of a word, <code>false</code> otherwise.
   */
  private static boolean isWordChar(final char c) {
    //return ((c >= 'a') && (c <='z')) || ((c >= 'A') && (c <='Z')) || (c == '-');
    return ((c >= 'a') && (c <='z')) || (c == '-');
  }

  /**
   * Determine whether the specified character sequence is a word or to be discarded.
   * @param s the character sequence to examine.
   * @return <code>true</code> if the character is part of a word, <code>false</code> otherwise.
   */
  private static boolean isWord(final String s) {
    return NodeListener.isInDictionary(s);
  }

  /**
   * Get the number of redirected articles.
   * @return the number as an int.
   */
  public int getNbRedirects() {
    return nbRedirects;
  }

  /**
   * Get the number of articles in this task.
   * @return the number of articles including redirects.
   */
  public int getNbArticles() {
    return nbArticles;
  }

  /**
   * Get the data in this task.
   * @return a list of wikipedia articles.
   */
  public List<String> getArticles() {
    return articles;
  }
}
