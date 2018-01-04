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

package org.jppf.persistence;

import java.io.IOException;
import java.nio.file.*;

import org.jppf.utils.FileUtils;
import org.slf4j.*;

/**
 * Abstract common superclass for file-based persistence implementations.
 * @param <I> the type of objects to persist.
 * @param <E> the type of exceptions to raise.
 * @author Laurent Cohen
 * @since 6.0
 */
public abstract class AbstractFilePersistence<I, E extends Exception> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractFilePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Default extension for the persisted objects files.
   * @exclude
   */
  protected static final String DEFAULT_EXTENSION = ".data";
  /**
   * Default extension for temporary files.
   * @exclude
   */
  protected static final String TEMP_EXTENSION = ".tmp";
  /**
   * The root directory for this persistence.
   * @exclude
   */
  protected final Path rootPath;

  /**
   * Initialize this persistence with the specified path as root directory.
   * @param paths the root directory for this persistence.
   */
  public AbstractFilePersistence(final String... paths) {
    this.rootPath = Paths.get(paths[0]);
    if (debugEnabled) log.debug("initializing {} with rootPath={}", getClass().getSimpleName(), rootPath);
  }

  /**
   * Get the full path name for the file or directory denoted by the specified path.
   * @param path the path for which to get the full name.
   * @return the full path name.
   * @exclude
   */
  protected String pathname(final Path path) {
    return path.toFile().getPath();
  }

  /**
   * Check the specified directory and create it if it doesn't exist.
   * @param dir the directory to check.
   * @throws E if any error occurs.
   * @exclude
   */
  protected synchronized void checkDirectory(final Path dir) throws E {
    if (!Files.exists(dir)) {
      try {
        Files.createDirectories(dir);
      } catch (final IOException e) {
        throw convertException(e);
      }
    }
  }

  /**
   * Delete the specified directyory if it is empty.
   * @param channelPath directory path to delete.
   * @return true if the directory was deleted, false otherwise.
   * @throws E if any error occurs.
   * @exclude
   */
  protected boolean deleteIfEmpty(final Path channelPath) throws E {
    if (!Files.exists(channelPath)) return false;
    try {
      try (DirectoryStream<Path> channelDS = Files.newDirectoryStream(channelPath)) {
        for (Path path : channelDS) {
          if (!Files.isDirectory(path) && pathname(path.getFileName()).endsWith(DEFAULT_EXTENSION)) return false;
        }
      }
      Files.walkFileTree(channelPath, new FileUtils.DeleteFileVisitor());
      return true;
    } catch (final IOException e) {
      throw convertException(e);
    }
  }

  /**
   * Get the directory of witht he psecified name directly under the root.
   * @param name the name of the directory to lookup.
   * @return a {@link Path} instance.
   * @exclude
   */
  protected Path getSubDir(final String name) {
    return Paths.get(pathname(rootPath), name);
  }

  /**
   * A filter that only matches directories.
   * @exclude
   */
  protected static class DirectoryFilter implements DirectoryStream.Filter<Path> {
    /** */
    public DirectoryFilter() {
    }

    @Override
    public boolean accept(final Path entry) throws IOException {
      return Files.isDirectory(entry);
    }
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append("[rootPath=").append(rootPath).append(']').toString();
  }

  /**
   * Create the appropriate type of exception.
   * @param e the actual exception from which to create the new one.
   * @return an exception of type E.
   * @exclude
   */
  protected abstract E convertException(final Exception e);
}
