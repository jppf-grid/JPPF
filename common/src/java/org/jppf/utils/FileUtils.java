/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
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
	public static InputStream getFileInputStream(String path) throws IOException
	{
		InputStream is = null;
		File file = new File(path);
		if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
		if (is == null)
		{
			URL url = FileUtils.class.getClassLoader().getResource(path);
			is = (url == null) ? null : url.openStream();
		}
		return is;
	}

	/**
	 * Load a file from the specified path.
	 * This method looks up the schema first in the file system, then in the classpath
	 * if it is not found in the file system.
	 * @param path the path to the file to load.
	 * @return a <code>Reader</code> instance, or null if the schema file could not be found.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
	public static Reader getFileReader(String path) throws IOException
	{
		InputStream is = getFileInputStream(path);
		if (is == null) return null;
		return new InputStreamReader(is);
	}

	/**
	 * Get a list of files whose paths are found in a text file.
	 * @param fileListPath the path to the file that holds the list of documents to validate.
	 * @return the file paths as a lst of strings.
	 * @throws IOException if an error occurs while looking up or reading the file.
	 */
	public static List<String> getFilePathList(String fileListPath) throws IOException
	{
		InputStream is = getFileInputStream(fileListPath);
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

	/**
	 * Get the extension of a file.
	 * @param filePath the file from which to get the extension.
	 * @return the file extension, or null if it si not a file or does not have an extension.
	 */
	public static String getFileExtension(String filePath)
	{
		return getFileExtension(new File(filePath));
	}

	/**
	 * Get the extension of a file.
	 * @param file the file from which to get the extension.
	 * @return the file extension, or null if it si not a file or does not have an extension.
	 */
	public static String getFileExtension(File file)
	{
		if ((file == null) || !file.exists() || !file.isFile()) return null;
		String filePath = file.getPath();
		int idx = filePath.lastIndexOf(".");
		if (idx >=0) return filePath.substring(idx+1);
		return null;
	}
}
