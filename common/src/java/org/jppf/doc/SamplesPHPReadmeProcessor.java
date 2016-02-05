/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.io.File;
import java.util.*;

import org.jppf.utils.FileUtils;
import org.jppf.utils.streams.StreamUtils;

/**
 * This utility generates the doc-source php files for the samples pack from the readme in each sample.
 * @author Laurent Cohen
 */
public class SamplesPHPReadmeProcessor implements Runnable {
  /**
   * The source directory from which all Readme.html are found.
   */
  private File sourceDir = null;
  /**
   * The destination directory where all Readme.php are generated.
   */
  private File destDir = null;

  /**
   * Initialize this processor with the specified source, destination and template file path.
   * @param sourceDir the source directory from which all Readme.html are found.
   * @param destDir the destination directory where all Readme.php are generated.
   * @throws Exception if any error occurs.
   */
  public SamplesPHPReadmeProcessor(final File sourceDir, final File destDir) throws Exception {
    this.sourceDir = sourceDir;
    this.destDir = destDir;
  }

  /**
   * Get the list of all Readme.html files in the samples pack.
   * @return a list of files.
   * @throws Exception if any error occurs.
   */
  private List<File> getHTMLFiles() throws Exception {
    List<File> result = new ArrayList<>();
    File[] subdirs = sourceDir.listFiles(new JPPFDirFilter());
    for (File dir : subdirs) {
      File readme = new File(dir, "Readme.html");
      if (readme.exists()) result.add(readme);
    }
    return result;
  }

  @Override
  public void run() {
    try {
      List<File> list = getHTMLFiles();
      for (File file : list) processFile(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Process the specified html file into an equivalent php file.
   * @param file the html file to process.
   * @throws Exception if any error occurs.
   */
  private void processFile(final File file) throws Exception {
    System.out.println("processing input file " + file);
    int len = sourceDir.getCanonicalPath().length();
    String s = file.getParentFile().getCanonicalPath().substring(len);
    if (s.startsWith("/") || s.startsWith("\\")) s = s.substring(1);
    //String[] filenames = { "/index.php", "/Readme.php" };
    String[] filenames = { "/index.php" };
    for (String name: filenames) {
      File outFile = new File(destDir, s + name);
      FileUtils.mkdirs(outFile);
      StreamUtils.copyFile(file, outFile);
      System.out.println("writing output file " + outFile);
    }
  }

  /**
   * Run this utility with the specified command-line parameters.
   * @param args the source and destination directories.
   */
  public static void main(final String[] args) {
    try {
      File srcDir = new File(args[0]);
      File destDir = new File(args[1]);
      new SamplesPHPReadmeProcessor(srcDir, destDir).run();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
