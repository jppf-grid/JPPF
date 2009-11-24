/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
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
 * This class generates an HTML doc based on HTML templates and text files for the content.<br><br>
 * The goal is to programmatically realize something like a client-side include for HTML documents.
 * The templates are place holders inserted within the HTML code. They can hold parameters to make
 * them more generic and resusable.<br>
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
	 * Separator for parameter nane/value pair.
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
	public void generatePage(String source, String target, String templateFolder) throws Exception
	{
		String s = FileUtils.readTextFile(source);
		s = processTemplates(new HashMap<String, String>(), s, templateFolder);
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
	private String processTemplates(Map<String, String> parameterMap, String content, String templateFolder)
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
	private String processTemplateCall(String templateCall, Map<String, String> callerMap, String templateFolder)
		throws Exception
	{
		int pos = TEMPLATE_START.length();
		int index = pos;
		Map<String, String> parameterMap = new HashMap<String, String>();
		while (index > 0)
		{
			index = templateCall.indexOf(EQUALS, pos);
			if (index >= 0)
			{
				String paramName = templateCall.substring(pos, index).trim();
				String paramValue = "";
				index = templateCall.indexOf(QUOTE, pos);
				if (index < 0) throw new Exception("Missing opening quote for parameter '"+paramName+"'");
				pos = index+QUOTE.length();
				String sub = templateCall.substring(pos);
				if (sub.startsWith(CONTENT_START))
				{
					pos += CONTENT_START.length();
					index = templateCall.indexOf(CONTENT_END+QUOTE, pos);
					if (index < 0) throw new Exception("Missing closing content string for parameter '"+paramName+"' in template call: '"
							+ templateCall + "'");
					paramValue = templateCall.substring(pos, index).trim();
					paramValue = processParameters(callerMap, paramValue);
					pos = index + (CONTENT_END+QUOTE).length();
				}
				else
				{
					pos = index + QUOTE.length();
					index = templateCall.indexOf(QUOTE, pos);
					if (index < 0) throw new Exception("Missing closing quote for parameter '"+paramName+"'");
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
		if (!templateFolder.endsWith("/")) templateFolder += "/";
		String templateFile = templateFolder + parameterMap.get("name") + ".html";
		String content = FileUtils.readTextFile(templateFile);
		content = processTemplates(parameterMap, content, templateFolder);
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
	private String processParameters(Map<String, String> parameterMap, String content) throws Exception
	{
		LineNumberReader reader = new LineNumberReader(new StringReader(content));
		StringBuilder sb = new StringBuilder();
		String s = "";

		while (s != null)
		{
			s = reader.readLine();
			if (s == null) break;
			String s2 = s.trim();
			if ("".equals(s2) || s2.startsWith(COMMENT)) continue;
			sb.append(s).append("\n");
		}
		reader.close();
		String template = sb.toString();
		for (Map.Entry<String, String> entry: parameterMap.entrySet())
		{
			String param = PARAM_START + entry.getKey() + PARAM_END;
			template = template.replace(param, entry.getValue());
		}
		return template;
	}

	/**
	 * Test this class.
	 * @param args not used.
	 */
	public static void main(String...args)
	{
		try
		{
			if (args.length < 3) showUsageAndExit("Missing parameter(s).\n");
			if (args.length > 3) showUsageAndExit("Too many parameter(s).\n");
			File sourceDir = new File(args[0]);
			if (!sourceDir.exists() || !sourceDir.isDirectory())
				showUsageAndExit("Source location must be an existing folder");
			File targetDir = new File(args[1]);
			if (!targetDir.exists() || !targetDir.isDirectory())
				showUsageAndExit("Target location must be an existing folder");
			File templateDir = new File(args[2]);
			if (!templateDir.exists() || !templateDir.isDirectory())
				showUsageAndExit("Templates location must be an existing folder");

			FileFilter filter = new FileFilter()
			{
				public boolean accept(File pathname)
				{
					if (pathname.isDirectory()) return false;
					String s = pathname.getPath();
					int idx = s.lastIndexOf(".");
					if (idx < 0) return false;
					s = s.substring(idx).toLowerCase();
					return ".html".equals(s) || ".htm".equals(s) || ".php".equals(s);
				}
			};
			HtmlDocGenerator docGen = new HtmlDocGenerator();
			for (File file: sourceDir.listFiles(filter))
			{
				String target = targetDir.getPath();
				if (!target.endsWith("/") && !target.endsWith("\\")) target += "/";
				target += file.getName();
				docGen.generatePage(file.getPath(), target, templateDir.getPath());
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Give a brief explanation of the comand-line parameters.
	 * @param msg text to display before usage text.
	 */
	private static void showUsageAndExit(String msg)
	{
		System.err.println(msg);
		System.err.println("HtmlDocGenerator usage: java "+HtmlDocGenerator.class.getName()+" sourceDir"+" targetDir"+" templatesDir");
		System.err.println();
		System.err.println("Where:");
		System.err.println("- sourceDir is the location of the folder with the documents sources (those that use templates)");
		System.err.println("- targetDir is the location of the folder where the actual HTML documents are created");
		System.err.println("- templatesDir is the location of the root folder where the templates are");
		System.err.println();
		System.err.println("This tool only handles html documents, thus any other file has to be already in the right location,");
		System.err.println("including stylesheets, image files and others.");
		System.exit(0);
	}
}
