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

package org.jppf.example.extendedclassloading;

import java.io.*;
import java.net.URL;
import java.util.*;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;

/**
 * A simple implementation of a repository.
 * <p>This repository is pesrsisted locally. The persistence only handles jar files in this implementation.
 * These files are saved in a root directory specified in the constructor, with a flat structure (no sub-folders).
 * The file names in this folder have the format <code><i>actual_jar_name</i>-<i>signature</i>.jar</code>.
 * <p>In addition to the jar files, the persistence maintains a text file named 'toDelete.txt' which contains a list
 * of jar files to delete upon loading of the repository. This is a workaround for the fact that on some OSes (e.g. Windows),
 *  the JVM keeps a lock on the jar files it uses,  which makes it impossible to delete them as long as the JVM is alive.
 * @author Laurent Cohen
 */
public class RepositoryImpl implements Repository {
  /**
   * Location where the libraries are stored on the local file system.
   */
  private final String rootDir;
  /**
   * Location of the file listing libraries to delete on the local file system.
   */
  private final String toDeleteFile;
  /**
   * A list of old library files to delete whenever possible.
   */
  private Set<String> filesToDelete = new HashSet<>();

  /**
   * Create a repository persisted in the specified root folder.
   * @param rootDir the folder where the libraries are stored on the local file system.
   */
  public RepositoryImpl(final String rootDir) {
    this.rootDir = rootDir;
    checkOrCreateRootFolder();
    toDeleteFile = rootDir + "/toDelete.txt";
    loadFilesToDelete();
    cleanup();
  }

  @Override
  public URL[] download(final ClassPath classpath, final AbstractJPPFClassLoader cl) {
    final URL[] urls = new URL[classpath.size()];
    final List<String> toDownload = new ArrayList<>();
    final List<Integer> toDownloadIndices = new ArrayList<>();
    int idx = 0;
    for (final Map.Entry<String, String> elt: classpath.elements().entrySet()) {
      try {
        final String name = elt.getKey();
        final String signature = elt.getValue();
        final File file = getLibFilePath(name, signature);
        // is the file already in the repository ?
        if (file.exists()) urls[idx] = file.toURI().toURL(); // yes: add it to the results
        else {
          // no: collect the file name and its position in the result array
          toDownload.add(name);
          toDownloadIndices.add(idx);
        }
      } catch (final Exception e) {
        urls[idx] = null;
        e.printStackTrace();
      } finally {
        idx++;
      }
    }
    // download the missing files from the client and save them in the repository
    if (!toDownload.isEmpty()) {
      System.out.println("downloading files " + toDownload);
      final String[] fileNames = toDownload.toArray(new String[toDownload.size()]);
      // download the files from the remote client's classpath
      final URL[] tempUrls = cl.getMultipleResources(fileNames);
      // copy each downloaded file to a permanent location so it will survive a node restart
      for (int i=0; i<toDownload.size(); i++) {
        final String name = toDownload.get(i);
        final int index = toDownloadIndices.get(i);
        final URL tempUrl = tempUrls[i];
        if (tempUrl != null) {
          // compute the signature and save the file to rootDir/name-signature.jar
          final String signature = ClassPathHelper.computeSignature(tempUrl);
          try {
            urls[index] = saveLibToFile(name, signature, tempUrl);
          } catch (final Exception e) {
            urls[index] = null;
            System.out.println("could not copy '" + name + "' to a permanent location : " + ExceptionUtils.getMessage(e));
          }
        }
        else System.out.println("library file '" + name + "' could not be downloaded from the client");
      }
    }
    return urls;
  }

  /**
   * Save a library file obtained from the resource cache of the node.
   * @param fileName the name of the resource.
   * @param signature the library file MD5 signature.
   * @param tempUrl URl provided by the JPPF class loader which points to a location in the temporary cache.
   * @return a URL pointing to the permanenent location where the resource is saved.
   * @throws Exception if any error occurs.
   */
  private URL saveLibToFile(final String fileName, final String signature, final URL tempUrl) throws Exception {
    // save the file to the local directory
    final File file = getLibFilePath(fileName, signature);
    final OutputStream os = FileUtils.getFileOutputStream(file);
    final InputStream is = tempUrl.openStream();
    // copy the input stream into the output stream
    StreamUtils.copyStream(is, os);
    return file.toURI().toURL();
  }

  /**
   * Compute the actual file path for a library, based on the original name and its signature.
   * The resulting file path should be in the form <code>root_folder/<i>name</i>-<i>signature</i>.jar</code>
   * @param libName the original name of the library.
   * @param signature the file's signature.
   * @return a normalized fie path.
   */
  private File getLibFilePath(final String libName, final String signature) {
    final StringBuilder sb = new StringBuilder();
    sb.append(rootDir).append('/').append(getLibFileName(libName, signature));
    return new File(sb.toString());
  }

  /**
   * Compute the file name for a library, based on the original name and its signature.
   * The resulting file name should be in the form <code><i>name</i>-<i>signature</i>.jar</code>
   * @param libName the original name of the library.
   * @param signature the file's signature.
   * @return a file name as a string.
   */
  private static String getLibFileName(final String libName, final String signature) {
    final StringBuilder sb = new StringBuilder();
    final int n = libName.lastIndexOf('.');
    if (n >= 0) {
      final String ext = libName.substring(n);
      final String s = libName.substring(0, n);
      sb.append(s).append('-').append(signature).append(ext);
    } else sb.append(libName).append('-').append(signature);
    return sb.toString();
  }

  @Override
  public void delete(final RepositoryFilter filter) {
    try {
      final File dir = new File(rootDir + "/");
      // get the list of all jar files in the repository
      final File[] files = dir.listFiles(new FileFilter() {
        @Override
        public boolean accept(final File path) {
          final String name = path.getName();
          return name.endsWith(".jar");
        }
      });
      // apply the filter to the list of files
      for (final File file: files) {
        final String filename = file.getName();
        if (!filesToDelete.contains(filename)) {
          // extract the original name and signature from the full file name 
          final int idx = filename.lastIndexOf('-');
          final int idx2 = filename.lastIndexOf('.');
          final String name = filename.substring(0, idx) + ".jar";
          final String signature = filename.substring(idx + 1, idx2);
          // if the file is accepted by the filer, add it to the list of files to delete
          final boolean accepted = filter.accepts(name, signature);
          if (accepted) filesToDelete.add(filename);
        }
      }
    } finally {
      // perform the actual file deletions
      deleteFilesToDelete();
    }
  }

  /**
   * Load the list of libraries to delete from the file system.
   */
  private void loadFilesToDelete() {
    try {
      final File file = new File(toDeleteFile);
      if (!file.exists()) return;
      final Reader reader = new BufferedReader(new FileReader(file));
      // transform the text file into a set of  strings and close the reader.
      filesToDelete = new HashSet<>(FileUtils.textFileAsLines(reader));
      deleteFilesToDelete();
    } catch (@SuppressWarnings("unused") final IOException ignore) {
    }
  }

  /**
   * Save the list of old libraries to delete from the file system.
   */
  private void saveFilesToDelete() {
    final File file = new File(toDeleteFile);
    if (filesToDelete.isEmpty()) {
      if (file.exists()) file.delete();
    } else {
      BufferedWriter writer = null;
      try {
        writer = new BufferedWriter(new FileWriter(file));
        for (String s: filesToDelete) writer.write(s + '\n');
      } catch (@SuppressWarnings("unused") final IOException ignore) {
      } finally {
        StreamUtils.closeSilent(writer);
      }
    }
  }

  /**
   * Delete the old libraries if possible.
   */
  private void deleteFilesToDelete() {
    try {
      final File dir = new File(rootDir + "/");
      final Iterator<String> it = filesToDelete.iterator();
      while (it.hasNext()) {
        final File file = new File(dir, it.next());
        // remove the file from the list if it doesn't exist or if its deletion is successful
        if (!file.exists() || file.delete()) it.remove();
      }
    } finally {
      saveFilesToDelete();
    }
  }

  @Override
  public void cleanup() {
    deleteFilesToDelete();
  }

  /**
   * Check that the root folder for this repository exists and create it if it doesn't.
   */
  private void checkOrCreateRootFolder() {
    final File file = new File(rootDir);
    if (!file.exists()) {
      if (!file.mkdirs()) throw new IllegalStateException("could not create the repository root '" + rootDir + "'");
    }
  }
}
