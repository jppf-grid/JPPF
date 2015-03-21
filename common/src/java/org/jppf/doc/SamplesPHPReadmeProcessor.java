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

package org.jppf.doc;

import java.io.File;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.utils.FileUtils;

/**
 * This utility generates the doc-source php files for the samples pack from the readme in each sample.
 * @author Laurent Cohen
 */
public class SamplesPHPReadmeProcessor implements Runnable
{
  /**
   * Marks the start of the readme's content.
   */
  private static final String START_CONTENT_TAG = "<!-- ${SAMPLE_START_CONTENT} -->";
  /**
   * Marks the end of the readme's content.
   */
  private static final String END_CONTENT_TAG = "<!-- ${SAMPLE_END_CONTENT} -->";
  /**
   * The source directory from which all Readme.html are found.
   */
  private File sourceDir = null;
  /**
   * The destination directory where all Readme.php are generated.
   */
  private File destDir = null;
  /**
   * The template file for each Readme.php.
   */
  private String template = null;

  /**
   * Initialize this processor with the specified source, destination and template file path.
   * @param sourceDir the source directory from which all Readme.html are found.
   * @param destDir the destination directory where all Readme.php are generated.
   * @param template the template file for each Readme.php.
   * @throws Exception if any error occurs.
   */
  public SamplesPHPReadmeProcessor(final File sourceDir, final File destDir, final File template) throws Exception
  {
    this.sourceDir = sourceDir;
    this.destDir = destDir;
    this.template = FileUtils.readTextFile(template);
  }

  /**
   * Get the list of all Readme.html files in the samples pack.
   * @return a list of files.
   * @throws Exception if any error occurs.
   */
  private List<File> getHTMLFiles() throws Exception
  {
    List<File> result = new ArrayList<>();
    File[] subdirs = sourceDir.listFiles(new JPPFDirFilter());
    for (File dir: subdirs)
    {
      File readme = new File(dir, "Readme.html");
      if (readme.exists()) result.add(readme);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void run()
  {
    try
    {
      List<File> list = getHTMLFiles();
      for (File file: list) processFile(file);
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }

  /**
   * Process the specified html file into an equivalent php file.
   * @param file the html file to process.
   * @throws Exception if any error occurs.
   */
  private void processFile(final File file) throws Exception
  {
    System.out.println("processing input file " + file);
    String text = FileUtils.readTextFile(file);
    int idx = text.indexOf("<h1>");
    if (idx < 0) throw new JPPFException("could not find start of title for " + file);
    int idx2 = text.indexOf("</h1>");
    if (idx2 < 0) throw new JPPFException("could not find end of title for " + file);
    String title = text.substring(idx + 4, idx2);
    idx = text.indexOf(START_CONTENT_TAG);
    if (idx < 0) throw new JPPFException("could not find start of content for " + file);
    idx2 = text.indexOf(END_CONTENT_TAG);
    if (idx2 < 0) throw new JPPFException("could not find end of content for " + file);
    String content = text.substring(idx + START_CONTENT_TAG.length(), idx2);
    content = content.replace("Readme.html", "Readme.php");
    String result = template.replace("${TITLE}", title);
    result = result.replace("${SAMPLE_README}", content);
    int len = sourceDir.getCanonicalPath().length();
    String s = file.getParentFile().getCanonicalPath().substring(len);
    if (s.startsWith("/") || s.startsWith("\\")) s = s.substring(1);
    s += "/Readme.php";
    File outFile = new File(destDir, s);
    FileUtils.mkdirs(outFile);
    FileUtils.writeTextFile(outFile, result);
    System.out.println("writing output file " + outFile);
  }

  /**
   * Run this utility with the specified command-line parameters.
   * @param args the source and destination directories.
   */
  public static void main(final String[] args)
  {
    try
    {
      File srcDir = new File(args[0]);
      File destDir = new File(args[1]);
      File template = new File(args[2]);
      new SamplesPHPReadmeProcessor(srcDir, destDir, template).run();
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
