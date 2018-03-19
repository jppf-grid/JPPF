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

package org.jppf.doc;

import java.io.*;
import java.util.*;

import org.jppf.utils.FileUtils;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class FileExplorer {
  /**
   * The accepted file extensions.
   */
  private final Set<String> exts = new HashSet<>();
  /**
   * The root of the file hierarchy to explore.
   */
  private final File root;
  /**
   * File filter based on the provided file extensions.
   */
  private final ExploreFilter filter;

  /**
   * Intiialize this file explorer.
   * @param root the root of the file hierarchy to explore.
   * @param extensions the accepted file extensions.
   */
  public FileExplorer(final String root, final String... extensions) {
    this(new File(root), extensions);
  }

  /**
   * Intiialize this file explorer.
   * @param root the root of the file hierarchy to explore.
   * @param extensions the accepted file extensions.
   */
  public FileExplorer(final File root, final String... extensions) {
    if (root == null) throw new IllegalArgumentException("root cannot be null");
    this.root = root;
    for (String s: extensions) exts.add(s.startsWith(".") ? s.substring(1) : s);
    this.filter = new ExploreFilter();
  }

  /**
   * Called before reading the content of the file currently explored.
   * @param file the file being explored.
   */
  public abstract void beforeReadFile(final File file);

  /**
   * Called before reading the content of the file currently explored.
   * @param file the file being explored.
   * @param content the file content as a string.
   */
  public abstract void afterReadFile(final File file, final String content);

  /**
   * Called before exploring a directory.
   * @param dir the directory being explored.
   */
  public abstract void beforeExploreDir(final File dir);

  /**
   * Called after exploring a directory.
   * @param dir the directory being explored.
   */
  public abstract void afterExploreDir(final File dir);

  /**
   * 
   */
  public void explore() {
    if (root.isDirectory()) exploreDirectory(root);
    else if (filter.accept(root)) exploreFile(root);
  }

  /**
   * Explore the specified file.
   * @param file the file to explore, guaranteed not to be a directory.
   */
  private void exploreFile(final File file) {
    beforeReadFile(file);
    String content = null;
    try {
      content = FileUtils.readTextFile(file);
    } catch(final IOException e) {
      e.printStackTrace();
    }
    afterReadFile(file, content);
  }

  /**
   * Explore the specified file.
   * @param dir the directory to explore, guaranteed to be a directory.
   */
  private void exploreDirectory(final File dir) {
    beforeExploreDir(dir);
    final File[] children = dir.listFiles(filter);
    for (final File child: children) {
      if (child.isDirectory()) exploreDirectory(child);
      else exploreFile(child);
    }
    afterExploreDir(dir);
  }

  /**
   * File filter based on a set of extensions.
   * @exclude
   */
  public class ExploreFilter implements FileFilter {
    @Override
    public boolean accept(final File file) {
      if (file.isDirectory()) return true;
      final String ext = FileUtils.getFileExtension(file);
      if (ext == null) return false;
      for (final String s: exts) {
        if (ext.equalsIgnoreCase(s)) return true;
      }
      return false;
    }
  }
}
