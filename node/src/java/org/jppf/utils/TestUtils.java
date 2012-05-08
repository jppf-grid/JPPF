/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

import java.io.*;
import java.util.*;

import org.jppf.utils.streams.StreamUtils;

/**
 * Collection of utility methods ot help building/executing the tests.
 * @author Laurent Cohen
 */
public class TestUtils
{
  /**
   * Get the fully qualified class names of all source files in the specified source
   * directory and its sub-directories recursively.
   * @param srcDir the root of the source directory to scan.
   * @return a list of Java class names.
   * @throws IOException if I/O error occurs.
   */
  public static List<String> classNamesFromSourceDir(final File srcDir) throws IOException
  {
    FileFilter filter = new FileFilter()
    {
      @Override
      public boolean accept(final File file)
      {
        if ((file == null) || file.isDirectory()) return false;
        String name = file.getName();
        return name.startsWith("Test") && name.endsWith(".java");
      }
    };
    return classNamesFromSourceDir("", srcDir, filter, new ArrayList<String>());
  }

  /**
   * Get the fully qualified class names of all source files in the specified source
   * directory and its sub-directories recursively, which pass the specified filter.
   * @param prefix the package name for classes dicovered in the currenntly scanend folder.
   * @param dir the root of the source directory to scan.
   * @param filter a filter to apply to the scanned source files.
   * @param names the list of class names being built.
   * @return a list of Java class names.
   * @throws IOException if I/O error occurs.
   */
  public static List<String> classNamesFromSourceDir(final String prefix, final File dir, final FileFilter filter, final List<String> names) throws IOException
  {
    File[] files = dir.listFiles();
    if (files == null) return names;
    for (File file: files)
    {
      if (file.isDirectory()) classNamesFromSourceDir(prefix + file.getName() + ".", file, filter, names);
      else
      {
        if (filter.accept(file))
        {
          String s = file.getName();
          // 5 = ".java".length()
          s = s.substring(0, s.length() - 5);
          names.add(prefix + s);
        }
      }
    }
    return names;
  }

  /**
   * Write a list of java class names to a file, one per line.
   * @param dest the file to write to.
   * @param classNames a list of Java class names.
   * @throws IOException if I/O error occurs.
   */
  public static void writeTestList(final File dest, final List<String> classNames) throws IOException
  {
    BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
    try
    {
      for (String name: classNames) writer.write(name + '\n');
    }
    finally
    {
      StreamUtils.closeSilent(writer);
    }
  }

  /**
   * Create a resource file with a list of all the JUnit test classes found in a specified folder.
   * @param args the following arguments are processed:
   * <ul>
   * <li><code>args[0]</code> is the folder where the test source files are</li>
   * <li><code>args[1]</code> is the path for the file to write the class names to</li>
   * </ul>
   */
  public static void main(final String[] args)
  {
    try
    {
      File srcDir = new File(args[0]);
      List<String> classNames = classNamesFromSourceDir(srcDir);
      File dest = new File(args[1]);
      writeTestList(dest, classNames);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
