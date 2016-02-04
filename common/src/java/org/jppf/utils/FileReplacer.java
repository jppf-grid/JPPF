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

package org.jppf.utils;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.slf4j.*;

/**
 * Utility to replace multiline content in text files.
 * This is the same kind of utility as the Ant "replace" task, except that it works.
 * @author Laurent Cohen
 * @exclude
 */
public class FileReplacer {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(FileReplacer.class);
  /**
   * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
   */
  private boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  //private boolean debugEnabled = true;
  /**
   * File containing the content to replace.
   */
  private String src = null;
  /**
   * File containing the replacement content.
   */
  private String dest = null;
  /**
   * Determines whether only the search is performed (no replacement).
   */
  private boolean searchOnly = true;
  /**
   * The pattern to using for matching the source string.
   */
  private Pattern pattern = null;
  /**
   * The filter to use when looking for files to process.
   */
  private ReplacerFilter filter = null;
  /**
   * Number of replacements actually performed.
   */
  private int nbReplacements = 0;
  /**
   * Number of files actually changed.
   */
  private int nbFilesChanged = 0;

  /**
   * Replace, in a set of specified files, a string with another.
   * @param rootDir the root directory from where to start search.
   * @param srcFile file containing the content to replace.
   * @param destFile file containing the replacement content.
   * @param ext comma-separated list of file extensions to process.
   * @param searchOnly determines whether only the search is performed (no replacement).
   * @param regex if true, then interpret the source as a regular expressison.
   * @throws Exception if an error occurs while performing the replacements.
   */
  public void replace(final String rootDir, final String srcFile, final String destFile, final String ext, final boolean searchOnly, final boolean regex) throws Exception {
    src = FileUtils.readTextFile(srcFile);
    dest = FileUtils.readTextFile(destFile);
    if (src.endsWith("\n") && dest.endsWith("\n")) {
      src = src.substring(0, src.length() - 1);
      dest = dest.substring(0, dest.length() - 1);
    }
    this.searchOnly = searchOnly;
    if (regex) pattern = Pattern.compile(src);
    else pattern = Pattern.compile(src, Pattern.LITERAL);
    filter = new ReplacerFilter(ext);
    File f = new File(rootDir);
    nbFilesChanged = 0;
    nbReplacements = 0;
    if (f.isDirectory()) replaceFolder(f);
    else replaceFile(f);
    log.info("Total number of occurrences found: " + nbReplacements);
    log.info("Total number of files" + (searchOnly ? " that would have been" : "") + " changed: " + nbFilesChanged);
  }

  /**
   * Recursively process a folder.
   * @param folder the process to visit.
   * @throws Exception if an error occurs while processing the folder.
   */
  private void replaceFolder(final File folder) throws Exception {
    //if (debugEnabled) log.info("Processing folder " + folder.getAbsolutePath());
    print("Processing folder " + folder.getAbsolutePath());
    File[] fileList = folder.listFiles(filter);
    List<File> folders = new ArrayList<>();
    List<File> files = new ArrayList<>();
    for (File f: fileList) {
      if (f.isDirectory()) folders.add(f);
      else files.add(f);
    }
    for (File f: files) replaceFile(f);
    for (File f: folders) replaceFolder(f);
  }


  /**
   * Recursively process a single file.
   * @param file the file to process.
   * @throws Exception if an error occurs while processing the folder.
   */
  private void replaceFile(final File file) throws Exception {
    String content = FileUtils.readTextFile(file.getPath());
    Matcher matcher = pattern.matcher(content);
    boolean b = true;
    int nbFound = 0;
    int start = 0;
    while (b) {
      b = matcher.find(start);
      if (b) {
        nbFound++;
        start = matcher.end();
      }
    }
    if (nbFound > 0) {
      nbFilesChanged++;
      nbReplacements += nbFound;
      log.info("Found " + nbFound + " occurrence" + (nbFound > 1 ? "s" : "") + " of the sequence in file '" + file + '\'');
      String s = matcher.replaceAll(dest);
      if (debugEnabled) log.debug("Content with replacements performed:\n" + s);
      if (!searchOnly) FileUtils.writeTextFile(file.getPath(), s);
    }
    else if (debugEnabled) log.debug("Sequence not found in file '"+file+ '\'');
  }

  /**
   * Main entry point.
   * <p>The arguments must be specified as described in the clsss :<br/>
   * -i searchFile : the file containing the text to search for, can be multi-lines<br/>
   * -o replacementFile : the file containing the replacement text<br/>
   * -f rootDir : the root directory from which to search files recursively<br/>
   * -e extensions : a comma-separated list of file extensions, used as file filter; e.g. java,html,xml<br/>
   * -p : if specified then the program only shows matching files, but does not do any replacement<br/>
   * -r : if specified, then the text in the search and replacement files are interpreted as regex and replacement expressions<br/>
   * @param args defines the text search, the text to replace it with, the file extensions to process, and whether changes are only simulated.
   */
  public static void main(final String...args) {
    try {
      Arguments a = parseArguments(args);
      System.out.println("using " + a);
      FileReplacer replacer = new FileReplacer();
      replacer.replace(a.root, a.in, a.out, a.exts, a.searchOnly, a.regex);
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Print hte specified message ot the console and to the log file.
   * @param message the message to print.
   */
  private static void print(final String message) {
    //log.info(message);
    System.out.println(message);
  }

  /**
   * Encapsulates the command-line arguments.
   * @exclude
   */
  public static class Arguments {
    /**
     * The input file containing the the text or expression to match.
     */
    public String in = null;
    /**
     * The input file containing the the replacement text.
     */
    public String out = null;
    /**
     * The root folder in which to perform the search.
     */
    public String root = null;
    /**
     * The file extensions to look for.
     */
    public String exts = null;
    /**
     * If true, then only find matches but do not actually replace in the matching files.
     */
    public boolean searchOnly = true;
    /**
     * If true, then interpret the content of 'in' as a regular expression.
     */
    public boolean regex = false;

    @Override
    public String toString() {
      return "Arguments[in=" + in + ", out=" + out + ", root=" + root + ", exts=" + exts + ", searchOnly=" + searchOnly + ", regex=" + regex + "]";
    }
  }

  /**
   * Parse the command-line arguments into a usable object.
   * @param args the command-line arguments to parse.
   * @return an <code>Arguments</code> instance.
   * @throws Exception if any erorr occurs.
   */
  private static Arguments parseArguments(final String...args) throws Exception {
    Arguments ag = new Arguments();
    for (int i=0; i<args.length; i++) {
      if ("-i".equals(args[i])) ag.in = args[++i];
      else if ("-o".equals(args[i])) ag.out = args[++i];
      else if ("-f".equals(args[i])) ag.root = args[++i];
      else if ("-e".equals(args[i])) ag.exts = args[++i];
      else if ("-p".equals(args[i])) ag.searchOnly = false;
      else if ("-r".equals(args[i])) ag.regex = true;
    }
    return ag;
  }

  /**
   * File filter based on a set of extensions.
   * @exclude
   */
  public static class ReplacerFilter implements FileFilter {
    /**
     * The list of file extensions to process.
     */
    private String[] extensions = null;

    /**
     * Initializer this filter with the specified set of file extensions.
     * @param ext a comma-separated list of file extensions to process.
     */
    public ReplacerFilter(final String ext) {
      String s = (ext == null) ? "" : ext;
      extensions = RegexUtils.COMMA_PATTERN.split(s);
      for (int i=0; i<extensions.length; i++) extensions[i] = extensions[i].trim();
    }

    /**
     * Tests whether or not the specified abstract pathname should be included in a pathname list.
     * @param file the abstract pathname to be tested
     * @return true if and only if pathname  should be included.
     */
    @Override
    public boolean accept(final File file) {
      if (file.isDirectory()) return true;
      String ext = FileUtils.getFileExtension(file);
      if (ext == null) return false;
      for (String s: extensions) {
        if (ext.equalsIgnoreCase(s)) return true;
      }
      return false;
    }
  }
}
