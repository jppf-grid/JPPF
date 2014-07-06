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

package org.jppf.example.extendedclassloading;

import java.io.*;
import java.util.*;

import org.jppf.utils.FileUtils;

/**
 * A default implementation of a <code>ClassPath</code>, backed by a {@link HashMap} for its elements.
 * <p>Depending on which constructor is used, this classpath may be transient or pesrsisted locally.
 * <p>The persistence only handles jar files in this implementation. These must be in a folder
 * specified in the constructor, with a flat structure (no sub-folders).
 * The file names in this folder have the format <code><i>actual_jar_name</i>-<i>signature</i>.jar</code>.
 * <p>The persistence maintains a single text file named "index.txt" in this same folder, which contains the definition for this classpath.
 * The classpath definition consists in a set of entries, each on a separate line, with the following format:<br/>
 * <code><i>jar_file_name</i>=<i>signature</i></code>.
 * <p>Example definition file content:
 * <pre>ClientLib1.jar=1B43C50E293A4DD0DEC3FA1D7297D0AF
ClientLib2.jar=D23B5C671CE09DF91C41BE7153E949E1</pre>
 * @author Laurent Cohen
 */
public class ClassPathImpl implements ClassPath {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Location where the libraries are stored on the local file system.
   */
  private final String rootDir;
  /**
   * Location listing the libraries to add or update on the local file system.
   */
  private final String indexFile;
  /**
   * Represents the repository elements.
   */
  private Map<String, String> elementsMap = new HashMap<>();

  /**
   * Create a transient repository that is not persisted.
   */
  public ClassPathImpl() {
    this.rootDir = null;
    indexFile = null;
  }

  /**
   * Create a classpath whose defintiion can be persisted in the specified folder.
   * @param rootDir the folder where the libraries are stored on the local file system.
   */
  public ClassPathImpl(final String rootDir) {
    this.rootDir = rootDir;
    indexFile = rootDir + "/index.txt";
  }

  @Override
  public boolean addElement(final String key, final String signature) {
    return elementsMap.put(key, signature) != null;
  }

  @Override
  public boolean removeElement(final String key) {
    String signature = elementsMap.remove(key);
    return signature != null;
  }

  @Override
  public String getElementSignature(final String key) {
    return elementsMap.get(key);
  }

  @Override
  public Map<String, String> elements() {
    return Collections.unmodifiableMap(new HashMap<>(elementsMap));
  }

  @Override
  public Collection<String> elementNames() {
    return Collections.unmodifiableSet(new HashSet<>(elementsMap.keySet()));
  }

  @Override
  public int size() {
    return elementsMap.size();
  }

  /**
   * Load this classpath from a definition file.
   * @return this classpath instance with the elements read from the definition file.
   * @throws IOException if any I/O error occurs.
   */
  public ClassPath loadFromDefinition() throws IOException {
    if (rootDir == null) throw new IllegalStateException("this repository is not persisted and can't be loaded");
    elementsMap.clear();
    checkOrCreateDefinition();
    List<String> lines = new ArrayList<>();
    Reader reader = FileUtils.getFileReader(indexFile);
    lines = FileUtils.textFileAsLines(reader);
    for (String line: lines) {
      line = line.trim();
      if (!line.isEmpty()) {
        String[] association = line.split("=");
        addElement(association[0], association[1]);
      }
    }
    return this;
  }

  /**
   * Save this classpath to a definition file.
   * @throws IOException if any I/O error occurs.
   */
  public void saveToDefinition() throws IOException {
    if (rootDir == null) throw new IllegalStateException("this repository is not persisted and can't be saved");
    checkOrCreateDefinition();
    Writer writer = null;
    try {
      writer = FileUtils.getFileWriter(indexFile);
      for (Map.Entry<String, String> entry: this.elements().entrySet()) {
        writer.write(entry.getKey() + '=' + entry.getValue() + '\n');
      }
    } finally {
      if (writer != null) writer.close();
    }
  }

  /**
   * Check if the definition exists and create it if it doesn't.
   * @throws IOException if the store could not be created.
   */
  private void checkOrCreateDefinition() throws IOException {
    File idxFile = new File(indexFile);
    if (!idxFile.exists()) {
      File dir = idxFile.getParentFile();
      // if the folder does not exist, attemp to create it
      if (!dir.exists()) {
        if (!dir.mkdirs()) throw new IOException("could not create the library store at '" + dir.getCanonicalPath() + "'");
      }
      // create an empty index file
      FileWriter writer = null;
      try {
        writer = new FileWriter(indexFile);
      } finally {
        if (writer != null) writer.close();
      }
    }
  }

  /**
   * Load this <code>ClassPath</code> by scanning its root folder for jar files
   * and computng their signatures.
   * @return this classpath instance with the elements found in the root folder.
   * @throws IOException if any I/O error occurs.
   */
  public ClassPath loadFromFileSystem() throws IOException {
    File dir = new File(rootDir + '/');
    // list all the jar files in the folder
    File[] jars = dir.listFiles(new FileFilter() {
      @Override
      public boolean accept(final File file) {
        return !file.isDirectory() && file.getName().toLowerCase().endsWith(".jar");
      }
    });
    if ((jars != null) && (jars.length > 0)) {
      for (File jar: jars) {
        String name = jar.getName();
        String signature = ClassPathHelper.computeSignature(jar);
        addElement(name, signature);
      }
    }
    return this;
  }

  /**
   * Get the root directory location.
   * @return the root folder where jar files are stored.
   */
  public String getRootDir() {
    return rootDir;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + '[' + elementsMap + ']';
  }
}
