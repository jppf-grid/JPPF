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
package org.jppf.job.persistence.impl;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import org.jppf.job.persistence.*;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * A file-based persistent store for jobs.
 * The store's structure is made of a root directory, under which there is one directory per job, named after the job's uuid.
 * Each job directory contains:
 * <ul>
 * <li>a file named {@code header.data} for the job header</li>
 * <li>a file named {@code data_provider.data} for the job's data_provider</li>
 * <li>a file named <code>task-<i>i</i>.data</code> for each task <i>i</i> of the job</li>
 * <li>a file named <code>result-<i>i</i>.data</code> for each task result <i>i</i> received from a node</li>
 * </ul>
 * @author Laurent Cohen
 * @since 6.0
 */
public class DefaultFilePersistence implements JobPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DefaultFilePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The default root path if none is specified.
   */
  private static final String DEFAULT_ROOT = "persistence";
  /**
   * Prefix for the job header file name.
   */
  private static final String HEADER_PREFIX = "header";
  /**
   * Prefix for the data provider file name.
   */
  private static final String DATA_PROVIDER_PREFIX = "data_provider";
  /**
   * Prefix format for a task file name.
   */
  private static final String TASK_PREFIX = "task-";
  /**
   * Prefix format for a task result file name.
   */
  private static final String RESULT_PREFIX = "result-";
  /**
   * Default extension for the persisted objects files.
   */
  private static final String DEFAULT_EXTENSION = ".data";
  /**
   * Default extension for temporary files.
   */
  private static final String TEMP_EXTENSION = ".tmp";
  /**
   * The root directory for this persistence.
   */
  private final Path rootPath;

  /**
   * Initialize this persistence with the root path {@link #DEFAULT_ROOT} under the current user directory.
   * @throws JobPersistenceException if the default root does not exist and could not be created.
   */
  public DefaultFilePersistence() throws JobPersistenceException {
    this(DEFAULT_ROOT);
  }

  /**
   * Initialize this persistence with the specified path as root directory.
   * @param paths the root directory for this persistence.
   * @throws JobPersistenceException if the specified root does not exist and could not be created.
   */
  public DefaultFilePersistence(final String... paths) throws JobPersistenceException {
    this.rootPath = Paths.get((paths == null) || (paths.length == 0) || (paths[0] == null) ? DEFAULT_ROOT : paths[0]);
    if (debugEnabled) log.debug("initializing {} with rootPath={}", getClass().getSimpleName(), rootPath);
  }

  @Override
  public void store(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    try {
      if (debugEnabled) log.debug("storing {}", infos);
      Path jobDir = getJobDir(infos.iterator().next().getJobUuid());
      checkDirectory(jobDir);
      for (PersistenceInfo info: infos) {
        Path path = getPathFor(jobDir, info, false);
        Path tmpPath = getPathFor(jobDir, info, true);
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
          InputStream in = info.getInputStream()) {
          StreamUtils.copyStream(in, out, false);
        }
        Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<InputStream> load(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if ((infos == null) || infos.isEmpty()) return null;
    try {
      if (debugEnabled) log.debug("loading {}", infos);
      Path jobDir = getJobDir(infos.iterator().next().getJobUuid());
      List<InputStream> result = new ArrayList<>(infos.size());
      for (PersistenceInfo info: infos) {
        Path path = getPathFor(jobDir, info, false);
        result.add(new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ)));
      }
      return result;
    } catch (Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    try {
      List<String> result = new ArrayList<>();
      DirectoryStream<Path> ds = Files.newDirectoryStream(rootPath, new DirectoryStream.Filter<Path>() {
        @Override
        public boolean accept(final Path entry) throws IOException {
          return Files.isDirectory(entry);
        }
      });
      for (Path path : ds) {
        if (path != null) result.add(path.getFileName().toString());
      }
      if (debugEnabled) log.debug("uuids of persisted jobs: {}", result);
      return result;
    } catch (Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public int[] getTaskPositions(final String jobUuid) throws JobPersistenceException {
    int[] result = getPositions(jobUuid, PersistenceObjectType.TASK);
    if (debugEnabled) log.debug("positions of tasks for job uuid={} : {}", jobUuid, StringUtils.buildString(", ", "{", "}", result));
    return result;
  }

  @Override
  public int[] getTaskResultPositions(final String jobUuid) throws JobPersistenceException {
    int[] result = getPositions(jobUuid, PersistenceObjectType.TASK_RESULT);
    if (debugEnabled) log.debug("positions of results for job uuid={} : {}", jobUuid, StringUtils.buildString(", ", "{", "}", result));
    return result;
  }

  @Override
  public void deleteJob(final String jobUuid) throws JobPersistenceException {
    try {
      if (debugEnabled) log.debug("deleting job with uuid = {}", jobUuid);
      Files.walkFileTree(getJobDir(jobUuid), new DeleteFileVisitor());
    } catch (Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public boolean isJobPersisted(final String jobUuid) throws JobPersistenceException {
    try {
      Path path = getPathFor(getJobDir(jobUuid), PersistenceObjectType.JOB_HEADER, -1, false);
      return (path != null) && Files.exists(path);
    } catch (IOException e) {
      throw new JobPersistenceException(e);
    }
  }

  /**
   * Get the positions of the specified types of files.
   * @param jobUuid the job uuid for which to get the tasks positions.
   * @param type the type of files for which to get the tasks positions.
   * Must be one of {@link PersistenceObjectType#TASK} or {@link PersistenceObjectType#TASK_RESULT}.
   * @return the tasks positions of the files.
   * @throws JobPersistenceException if any error occurs.
   */
  private int[] getPositions(final String jobUuid, final PersistenceObjectType type) throws JobPersistenceException {
    int[] positions = null;
    try {
      List<Path> list = getPathsFor(getJobDir(jobUuid), type);
      positions = new int[list.size()];
      int count = 0;
      for (Path path : list) {
        String s = pathname(path.getFileName());
        String prefix = getPrefixForType(type);
        String s2 = s.substring(prefix.length(), s.length() - DEFAULT_EXTENSION.length());
        try {
          positions[count++] = Integer.valueOf(s2);
        } catch (Exception e) {
          if (debugEnabled) log.debug(String.format("positions of %s for job %s (path=%s, s=%s, prefix=%s, s2=%s) : %s", type, jobUuid, path, s, prefix, s2, ExceptionUtils.getStackTrace(e)));
          throw e;
        }
      }
    } catch (Exception e) {
      throw new JobPersistenceException(e);
    }
    return positions;
  }

  /**
   * Get the directory of the job with the specified uuid.
   * @param jobUuid the uuid of the job for which to get a path.
   * @return a {@link Path} instance.
   */
  private Path getJobDir(final String jobUuid) {
    return Paths.get(pathname(rootPath), jobUuid);
  }

  /**
   *
   * @param jobDir tthe path of the job's directory.
   * @param info the information on the file to find.
   * @param isTemp whether to return a temp file path.
   * @return a list of the matching paths in the specified directory.
   * @throws IOException if any I/O error occurs.
   */
  private Path getPathFor(final Path jobDir, final PersistenceInfo info, final boolean isTemp) throws IOException {
    return getPathFor(jobDir, info.getType(), info.getTaskPosition(), isTemp);
  }

  /**
   *
   * @param jobDir the path of the job's directory.
   * @param type the type of object to find.
   * @param position the task or task result position when applicable.
   * @param isTemp whether to return a temp file path.
   * @return a list of the matching paths in the specified directory.
   * @throws IOException if any I/O error occurs.
   */
  private Path getPathFor(final Path jobDir, final PersistenceObjectType type, final int position, final boolean isTemp) throws IOException {
    String dir = pathname(jobDir);
    String ext = isTemp ? TEMP_EXTENSION : DEFAULT_EXTENSION;
    switch (type) {
      case JOB_HEADER:    return Paths.get(dir, HEADER_PREFIX + ext);
      case DATA_PROVIDER: return Paths.get(dir, DATA_PROVIDER_PREFIX + ext);
      case TASK:          return Paths.get(dir, TASK_PREFIX + position + ext);
      case TASK_RESULT:   return Paths.get(dir, RESULT_PREFIX + position + ext);
    }
    return null;
  }

  /**
   *
   * @param jobDir the path of the job's directory.
   * @param type the type of files to find.
   * @return a list of the matching paths in the specified directory.
   * @throws IOException if any I/O error occurs.
   */
  private List<Path> getPathsFor(final Path jobDir, final PersistenceObjectType type) throws IOException {
    List<Path> result = new ArrayList<>();
    DirectoryStream<Path> ds = Files.newDirectoryStream(jobDir, new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(final Path entry) throws IOException {
        String fileName = pathname(entry.getFileName());
        String prefix = getPrefixForType(type);
        return fileName.startsWith(prefix) && fileName.endsWith(DEFAULT_EXTENSION);
      }
    });
    for (Path path : ds) {
      if (path != null) result.add(path);
    }
    return result;
  }

  /**
   * Get the file prefix for the given object type.
   * @param type the type of object for which to find a file.
   * @return the prefix as a string.
   */
  private String getPrefixForType(final PersistenceObjectType type) {
    switch (type) {
      case JOB_HEADER:    return HEADER_PREFIX;
      case DATA_PROVIDER: return DATA_PROVIDER_PREFIX;
      case TASK:          return TASK_PREFIX;
      case TASK_RESULT:   return RESULT_PREFIX;
    }
    return null;
  }

  /**
   * Get the full path name for the file or directory denoted by the specified path.
   * @param path the path for which to get the full name.
   * @return the full path name.
   */
  private String pathname(final Path path) {
    //return path.toString();
    return path.toFile().getPath();
  }

  /**
   * Check the specified directory and create it if it doesn't exist.
   * @param dir the directory to check.
   * @throws Exception if any error occurs.
   */
  private synchronized void checkDirectory(final Path dir) throws Exception {
    if (!Files.exists(dir)) {
      try {
        Files.createDirectories(dir);
      } catch (IOException e) {
        throw new JobPersistenceException(e);
      }
    }
  }

  /**
   * A file walker that deletes a complete file and folder hierarchy.
   */
  private class DeleteFileVisitor extends SimpleFileVisitor<Path> {
    @Override
    public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
      Files.delete(file);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(final Path dir, final IOException e) throws IOException {
      if (e != null) throw e;
      Files.delete(dir);
      return FileVisitResult.CONTINUE;
    }
  }
}
