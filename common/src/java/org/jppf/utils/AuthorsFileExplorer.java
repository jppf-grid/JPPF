/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.utils;

import java.io.File;
import java.util.*;
import java.util.regex.*;

/**
 *
 * @author Laurent Cohen
 */
public class AuthorsFileExplorer extends FileExplorer {
  /**
   *
   */
  private final Set<String> authors = new HashSet<>();
  /**
   *
   */
  private static final Pattern AUTHOR_PATTERN = Pattern.compile("@author\\s+(.*)$?");
  /**
   *
   */
  private int matches = 0;

  /**
   *
   * @param root the root of the files to explore.
   */
  public AuthorsFileExplorer(final File root) {
    super(root, "java");
  }

  /**
   *
   * @param root the root of the files to explore.
   */
  public AuthorsFileExplorer(final String root) {
    super(root, "java");
  }

  @Override
  public void beforeReadFile(final File file) {
  }

  @Override
  public void afterReadFile(final File file, final String content) {
    Matcher matcher = AUTHOR_PATTERN.matcher(content);
    int count = 0;
    Set<String> temp = new HashSet<>();
    while (matcher.find()) {
      String author = matcher.group(1);
      if (!authors.contains(author)) {
        count++;
        temp.add(author);
        authors.add(author);
      }
    }
    if (temp.isEmpty()) System.out.printf("found no new author in file '%s'%n", file);
    else System.out.printf("found %d new author%s in file '%s' : %s%n", count, count > 1 ? "s" : "", file, temp);
    matches += count;
  }

  @Override
  public void beforeExploreDir(final File dir) {
    System.out.printf("exploring dir '%s'%n", dir);
  }

  @Override
  public void afterExploreDir(final File dir) {
  }

  /**
   * Get the authors found.
   * @return a list of authors.
   */
  public Set<String> getAuthors() {
    return authors;
  }

  /**
   *
   * @return .
   */
  public int getMatches() {
    return matches;
  }

  /**
   * 
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      File root = new File("C:/Workspaces/SourceForgeSVN");
      AuthorsFileExplorer explorer = new AuthorsFileExplorer(root);
      explorer.explore();
      System.out.printf("Found a total of %d authors under '%s' : %s%n", explorer.getMatches(), root, explorer.getAuthors());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
