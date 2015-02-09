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

import org.jppf.utils.streams.StreamUtils;

/**
 * This class reads articles from a wikipedia xml file.<br>
 * Here we use a naive approach to parsing the file. Since XML parsing is quite
 * slow and memory-hungry, we simply parse the file as text, looking for a start
 * and end tags for each article.
 * @author Laurent Cohen
 */
public class DataReader {
  /**
   * Start tag for an article.
   */
  private static final String ARTICLE_START = "<page>";
  /**
   * End tag for an article.
   */
  private static final String ARTICLE_END = "</page>";
  /**
   * The path to the data file to process.
   */
  private final String filename;
  /**
   * Reader used to get the articles line by line.
   */
  private BufferedReader reader = null;
  /**
   * Whether this data reader is closed.
   */
  private boolean closed = false;

  /**
   * Initiialize with the specified file.
   * @param filename the path to the file to process.
   */
  public DataReader(final String filename) {
    this.filename = filename;
  }

  /**
   * Read the next article.
   * @return the next article.
   * @throws Exception if any error occurs.
   */
  public String nextArticle() throws Exception {
    if (closed) return null;
    if (reader == null) reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "utf8"));
    String s = "";
    boolean found = false;
    while (!found) {
      s = reader.readLine();
      if (s == null) {
        close();
        return null;
      }
      if (s.indexOf(ARTICLE_START) >= 0) found = true;
    }
    if (!found) return null;
    StringBuilder sb = new StringBuilder();
    found = false;
    while (!found) {
      s = reader.readLine();
      if (s == null) {
        close();
        return null;
      }
      else if (s.indexOf(ARTICLE_END) >= 0) found = true;
      else sb.append(s).append('\n');
    }
    return sb.toString();
  }

  /**
   * Close this data reader and release its resources. 
   */
  public void close() {
    if (!closed) {
      closed = true;
      if (reader != null) StreamUtils.closeSilent(reader);
    }
  }

  /**
   * Determine whether this reader is closed.
   * @return <code>true</code> if this reader is closed, <code>false</code> otherwise.
   */
  public boolean isClosed() {
    return closed;
  }
}
