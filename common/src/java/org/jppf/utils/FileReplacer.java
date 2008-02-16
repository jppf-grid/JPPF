/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
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

import org.apache.commons.logging.*;

/**
 * Utility to replace multiline content in text files.
 * This is the same kind of utility as the Ant "replace" task, except that it works.
 * @author Laurent Cohen
 */
public class FileReplacer
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(FileReplacer.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
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
	private ReplacerFiler filter = null;
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
	 * @throws Exception if an error occurs while performing the replacements.
	 */
	public void replace(String rootDir, String srcFile, String destFile, String ext, boolean searchOnly) throws Exception
	{
		src = FileUtils.readTextFile(srcFile);
		dest = FileUtils.readTextFile(destFile);
		if (src.endsWith("\n") && dest.endsWith("\n"))
		{
			src = src.substring(0, src.length() - 1);
			dest = dest.substring(0, dest.length() - 1);
		}
		this.searchOnly = searchOnly;
		pattern = Pattern.compile(src, Pattern.LITERAL);
		filter = new ReplacerFiler(ext);
		File f = new File(rootDir);
		nbFilesChanged = 0;
		nbReplacements = 0;
		if (f.isDirectory()) replaceFolder(f);
		else replaceFile(f);
		log.info("Total number of occurences found: " + nbReplacements);
		log.info("Total number of files" + (searchOnly ? " would have been" : "") +
			" changed: " + nbFilesChanged);
	}

	/**
	 * Recursively process a folder.
	 * @param folder the process to visit.
	 * @throws Exception if an eeror occurs while processing the folder.
	 */
	private void replaceFolder(File folder) throws Exception
	{
		if (debugEnabled) log.info("Processing folder " + folder.getAbsolutePath());
		File[] fileList = folder.listFiles(filter);
		List<File> folders = new ArrayList<File>();
		List<File> files = new ArrayList<File>();
		for (File f: fileList)
		{
			if (f.isDirectory()) folders.add(f);
			else files.add(f);
		}
		for (File f: files) replaceFile(f);
		for (File f: folders) replaceFolder(f);
	}


	/**
	 * Recursively process a single file.
	 * @param file the file to process.
	 * @throws Exception if an eeror occurs while processing the folder.
	 */
	private void replaceFile(File file) throws Exception
	{
		String content = FileUtils.readTextFile(file.getPath());
		Matcher matcher = pattern.matcher(content);
		boolean b = true;
		int nbFound = 0;
		int start = 0;
		while (b)
		{
			b = matcher.find(start);
			if (b)
			{
				nbFound++;
				start = matcher.end();
			}
		}
		if (nbFound > 0)
		{
			nbFilesChanged++;
			nbReplacements += nbFound;
			log.info("Found "+nbFound+" occurrence"+(nbFound > 1 ? "s" : "")+" of the sequence in file '"+file+"'");
			String s = matcher.replaceAll(dest);
			if (debugEnabled) log.debug("Content with replacements performed:\n" + s);
			if (!searchOnly) FileUtils.writeTextFile(file.getPath(), s);
		}
		else if (debugEnabled) log.debug("Sequence not found in file '"+file+"'");
	}

	/**
	 * Main entry point.
	 * @param args not used yet.
	 */
	public static void main(String...args)
	{
		try
		{
			String rootFolder = args[0];
			String find = args[1];
			String replace = args[2];
			String ext = args[3];
			boolean searchOnly = Boolean.valueOf(args[4]);
			FileReplacer replacer = new FileReplacer();
			replacer.replace(rootFolder, find, replace, ext, searchOnly); 
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * File filter based on a set of extensions.
	 */
	public static class ReplacerFiler implements FileFilter
	{
		/**
		 * The list of file extensions to process.
		 */
		private String[] extensions = null;

		/**
		 * Initializer this filter with the specified set of file extensions.
		 * @param ext a comma-separated list of file extensions to process.
		 */
		public ReplacerFiler(String ext)
		{
			if (ext == null) ext = "";
			extensions = ext.split(",");
			for (int i=0; i<extensions.length; i++) extensions[i] = extensions[i].trim();
		}

		/**
		 * Tests whether or not the specified abstract pathname should be included in a pathname list.
		 * @param file the abstract pathname to be tested
		 * @return true if and only if pathname  should be included.
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		public boolean accept(File file)
		{
			if (file.isDirectory()) return true;
			String ext = FileUtils.getFileExtension(file);
			if (ext == null) return false;
			for (String s: extensions)
			{
				if (ext.equalsIgnoreCase(s)) return true;
			}
			return false;
		}
	}
}
