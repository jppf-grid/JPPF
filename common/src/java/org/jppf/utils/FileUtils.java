/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.utils;

import java.io.*;
import java.net.URL;
import java.util.*;

/**
 * This class provides a set of utility methods for reading, writing and manipulating files. 
 * @author Laurent Cohen
 */
public final class FileUtils
{
	/**
	 * Instantiation of this class is not permitted.
	 */
	private FileUtils()
	{
	}

	/**
	 * Read the content of a specified reader into a string.
	 * @param aReader the reader to read the vcontent from.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
	public static String readTextFile(Reader aReader) throws IOException
	{
		LineNumberReader reader = new LineNumberReader(aReader);
		StringBuilder sb = new StringBuilder();
		String s = "";
		while (s != null)
		{
			s = reader.readLine();
			if (s != null) sb.append(s).append("\n");
		}
		return sb.toString();
	}

	/**
	 * Read the content of a specified file into a string.
	 * @param filename the location of the file to read.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
	public static String readTextFile(String filename) throws IOException
	{
		return readTextFile(new FileReader(filename));
	}

	/**
	 * Write the content of a string into a specified file.
	 * @param filename the location of the file to write to.
	 * @param content the content to wwrite into the file.
	 * @throws IOException if the file can't be found or read.
	 */
	public static void writeTextFile(String filename, String content) throws IOException
	{
		LineNumberReader reader = new LineNumberReader(new StringReader(content));
		Writer writer = new BufferedWriter(new FileWriter(filename));
		String s = "";
		while (s != null)
		{
			s = reader.readLine();
			if (s != null)
			{
				writer.write(s);
				writer.write("\n");
			}
		}
		writer.flush();
		writer.close();
	}

	/**
	 * Load a file from the specified path.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param path the path to the file to load.
	 * @return a <code>InputStream</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
	public static InputStream findFile(String path) throws IOException
	{
		InputStream is = null;
		File file = new File(path);
		if (file.exists()) is = new FileInputStream(file);
		if (is == null)
		{
			URL url = FileUtils.class.getClassLoader().getResource(path);
			is = (url == null) ? null : url.openStream();
		}
		return is;
	}

	/**
	 * Get a list of files whose paths are found in a text file.
	 * @param fileListPath the path to the file that holds the list of documents to validate.
	 * @return the file paths as a lst of strings.
	 * @throws IOException if an error occurs while looking up or reading the file.
	 */
	public static List<String> getFilePathList(String fileListPath) throws IOException
	{
		InputStream is = findFile(fileListPath);
		String content = readTextFile(new BufferedReader(new InputStreamReader(is)));
		LineNumberReader reader = new LineNumberReader(new StringReader(content));
		List<String> filePaths = new ArrayList<String>();
		boolean end = false;
		while (!end)
		{
			String s = reader.readLine();
			if (s != null) filePaths.add(s);
			else end = true;
		}
		return filePaths;
	}
}
