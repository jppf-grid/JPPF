/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.jppf.utils.streams.StreamUtils;

/**
 * This class provides a set of utility methods for reading, writing and manipulating files.
 * @author Laurent Cohen
 */
public final class FileUtils {
  /**
   * Instantiation of this class is not permitted.
   */
  private FileUtils() {
  }

  /**
   * Read the content of a specified reader into a string.
   * @param aReader the reader to read the content from.
   * @return the content of the file as a string.
   * @throws IOException if the file can't be found or read.
   */
  public static String readTextFile(final Reader aReader) throws IOException {
    BufferedReader reader = (aReader instanceof BufferedReader) ? (BufferedReader) aReader : new BufferedReader(aReader);
    StringBuilder sb = new StringBuilder();
    try {
      String s = "";
      while (s != null) {
        s = reader.readLine();
        if (s != null) sb.append(s).append('\n');
      }
    } finally {
      reader.close();
    }
    return sb.toString();
  }

  /**
   * Read the content of a specified reader into a string.
   * This method closes the reader upon terminating.
   * @param aReader the reader to read the content from.
   * @return the content of the file as a string.
   * @throws IOException if the file can't be found or read.
   */
  public static List<String> textFileAsLines(final Reader aReader) throws IOException {
    List<String> lines = new ArrayList<>();
    BufferedReader reader = (aReader instanceof BufferedReader) ? (BufferedReader) aReader : new BufferedReader(aReader);
    try {
      String s = "";
      while (s != null) {
        s = reader.readLine();
        if ((s != null) && !"".equals(s.trim())) lines.add(s);
      }
    } finally {
      reader.close();
    }
    return lines;
  }

  /**
   * Read the content of a specified file into a string.
   * @param file the file to read.
   * @return the content of the file as a string.
   * @throws IOException if the file can't be found or read.
   */
  public static String readTextFile(final File file) throws IOException {
    return readTextFile(new FileReader(file));
  }

  /**
   * Read the content of a specified file into a string.
   * @param filename the location of the file to read.
   * @return the content of the file as a string.
   * @throws IOException if the file can't be found or read.
   */
  public static String readTextFile(final String filename) throws IOException {
    Reader reader = null;
    File f = new File(filename);
    if (f.exists()) reader = new FileReader(filename);
    else {
      InputStream is = FileUtils.class.getClassLoader().getResourceAsStream(filename);
      if (is == null) return null;
      reader = new InputStreamReader(is);
    }
    return readTextFile(reader);
  }

  /**
   * Write the content of a string into a specified file.
   * @param filename the location of the file to write to.
   * @param content the content to write into the file.
   * @throws IOException if the file can't be found or read.
   */
  public static void writeTextFile(final String filename, final String content) throws IOException {
    writeTextFile(new FileWriter(filename), content);
  }

  /**
   * Write the content of a string into a specified file.
   * @param file the location of the file to write to.
   * @param content the content to write into the file.
   * @throws IOException if the file can't be found or read.
   */
  public static void writeTextFile(final File file, final String content) throws IOException {
    writeTextFile(new FileWriter(file), content);
  }

  /**
   * Write the content of a string into a specified file.
   * @param dest the file to write to.
   * @param content the content to write into the file.
   * @throws IOException if the file can't be found or read.
   */
  public static void writeTextFile(final Writer dest, final String content) throws IOException {
    BufferedReader reader = new BufferedReader(new StringReader(content));
    Writer writer = (dest instanceof BufferedWriter) ? dest : new BufferedWriter(dest);
    try {
      String s = "";
      while (s != null) {
        s = reader.readLine();
        if (s != null) {
          writer.write(s);
          writer.write("\n");
        }
      }
      writer.flush();
    } finally {
      writer.close();
    }
  }

  /**
   * Get an input stream given a file path.
   * This method first looks up in the file system for the specified path, then in the classpath.
   * @param path the path to the file to lookup.
   * @return a <code>InputStream</code> instance, or null if the file could not be found.
   * @throws IOException if an IO error occurs while looking up the file.
   */
  public static InputStream getFileInputStream(final String path) throws IOException {
    InputStream is = null;
    File file = new File(path);
    if (file.exists()) is = new BufferedInputStream(new FileInputStream(file));
    if (is == null) {
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) cl = FileUtils.class.getClassLoader();
      is = cl.getResourceAsStream(path);
    }
    return is;
  }

  /**
   * Get an output stream given a file path.
   * @param path the path to the file to lookup.
   * @return an <code>OutputStream</code> instance, or null if the file could not be created.
   * @throws IOException if an IO error occurs while looking up the file.
   */
  public static OutputStream getFileOutputStream(final String path) throws IOException {
    return new BufferedOutputStream(new FileOutputStream(path));
  }

  /**
   * Get an output stream given a file path.
   * @param file the file to lookup.
   * @return an <code>OutputStream</code> instance, or null if the file could not be created.
   * @throws IOException if an IO error occurs while looking up the file.
   */
  public static OutputStream getFileOutputStream(final File file) throws IOException {
    return new BufferedOutputStream(new FileOutputStream(file));
  }

  /**
   * Get a <code>Reader</code> for the specified file path.
   * @param path the path to the file to lookup.
   * @return a <code>Reader</code> instance, or null if the file could not be found.
   * @throws IOException if an IO error occurs while looking up the file.
   */
  public static Reader getFileReader(final String path) throws IOException {
    InputStream is = getFileInputStream(path);
    if (is == null) return null;
    return new InputStreamReader(is);
  }

  /**
   * Get a <code>Writer</code> for the specified file path.
   * @param path the path to the file to create or open.
   * @return a <code>Writer</code> instance.
   * @throws IOException if an IO error occurs while creating the file writer.
   */
  public static Writer getFileWriter(final String path) throws IOException {
    return new BufferedWriter(new FileWriter(path));
  }

  /**
   * Get a list of files whose paths are found in a text file.
   * @param fileListPath the path to the file that holds the list of documents to validate.
   * @return the file paths as a lst of strings.
   * @throws IOException if an error occurs while looking up or reading the file.
   */
  public static List<String> getFilePathList(final String fileListPath) throws IOException {
    InputStream is = getFileInputStream(fileListPath);
    String content = readTextFile(new BufferedReader(new InputStreamReader(is)));
    BufferedReader reader = new BufferedReader(new StringReader(content));
    List<String> filePaths = new ArrayList<>();
    try {
      boolean end = false;
      while (!end) {
        String s = reader.readLine();
        if (s != null) filePaths.add(s);
        else end = true;
      }
    } finally {
      reader.close();
    }
    return filePaths;
  }

  /**
   * Get the extension of a file.
   * @param filePath the file from which to get the extension.
   * @return the file extension, or null if it si not a file or does not have an extension.
   */
  public static String getFileExtension(final String filePath) {
    return getFileExtension(new File(filePath));
  }

  /**
   * Get the extension of a file.
   * @param file the file from which to get the extension.
   * @return the file extension, or null if it si not a file or does not have an extension.
   */
  public static String getFileExtension(final File file) {
    if ((file == null) || !file.exists() || !file.isFile()) return null;
    String filePath = null;
    try {
      filePath = file.getCanonicalPath();
    } catch(IOException e) {
      return null;
    }
    int idx = filePath.lastIndexOf('.');
    if (idx >=0) return filePath.substring(idx+1);
    return null;
  }

  /**
   * Get the name of a file from its full path.
   * @param filePath the file from which to get the file name.
   * @return the file name without path information.
   */
  public static String getFileName(final String filePath) {
    int idx = getLastFileSeparatorPosition(filePath);
    return idx >= 0 ? filePath.substring(idx + 1) : filePath;
  }

  /**
   * Get the parent folder of a file or directory from its full path.
   * @param filePath the path from which to get the parent path.
   * @return the parent folder path.
   */
  public static String getParentFolder(final String filePath) {
    int idx = getLastFileSeparatorPosition(filePath);
    return idx >= 0 ? filePath.substring(0, idx) : filePath;
  }

  /**
   * Get the last position of a file separator in a file path.
   * @param path the path to parse.
   * @return the position as an positive integer, or -1 if no separator was found.
   */
  private static int getLastFileSeparatorPosition(final String path) {
    int idx1 = path.lastIndexOf('/');
    int idx2 = path.lastIndexOf('\\');
    if ((idx1 < 0) && (idx2 < 0)) return -1;
    return idx1 < 0 ? idx2 : (idx2 < 0 ? idx1 : Math.max(idx1, idx2));
  }

  /**
   * Split a file into multiple files whose size is as close as possible to the specified split size.
   * @param file the text file to split.
   * @param splitSize the maximum number of lines of each resulting file.
   * @throws IOException if an IO error occurs.
   */
  public static void splitTextFile(final String file, final int splitSize) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    BufferedWriter writer = null;
    int count = 0;
    int size = 0;
    try {
      String s = "";
      while (s != null) {
        if (writer == null) {
          String name = file + '.' + count;
          System.out.println("creating file " + name);
          writer = new BufferedWriter(new FileWriter(name));
        }
        s = reader.readLine();
        if (s == null) break;
        writer.write(s + "\n");
        size += s.length();
        if (size >= splitSize) {
          writer.close();
          writer = null;
          count++;
          size = 0;
        }
      }
      if (writer != null) writer.close();
    } finally {
      reader.close();
    }
  }

  /**
   * Entry point for the splitTextFile() method.
   * @param args contains the arguments for the splitTextFile() method.
   */
  public static void main(final String...args) {
    try {
      int size = Integer.valueOf(args[1]).intValue();
      splitTextFile(args[0], size);
      System.out.println("Done");
    } catch(Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Get the content of a file or resource on the classpath as an array of bytes.
   * @param path the path of the file to read from as a string.
   * @return a byte array with the file content.
   * @throws IOException if an IO error occurs.
   */
  public static byte[] getPathAsByte(final String path) throws IOException {
    return StreamUtils.getInputStreamAsByte(getFileInputStream(path));
  }

  /**
   * Get the content of a file as an array of bytes.
   * @param path the path of the file to read from as a string.
   * @return a byte array with the file content.
   * @throws IOException if an IO error occurs.
   */
  public static byte[] getFileAsByte(final String path) throws IOException {
    return getFileAsByte(new File(path));
  }

  /**
   * Get the content of a file as an array of bytes.
   * @param file the abstract path of the file to read from.
   * @return a byte array with the file content.
   * @throws IOException if an IO error occurs.
   */
  public static byte[] getFileAsByte(final File file) throws IOException {
    InputStream is = new BufferedInputStream(new FileInputStream(file));
    byte[] data = StreamUtils.getInputStreamAsByte(is);
    return data;
  }

  /**
   * Convert a set of file names into a set of <code>File</code> objects.
   * @param dir the directory in which the files are located
   * @param names the name part of each file (not the full path)
   * @return an array of <code>File</code> objects.
   */
  public static File[] toFiles(final File dir, final String...names) {
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
  public static URL[] toURLs(final File...files) {
    URL[] urls = new URL[files.length];
    for (int i=0; i<files.length; i++) {
      try {
        urls[i] = files[i].toURI().toURL();
      } catch(MalformedURLException ignored) {
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
  public static void writeBytesToStream(final byte[] data, final OutputStream os) throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    StreamUtils.copyStream(bais, os);
    bais.close();
  }

  /**
   * Write a byte array into an file.
   * @param data the byte array to write.
   * @param path the path to the file to write to.
   * @throws IOException if an I/O error occurs.
   */
  public static void writeBytesToFile(final byte[] data, final String path) throws IOException {
    writeBytesToFile(data, new File(path));
  }

  /**
   * Write a byte array into an file.
   * @param data the byte array to write.
   * @param path the path to the file to write to.
   * @throws IOException if an I/O error occurs.
   */
  public static void writeBytesToFile(final byte[] data, final File path) throws IOException {
    ByteArrayInputStream bais = new ByteArrayInputStream(data);
    OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
    StreamUtils.copyStream(bais, os);
  }

  /**
   * Delete the specified path, recursively if this is a directory.
   * @param path the path to delete.
   * @return true if the folder and all contained files and subfolders were deleted, false otherwise.
   */
  public static boolean deletePath(final File path) {
    if ((path == null) || !path.exists()) return false;
    boolean success = true;
    try {
      if (path.isDirectory()) {
        File[] files = path.listFiles();
        if (files != null) {
          for (File child: files) {
            if (!deletePath(child)) success = false;
          }
        }
      }
      if (!path.delete()) success = false;
    } catch (Exception e) {
      success = false;
    }
    return success;
  }

  /**
   * Create the folders of the specified path, if they do not all already esist.
   * @param file the path for which to create the folders. If it is a file, then folders for its parent path are created.
   * @throws IOException if the folders could not be created.
   */
  public static void mkdirs(final File file) throws IOException {
    File folder = file.isDirectory() ? file : file.getParentFile();
    if (!folder.exists()) {
      if (!folder.mkdirs()) throw new IOException("could not create folder " + folder);
    }
  }

  /**
   * Transform a file path into a URL.
   * @param path the path to transform.
   * @return the path expressed as a URL.
   */
  public static URL getURLFromFilePath(final String path) {
    File file = new File(path);
    try {
      return file.toURI().toURL();
    } catch (MalformedURLException ignore) {
    }
    return null;
  }

  /**
   * Transform a file path into a URL.
   * @param path the path to transform.
   * @return the path expressed as a URL.
   */
  public static URL getURLFromFilePath(final File path) {
    try {
      return path.toURI().toURL();
    } catch (MalformedURLException ignore) {
      return null;
    }
  }
}
