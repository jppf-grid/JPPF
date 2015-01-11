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

import static org.jppf.doc.ParameterNames.*;

import java.io.*;
import java.util.*;

import org.jppf.utils.FileUtils;

/**
 * This class generates an HTML doc based on HTML templates and text files for the content.<br><br>
 * The goal is to programmatically realize something like a client-side include for HTML documents.
 * The templates are place holders inserted within the HTML code. They can hold parameters to make
 * them more generic and reusable.<br>
 * A template can be nested within another template.<br><br>
 *
 * Rationale: HTML docs often contain more code for the visual rendering of the content than
 * for the content itself. For example, using rounded tables holding the actual content implies a
 * lot of additional table constructs and ends up cluttering the code. This makes the documentation
 * difficult to maintain.<br>
 * This implementation provides a means to leave the additional rendering constructs
 * in separate files, with the double benefit of making the doc easier to author, read and maintain,
 * and making it possible to reuse the visual effects easily.
 * @author Laurent Cohen
 */
public class HtmlDocGenerator
{
  /**
   * Start of a template insertion.
   */
  public static final String TEMPLATE_START = "$template{";
  /**
   * End of a template insertion.
   */
  public static final String TEMPLATE_END = "}$";
  /**
   * Start of a content parameter value.
   */
  public static final String CONTENT_START = "$CONTENT[";
  /**
   * End of a content parameter value.
   */
  public static final String CONTENT_END = "]CONTENT$";
  /**
   * Separator for parameter name/value pair.
   */
  public static final String EQUALS = "=";
  /**
   * Enclosing sequence for parameter values.
   */
  public static final String QUOTE = "\"";
  /**
   * Start of a template line comment.
   */
  public static final String COMMENT = "#";
  /**
   * Start of a template parameter placeholder.
   */
  public static final String PARAM_START = "${";
  /**
   * End of a template parameter placeholder.
   */
  public static final String PARAM_END = "}";

  /**
   * Generate a target HTML document for a document source, using a specified location for the
   * templates to use.
   * @param source the source document.
   * @param target the target HTML document.
   * @param templateFolder the location of the templates.
   * @throws Exception if any error occurs while reading, parsing or writing any of the files.
   */
  public void generatePage(final String source, final String target, final String templateFolder) throws Exception
  {
    System.out.println("Processing source file " + source);
    String s = FileUtils.readTextFile(source);
    s = processTemplates(new HashMap<String, String>(), s, templateFolder);
    File targetFile = new File(target);
    FileUtils.mkdirs(targetFile);
    FileUtils.writeTextFile(target, s);
  }

  /**
   * Generate an instance of a template, to be inserted in an enclosing document.
   * @param parameterMap a map of parameters key/values for the template.
   * @param content the template's actual content.
   * @param templateFolder location of the templates root folder.
   * @return a string containing an instance of the template, where all placeholders have
   * been replaced with parameter values.
   * @throws Exception if an error occurs while parsing the template or building its instance.
   */
  private String processTemplates(final Map<String, String> parameterMap, final String content, final String templateFolder)
  throws Exception
  {
    StringBuilder sb = new StringBuilder();
    boolean end = false;
    int pos = 0;
    while (!end && (pos >= 0) && (pos < content.length()))
    {
      int index = content.indexOf(TEMPLATE_START, pos);
      if (index <0)
      {
        if (pos < content.length()) sb.append(content.substring(pos));
        break;
      }
      sb.append(content.substring(pos, index));
      pos = index;
      index = content.indexOf(TEMPLATE_END, pos+TEMPLATE_START.length());
      if (index >= 0)
      {
        String templateCall = content.substring(pos, index + TEMPLATE_END.length());
        sb.append(processTemplateCall(templateCall, parameterMap, templateFolder));
        pos = index + TEMPLATE_END.length();
      }
    }

    return processParameters(parameterMap, sb.toString());
  }

  /**
   * Replace a template call with actual HTML content.
   * @param templateCall the string representing the template invocation, with the format:<br>
   * &nbsp;&nbsp;<code>$template{name="some name" param1="value1" ... paramN="valueN"}$</code>
   * @param callerMap map of parameter/value entries gathered from the calling document.
   * @param templateFolder location of the templates root folder.
   * @return an HTML fragment string where all template calls and place holders have been
   * replaced with actual content.
   * @throws Exception if an error occurs while parsing the template.
   */
  private String processTemplateCall(final String templateCall, final Map<String, String> callerMap, final String templateFolder) throws Exception
  {
    int pos = TEMPLATE_START.length();
    int index = pos;
    Map<String, String> parameterMap = new HashMap<>();
    while (index > 0)
    {
      index = templateCall.indexOf(EQUALS, pos);
      if (index >= 0)
      {
        String paramName = templateCall.substring(pos, index).trim();
        String paramValue = "";
        index = templateCall.indexOf(QUOTE, pos);
        if (index < 0) throw new Exception("Missing opening quote for parameter '"+paramName+ '\'');
        pos = index+QUOTE.length();
        String sub = templateCall.substring(pos);
        if (sub.startsWith(CONTENT_START))
        {
          pos += CONTENT_START.length();
          index = templateCall.indexOf(CONTENT_END+QUOTE, pos);
          if (index < 0) throw new Exception("Missing closing content string for parameter '"+paramName+"' in template call: '"
              + templateCall + '\'');
          paramValue = templateCall.substring(pos, index).trim();
          paramValue = processParameters(callerMap, paramValue);
          pos = index + (CONTENT_END+QUOTE).length();
        }
        else
        {
          pos = index + QUOTE.length();
          index = templateCall.indexOf(QUOTE, pos);
          if (index < 0) throw new Exception("Missing closing quote for parameter '"+paramName+ '\'');
          paramValue = templateCall.substring(pos, index).trim();
          pos = index + QUOTE.length();
        }
        parameterMap.put(paramName, paramValue);
      }
      if (templateCall.substring(pos).startsWith(TEMPLATE_END))
      {
        pos += TEMPLATE_END.length();
        break;
      }
    }
    String tf = !templateFolder.endsWith("/") ? templateFolder + "/" : templateFolder;
    String templateFile = tf + parameterMap.get("name") + ".html";
    if (!(new File(templateFile).exists())) throw new Exception("Could not find template file " + templateFile);
    //String content = FileUtils.readTextFile(templateFile);
    String content = readTextFileStripComments(templateFile);
    content = processTemplates(parameterMap, content, tf);
    content = processParameters(parameterMap, content);
    return content;
  }

  /**
   * Process a template by processing all nested template calls and substituting parameters with actual values.
   * @param parameterMap a map of parameters key/values for the template.
   * @param content the template content.
   * @return a string containing an instance of the template, where all placeholders have
   * been replaced with parameter values, and all nested template calls with corresponding instances.
   * @throws Exception if an error occurs while parsing the template or building its instance.
   */
  private static String processParameters(final Map<String, String> parameterMap, final String content) throws Exception
  {
    String template = readTextStripComments(new StringReader(content));
    for (Map.Entry<String, String> entry: parameterMap.entrySet())
    {
      String param = PARAM_START + entry.getKey() + PARAM_END;
      template = template.replace(param, entry.getValue());
    }
    return template;
  }

  /**
   * Read a text file into a string and strip all the comment lines it contains.
   * @param path the path to the text file to read.
   * @return a string holding the file content, from which all comments have been stripped.
   * @throws Exception if an error occurs while parsing the template or building its instance.
   */
  private static String readTextFileStripComments(final String path) throws Exception
  {
    return readTextStripComments(new FileReader(path));
  }

  /**
   * Read a text content from a <code>Reader</code> into a string and strip all the comment lines it contains.
   * @param reader the from which to read the text content.
   * @return a string holding the file content, from which all comments have been stripped.
   * @throws Exception if an error occurs while parsing the template or building its instance.
   */
  private static String readTextStripComments(final Reader reader) throws Exception
  {
    BufferedReader bufferedReader = new BufferedReader(reader);
    StringBuilder sb = new StringBuilder();
    try
    {
      String s = "";
      while (s != null)
      {
        s = bufferedReader.readLine();
        if (s == null) break;
        String s2 = s.trim();
        if ("".equals(s2) || s2.startsWith(COMMENT)) continue;
        sb.append(s).append('\n');
      }
    }
    finally
    {
      bufferedReader.close();
    }
    return sb.toString();
  }

  /**
   * Test this class.
   * @param args the options to use.
   */
  public static void main(final String...args)
  {
    try
    {
      Map<ParameterNames, Object> parameters = new ParametersHandler().parseArguments(args);
      File sourceDir = new File((String) parameters.get(SOURCE_DIR));
      if (!sourceDir.exists() || !sourceDir.isDirectory()) showUsageAndExit("Source location must be an existing folder");
      File destDir = new File((String) parameters.get(DEST_DIR));
      if (!destDir.exists() || !destDir.isDirectory()) showUsageAndExit("Target location must be an existing folder");
      File templateDir = new File((String) parameters.get(TEMPLATES_DIR));
      if (!templateDir.exists() || !templateDir.isDirectory()) showUsageAndExit("Templates location must be an existing folder");

      boolean recursive = (Boolean) parameters.get(RECURSIVE);
      if (recursive) generateDocRecursive(sourceDir, destDir, templateDir, parameters);
      else generateDoc(sourceDir, destDir, templateDir, parameters);
    }
    catch(Exception e)
    {
      //e.printStackTrace();
      showUsageAndExit(e.getMessage());
    }
  }

  /**
   * Generate the documentation recursively.
   * @param sourceDir source folder.
   * @param destDir target folder.
   * @param templateDir templates folder.
   * @param parameters the options to use.
   * @throws Exception if any error occurs.
   */
  private static void generateDocRecursive(final File sourceDir, final File destDir, final File templateDir, final Map<ParameterNames, Object> parameters) throws Exception
  {
    generateDoc(sourceDir, destDir, templateDir, parameters);
    List<File> allSourceDirs = new ArrayList<>();
    allDirsRecursive(sourceDir, allSourceDirs, parameters);
    String rootSourceName = sourceDir.getCanonicalPath();
    String rootTargetName = destDir.getCanonicalPath();
    for (File source: allSourceDirs)
    {
      String s = source.getCanonicalPath().substring(rootSourceName.length());
      File target = new File(rootTargetName + s);
      generateDoc(source, target, templateDir, parameters);
    }
  }

  /**
   * Generate the documentation recursively.
   * @param sourceDir source folder.
   * @param destDir target folder.
   * @param templateDir templates folder.
   * @param parameters the options to use.
   * @throws Exception if any error occurs.
   */
  private static void generateDoc(final File sourceDir, final File destDir, final File templateDir, final Map<ParameterNames, Object> parameters) throws Exception
  {
    HtmlDocGenerator docGen = new HtmlDocGenerator();
    for (File file: sourceDir.listFiles(new JPPFFileFilter((String[]) parameters.get(FILE_INCLUDES), (String[]) parameters.get(FILE_EXCLUDES))))
    {
      FileUtils.mkdirs(destDir);
      String target = destDir.getPath();
      if (!target.endsWith("/") && !target.endsWith("\\")) target += '/';
      target += file.getName();
      docGen.generatePage(file.getPath(), target, templateDir.getPath());
    }
  }

  /**
   * Get recursively all directories under the specified root, and add them to the specified list.
   * @param root the root directory to search from.
   * @param list the list to add directories to.
   * @param parameters the options to use.
   * @throws Exception if any error occurs.
   */
  private static void allDirsRecursive(final File root, final List<File> list, final Map<ParameterNames, Object> parameters) throws Exception
  {
    for (File file: root.listFiles(new JPPFDirFilter((String[]) parameters.get(DIR_INCLUDES), (String[]) parameters.get(DIR_EXCLUDES))))
    {
      list.add(file);
      allDirsRecursive(file, list, parameters);
    }
  }

  /**
   * Give a brief explanation of the command-line parameters.
   * @param msg text to display before usage text.
   */
  private static void showUsageAndExit(final String msg)
  {
    System.err.println(msg);
    System.err.println("HtmlDocGenerator usage: java " + HtmlDocGenerator.class.getName() + " -s sourceDir -d destDir -t templatesDir");
    System.err.println("  [[-r] [-fi includedFiles] [-fe excludedFiles] [-di includedDirs] [-de excludedDirs]]");
    System.err.println("where:");
    System.err.println("-s  sourceDir is the location of the root folder with the documents sources");
    System.err.println("-d  destDir is the root folder where the converted documents are created");
    System.err.println("-t  templatesDir is the location of the root folder where the templates are");
    System.err.println("-r  specifies whether the source directory should be processed recursively");
    System.err.println("-fi specifies extensions of the files to include");
    System.err.println("    if left unspecified, default 'html, htm, php' are included");
    System.err.println("-fe specifies extensions of the files to exclude");
    System.err.println("    if left unspecified none are excluded");
    System.err.println("-di specifies the names of the directories to include");
    System.err.println("    if left unspecified all are included");
    System.err.println("-de specifies names of the directories to exclude");
    System.err.println("    if left unspecified, default 'CVS, .svn' are excluded");
    System.err.println();
    System.exit(1);
  }
}
