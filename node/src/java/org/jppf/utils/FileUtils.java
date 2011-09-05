/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
import java.net.*;
import java.util.*;

import org.jppf.utils.streams.JPPFByteArrayOutputStream;
import org.slf4j.Logger;

/**
 * This class provides a set of utility methods for reading, writing and manipulating files. 
 * @author Laurent Cohen
 */
public final class FileUtils
{
	/**
	 * Maximum buffer size for reading class files.
	 */
	private static final int TEMP_BUFFER_SIZE = 32*1024;

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
		BufferedReader reader = (aReader instanceof BufferedReader) ? (BufferedReader) aReader : new BufferedReader(aReader);
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
	 * Read the content of a specified reader into a string.
	 * @param aReader the reader to read the vcontent from.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
	public static List<String> textFileAsLines(Reader aReader) throws IOException
	{
		List<String> lines = new ArrayList<String>();
		BufferedReader reader = (aReader instanceof BufferedReader) ? (BufferedReader) aReader : new BufferedReader(aReader);
		String s = "";
		while (s != null)
		{
			s = reader.readLine();
			if (s != null) lines.add(s);
		}
		return lines;
	}

	/**
	 * Read the content of a specified file into a string.
	 * @param file the file to read.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
	public static String readTextFile(File file) throws IOException
	{
		Reader reader = new FileReader(file);
		String result = readTextFile(reader);
		reader.close();
		return result;
	}

	/**
	 * Read the content of a specified file into a string.
	 * @param filename the location of the file to read.
	 * @return the content of the file as a string.
	 * @throws IOException if the file can't be found or read.
	 */
	public static String readTextFile(String filename) throws IOException
	{
		Reader reader = null;
		File f = new File(filename);
		if (f.exists()) reader = new FileReader(filename);
		else
		{
			InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(filename);
			if (is == null) return null;
			reader = new InputStreamReader(is);
		}
		String result = readTextFile(reader);
		reader.close();
		return result;
	}

	/**
	 * Write the content of a string into a specified file.
	 * @param filename the location of the file to write to.
	 * @param content the content to wwrite into the file.
	 * @throws IOException if the file can't be found or read.
	 */
	public static void writeTextFile(String filename, String content) throws IOException
	{
		writeTextFile(new FileWriter(filename), content);
	}

	/**
	 * Write the content of a string into a specified file.
	 * @param file the location of the file to write to.
	 * @param content the content to wwrite into the file.
	 * @throws IOException if the file can't be found or read.
	 */
	public static void writeTextFile(File file, String content) throws IOException
	{
		writeTextFile(new FileWriter(file), content);
	}

	/**
	 * Write the content of a string into a specified file.
	 * @param dest the file to write to.
	 * @param content the content to wwrite into the file.
	 * @throws IOException if the file can't be found or read.
	 */
	public static void writeTextFile(Writer dest, String content) throws IOException
	{
		BufferedReader reader = new BufferedReader(new StringReader(content));
		Writer writer = (dest instanceof BufferedWriter) ? dest : new BufferedWriter(dest);
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
	 * Get an input stream given a file path.
	 * @param path the path to the file to lookup.
	 * @return a <code>InputStream</code> instance, or null if the file could not be found.
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
	 * Get an output stream given a file path.
	 * @param path the path to the file to lookup.
	 * @return an <code>OutputStream</code> instance, or null if the file could not be created.
	 * @throws IOException if an IO error occurs while looking up the file.
	 */
	public static OutputStream getFileOutputStream(String path) throws IOException
	{
		return new BufferedOutputStream(new FileOutputStream(path));
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

	/**
	 * Get the name of a file from its full path.
	 * @param filePath the file from which to get the file name.
	 * @return the file name without path information.
	 */
	public static String getFileName(String filePath)
	{
		int idx = getLastFileSeparatorPosition(filePath);
		return idx >= 0 ? filePath.substring(idx + 1) : filePath;
	}

	/**
	 * Get the parent folder of a file or directory from its full path.
	 * @param filePath the path from which to get the parent path.
	 * @return the parent folder path.
	 */
	public static String getParentFolder(String filePath)
	{
		int idx = getLastFileSeparatorPosition(filePath);
		return idx >= 0 ? filePath.substring(0, idx) : filePath;
	}

	/**
	 * Get the last position of a file separator in a file path.
	 * @param path the path to parse.
	 * @return the position as an positive integer, or -1 if no separator was found.
	 */
	private static int getLastFileSeparatorPosition(String path)
	{
		int idx1 = path.lastIndexOf("/");
		int idx2 = path.lastIndexOf("\\");
		if ((idx1 < 0) && (idx2 < 0)) return -1;
		int idx = idx1 < 0 ? idx2 : idx2 < 0 ? idx1 : Math.max(idx1, idx2);
		return idx;
	}

	/**
	 * Split a file into multiple files whose size is as close as possible to the specified split size.
	 * @param file the etxt file to split.
	 * @param splitSize the maximum number of lines of each resulting file.
	 * @throws IOException if an IO error occurs.
	 */
	public static void splitTextFile(String file, int splitSize) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(file));
		StringBuilder sb = new StringBuilder();
		String s = "";
		int count = 0;
		while (s != null)
		{
			s = reader.readLine();
			if (s == null) break;
			sb.append(s).append("\n");
			if (sb.length() >= splitSize)
			{
				count++;
				writeTextFile(file +"." + count, sb.toString());
				sb = new StringBuilder();
			}
		}
		if (sb.length() > 0)
		{
			count++;
			writeTextFile(file +"." + count, sb.toString());
		}
		reader.close();
	}

	/**
	 * Entry point for the splitTextFile() method.
	 * @param args contains the arguments for the splitTextFile() method.
	 */
	public static void main(String...args)
	{
		try
		{
			int size = Integer.valueOf(args[1]).intValue();
			splitTextFile(args[0], size);
			System.out.println("Done");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Get the content of a file as an array of bytes.
	 * @param path the path of the file to read from as a string.
	 * @return a byte array.
	 * @throws IOException if an IO error occurs.
	 */
	public static byte[] getFileAsByte(String path) throws IOException
	{
		return getFileAsByte(new File(path));
	}

	/**
	 * Get the content of a file as an array of bytes.
	 * @param file the abstract path of the file to read from.
	 * @return a byte array.
	 * @throws IOException if an IO error occurs.
	 */
	public static byte[] getFileAsByte(File file) throws IOException
	{
		InputStream is = new BufferedInputStream(new FileInputStream(file));
		byte[] data = getInputStreamAsByte(is);
		is.close();
		return data;
	}

	/**
	 * Get the content of an input stream as an array of bytes.
	 * @param is the input stream to read from.
	 * @return a byte array.
	 * @throws IOException if an IO error occurs.
	 */
	public static byte[] getInputStreamAsByte(InputStream is) throws IOException
	{
		byte[] buffer = new byte[TEMP_BUFFER_SIZE];
		byte[] result = null;
		ByteArrayOutputStream baos = new JPPFByteArrayOutputStream();
		boolean end = false;
		while (!end)
		{
			int n = is.read(buffer, 0, buffer.length);
			if (n < 0) end = true;
			else baos.write(buffer, 0, n);
		}
		is.close();
		baos.flush();
		result = baos.toByteArray();
		baos.close();
		return result;
	}

	/**
	 * Copy the data read from the specified input stream to the specified output stream. 
	 * @param is the input stream to read from.
	 * @param os the output stream to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void copyStream(InputStream is, OutputStream os) throws IOException
	{
		byte[] bytes = new byte[TEMP_BUFFER_SIZE];
		while(true)
		{
			int n = is.read(bytes);
			if (n <= 0) break;
			os.write(bytes, 0, n);
		}
	}

	/**
	 * Convert a set of file names into a set of <code>File</code> objects.
	 * @param dir the directory in which the files are located
	 * @param names the name part of each file (not the full path)
	 * @return an array of <code>File</code> objects.
	 */
	public static File[] toFiles(File dir, String...names)
	{
		int len = names.length;
		File[] files = new File[len];
		for (int i=0; i<len; i++) files[i] = new File(dir.getPath(), names[i]);
		return files;
	}

	/**
	 * Convert a set of file paths into a set of URLs.
	 * @param files the files whose path is to be converted to a URL.
	 * @return an array of <code>URL</code> objects.
	 */
	public static URL[] toURLs(File...files)
	{
		URL[] urls = new URL[files.length];
		for (int i=0; i<files.length; i++)
		{
			try
			{
				urls[i] = files[i].toURI().toURL();
			}
			catch(MalformedURLException ignored)
			{
			}
		}
		return urls;
	}

	/**
	 * Write a byte array into an output stream.
	 * @param data the byte array to write.
	 * @param os the output stream to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void writeBytesToStream(byte[] data, OutputStream os) throws IOException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		copyStream(bais, os);
		bais.close();
	}

	/**
	 * Write a byte array into an file.
	 * @param data the byte array to write.
	 * @param path the path to the file to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void writeBytesToFile(byte[] data, String path) throws IOException
	{
		writeBytesToFile(data, new File(path));
	}

	/**
	 * Write a byte array into an file.
	 * @param data the byte array to write.
	 * @param path the path to the file to write to.
	 * @throws IOException if an I/O error occurs.
	 */
	public static void writeBytesToFile(byte[] data, File path) throws IOException
	{
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
		copyStream(bais, os);
		bais.close();
		os.flush();
		os.close();
	}

	/**
	 * Attempt to close the specified input stream and log any eventual error.
	 * @param is the input stream to close.
	 * @param log the logger to use; if null no logging occurs.
	 */
	public static void closeInputStream(InputStream is, Logger log)
	{
		if (is != null)
		{
			try
			{
				is.close();
			}
			catch (Exception e)
			{
				if (log != null)
				{
					if (log.isDebugEnabled()) log.debug("unable to close input stream", e);
					else log.warn("unable to close input stream: " + e.getClass().getName() + ": " + e.getMessage());
				}
			}
		}
	}

	/**
	 * Delete the specified path, recursively if this is a directory.
	 * @param path the path to delete.
	 * @return true if the folder and all contained files and subfolders were deleted, false otherwise.
	 */
	public static boolean deletePath(File path)
	{
		if ((path == null) || !path.exists()) return false;
		boolean success = true;
		try
		{
			if (path.isDirectory())
			{
				for (File child: path.listFiles()) success &= deletePath(child);
			}
			success &= path.delete();
		}
		catch (Exception e)
		{
			success = false;
		}
		return success;
	}
}
