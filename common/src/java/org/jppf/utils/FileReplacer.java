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

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.jppf.utils.cli.NamedArguments;
import org.jppf.utils.streams.StreamUtils;
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
   * Replace, in a set of specified files, a piece of text with another. The text can be multi-lines.
   * @param args the parameters for searching, matching replacing.
   * @throws Exception if an error occurs while performing the replacements.
   */
  public void replace(final NamedArguments args) throws Exception {
    src = FileUtils.readTextFile(args.getString("-i"));
    dest = FileUtils.readTextFile(args.getString("-o"));
    if (src.endsWith("\n") && dest.endsWith("\n")) {
      src = src.substring(0, src.length() - 1);
      dest = dest.substring(0, dest.length() - 1);
    }
    this.searchOnly = args.getBoolean("-p", false);
    if (args.getBoolean("-r", false)) pattern = Pattern.compile(src);
    else pattern = Pattern.compile(src, Pattern.LITERAL);
    filter = new ReplacerFilter(args.getStringArray("-e", ","), args.getStringArray("-ef", ","));
    final File f = new File(args.getString("-f"));
    nbFilesChanged = 0;
    nbReplacements = 0;
    if (f.isDirectory()) replaceFolder(f);
    else replaceFile(f);
    StreamUtils.printf(log, "Total number of occurrences found: %d", nbReplacements);
    StreamUtils.printf(log, "Total number of files %s changed: %d", (searchOnly ? " that would have been" : ""), nbFilesChanged);
  }

  /**
   * Recursively process a folder.
   * @param folder the process to visit.
   * @throws Exception if an error occurs while processing the folder.
   */
  private void replaceFolder(final File folder) throws Exception {
    //if (debugEnabled) log.info("Processing folder " + folder.getAbsolutePath());
    if (debugEnabled) StreamUtils.printf(log, "Processing folder %s", folder.getAbsolutePath());
    final File[] fileList = folder.listFiles(filter);
    final List<File> folders = new ArrayList<>();
    final List<File> files = new ArrayList<>();
    for (final File f: fileList) {
      if (f.isDirectory()) folders.add(f);
      else files.add(f);
    }
    for (final File f: files) replaceFile(f);
    for (final File f: folders) replaceFolder(f);
  }


  /**
   * Recursively process a single file.
   * @param file the file to process.
   * @throws Exception if an error occurs while processing the folder.
   */
  private void replaceFile(final File file) throws Exception {
    final String content = FileUtils.readTextFile(file.getPath());
    final Matcher matcher = pattern.matcher(content);
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
      StreamUtils.printf(log, "Found %d ocurrence%s of the sequence in file '%s'", nbFound, (nbFound > 1 ? "s" : ""), file);
      final String s = matcher.replaceAll(dest);
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
      System.out.println("using " + Arrays.asList(args));
      final NamedArguments namedArgs = new NamedArguments()
        .addArg("-i", "The input file containing the text or expression to match")
        .addArg("-o", "The input file containing the replacement text")
        .addArg("-f", "The root folder in which to perform the search")
        .addArg("-e", "The file extensions to look for (comma-separated list without the '.' nor spaces)")
        .addSwitch("-p", "If specified, then only find matches but do not actually replace in the matching files")
        .addSwitch("-r", "If specified, then interpret the content of '-i' as a regular expression")
        .addArg("-ef", "A list of comma-separated file patterns to exclude folders from the search")
        .parseArguments(args);
      System.out.println("parsed arguments:\n" + namedArgs);
      //namedArgs.printUsage();
      new FileReplacer().replace(namedArgs);
    } catch(final Exception e) {
      e.printStackTrace();
    }
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
     * The patterns used to exclude specified folders.
     */
    private Pattern[] folderExlusionPatterns = null;

    /**
     * Initializer this filter with the specified set of file extensions.
     * @param exts an array of file extensions to process.
     * @param exc a comma-separated list of regex used to exclude folders.
     */
    public ReplacerFilter(final String[] exts, final String[] exc) {
      this.extensions = exts;
      if (exc != null) {
        for (int i=0; i<exc.length; i++) exc[i] = exc[i].replace(".", "\\.").replace("*", ".*").replace("?", ".?").replace("\\", "\\\\");
        folderExlusionPatterns = new Pattern[exc.length];
        for (int i=0; i<exc.length; i++) folderExlusionPatterns[i] = Pattern.compile(exc[i]);
      }
    }

    /**
     * Initializer this filter with the specified set of file extensions.
     * @param exts a comma-separated list of file extensions to process.
     */
    public ReplacerFilter(final String[] exts) {
      this(exts, null);
    }

    /**
     * Tests whether or not the specified abstract pathname should be included in a pathname list.
     * @param file the abstract pathname to be tested
     * @return true if and only if pathname  should be included.
     */
    @Override
    public boolean accept(final File file) {
      if (file.isDirectory()) {
        if (folderExlusionPatterns != null) {
          final String name = file.getPath();
          for (final Pattern p: folderExlusionPatterns) {
            if (p.matcher(name).find()) return false;
          }
        }
        return true;
      }
      final String ext = FileUtils.getFileExtension(file);
      if (ext == null) return false;
      for (final String s: extensions) {
        if (ext.equalsIgnoreCase(s)) return true;
      }
      return false;
    }
  }
}
