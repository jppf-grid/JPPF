/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

import org.slf4j.*;

/**
 * A file walker that deletes a complete file and folder hierarchy.
 */
public class DeleteFileVisitor extends SimpleFileVisitor<Path> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DeleteFileVisitor.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Optional path matcher to filter the files to delete.
   */
  private final PathMatcher fileMatcher;
  /**
   * Optional path matcher to filter the directories to delete.
   */
  private final PathMatcher dirMatcher;

  /**
   * Initialize without a matcher.
   */
  public DeleteFileVisitor() {
    this(null, null);
  }

  /**
   * Initialize with the specified path matcher.
   * @param fileMatcher an optional path matcher to filter the files to delete.
   */
  public DeleteFileVisitor(final PathMatcher fileMatcher) {
    this(fileMatcher, null);
  }

  /**
   * Initialize with the specified file and directory path matchers.
   * @param fileMatcher an optional path matcher to filter the files to delete.
   * @param dirMatcher an optional path matcher to filter the directories to delete.
   */
  public DeleteFileVisitor(final PathMatcher fileMatcher, final PathMatcher dirMatcher) {
    this.fileMatcher = fileMatcher;
    this.dirMatcher = dirMatcher;
  }

  @Override
  public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
    try {
      if ((fileMatcher == null) || fileMatcher.matches(file)) {
        if (debugEnabled) log.debug("deleting file = {}", file);
        Files.delete(file);
      }
    } catch (final NoSuchFileException e) {
      if (debugEnabled) log.debug("error trying to delete file '{}':\n{}", file, ExceptionUtils.getStackTrace(e));
      else log.warn("error trying to delete file '{}': {}", file, ExceptionUtils.getMessage(e));
    }
    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
    try {
      if ((dir.toFile().listFiles().length <= 0) && ((dirMatcher == null) || dirMatcher.matches(dir))) {
        if (debugEnabled)  log.debug("deleting dir = {}", dir);
        Files.delete(dir);
      }
    } catch (final NoSuchFileException e2) {
      if (debugEnabled) log.debug("error trying to delete directory '{}':\n{}", dir, ExceptionUtils.getStackTrace(e2));
      else log.warn("error trying to delete directory '{}': {}", dir, ExceptionUtils.getMessage(e2));
    }
    if (e != null) throw e;
    return FileVisitResult.CONTINUE;
  }
}