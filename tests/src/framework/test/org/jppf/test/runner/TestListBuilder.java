/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package test.org.jppf.test.runner;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.jppf.utils.streams.StreamUtils;
import org.junit.Test;

/**
 * Collection of utility methods ot help building/executing the tests.
 * @author Laurent Cohen
 */
public class TestListBuilder {
  /**
   * The root directory to scan for tests.
   */
  private final File rootDir;
  /**
   * The root directory to scan for tests.
   */
  private final FileFilter filter;
  /**
   * A list of Java class names holding JUnit tests.
   */
  private final List<String> names = new ArrayList<>();

  /**
   * Initialize this test list builder witht he specified root directory.
   * @param rootDir the root directory to scan for tests.
   */
  public TestListBuilder(final File rootDir) {
    this(rootDir, new FileFilter() {
      @Override
      public boolean accept(final File file) {
        return (file != null) && !file.isDirectory() && file.getName().endsWith(".class");
      }
    });
  }


  /**
   * Initialize this test list builder witht he specified root directory.
   * @param rootDir the root directory to scan for tests.
   * @param filter a filter to apply to the scanned files.
   */
  public TestListBuilder(final File rootDir, final FileFilter filter) {
    this.rootDir = rootDir;
    this.filter = filter;
  }

  /**
   * Get the fully qualified class names of all source files in the specified source
   * directory and its sub-directories recursively.
   * @return a list of Java class names.
   * @throws Exception if I/O error occurs.
   */
  public List<String> buildList() throws Exception {
    return buildList("", rootDir);
  }

  /**
   * Get the fully qualified class names of all source files in the specified source
   * directory and its sub-directories recursively, which pass the specified filter.
   * @param prefix the package name for classes dicovered in the currenntly scanend folder.
   * @param dir the root of the source directory to scan.
   * @return a list of Java class names.
   * @throws Exception if any error occurs.
   */
  public List<String> buildList(final String prefix, final File dir) throws Exception {
    //System.out.println("exploring directory " + dir);
    File[] files = dir.listFiles();
    if (files == null) return names;
    for (File file: files) {
      if (file.isDirectory()) buildList(prefix + file.getName() + ".", file);
      else {
        String s = file.getName();
        int i = s.lastIndexOf(".");
        if (i >= 0) s = s.substring(0, i);
        String name = prefix + s;
        if (filter.accept(file) && hasJUnitTest(name)) {
          //System.out.println(name  + " is accepted");
          names.add(name);
        }
        //else System.out.println(name  + " is not accepted");
      }
    }
    return names;
  }

  /**
   * Determine whether the specified class hodls a method with a <code>@Test</code> annotation.
   * @param className the fully qualified name of the class to check.
   * @return a list of Java class names.
   * @throws Exception if any error occurs.
   */
  protected boolean hasJUnitTest(final String className) throws Exception {
    Class<?> clazz = Class.forName(className);
    int mod = clazz.getModifiers();
    if (Modifier.isAbstract(mod) || !Modifier.isPublic(mod)) return false;
    Method[] methods = clazz.getDeclaredMethods();
    for (Method m: methods) {
      mod = m.getModifiers();
      if (Modifier.isStatic(mod) || !Modifier.isPublic(mod)) continue;
      if (m.getAnnotation(Test.class) != null) return true;
    }
    return false;
  }

  /**
   * Write the list of java class names to a file, one per line.
   * @param dest the file to write to.
   * @throws IOException if I/O error occurs.
   */
  public void writeTestList(final File dest) throws IOException {
    BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
    try {
      for (String name: names) writer.write(name + '\n');
    } finally {
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
  public static void main(final String[] args) {
    try {
      File srcDir = new File(args[0]);
      TestListBuilder builder = new TestListBuilder(srcDir);
      List<String> classNames = builder.buildList();
      File dest = new File(args[1]);
      builder.writeTestList(dest);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
