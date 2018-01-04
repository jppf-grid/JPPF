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
import java.util.*;

import org.jppf.job.persistence.*;
import org.jppf.persistence.AbstractFilePersistence;
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
public class DefaultFilePersistence extends AbstractFilePersistence<PersistenceInfo, JobPersistenceException> implements JobPersistence {
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
   * Initialize this persistence with the root path {@link #DEFAULT_ROOT} under the current user directory.
   */
  public DefaultFilePersistence() {
    this(DEFAULT_ROOT);
  }

  /**
   * Initialize this persistence with the specified path as root directory.
   * @param paths the root directory for this persistence.
   */
  public DefaultFilePersistence(final String... paths) {
    super(paths.length > 0 ? paths : new String[] { DEFAULT_ROOT });
  }

  /**
   * In this implementation, job elements are first stored in a temporary file, then moved to a "normal" file once the atomic store operation is complete.
   * This addresses the situation where the store operation is interrupted (because the driver dies or any other reason) and would leave an incomplete or corrupted file.
   * @param infos collection of information objects on the job elements to store.
   * @throws JobPersistenceException if any erorr occurs during the persistence operation.
   */
  @Override
  public void store(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    try {
      if (debugEnabled) log.debug("storing {}", infos);
      final Path jobDir = getSubDir(infos.iterator().next().getJobUuid());
      checkDirectory(jobDir);
      for (final PersistenceInfo info: infos) {
        final Path path = getPathFor(jobDir, info, false);
        final Path tmpPath = getPathFor(jobDir, info, true);
        try (BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING));
          InputStream in = info.getInputStream()) {
          StreamUtils.copyStream(in, out, false);
        }
        Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
      }
    } catch (final Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<InputStream> load(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if ((infos == null) || infos.isEmpty()) return null;
    try {
      if (debugEnabled) log.debug("loading {}", infos);
      final Path jobDir = getSubDir(infos.iterator().next().getJobUuid());
      final List<InputStream> result = new ArrayList<>(infos.size());
      if (Files.exists(jobDir)) {
        for (PersistenceInfo info: infos) {
          final Path path = getPathFor(jobDir, info, false);
          result.add(new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ)));
        }
      }
      return result;
    } catch (final Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    try {
      final List<String> result = new ArrayList<>();
      if (Files.exists(rootPath)) {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(rootPath, new DirectoryFilter())) {
          for (Path path : ds) {
            if (path != null) result.add(path.getFileName().toString());
          }
        }
      }
      if (debugEnabled) log.debug("uuids of persisted jobs: {}", result);
      return result;
    } catch (final Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public int[] getTaskPositions(final String jobUuid) throws JobPersistenceException {
    final int[] result = getPositions(jobUuid, PersistenceObjectType.TASK);
    if (debugEnabled) log.debug("positions of tasks for job uuid={} : {}", jobUuid, StringUtils.buildString(", ", "{", "}", result));
    return result;
  }

  @Override
  public int[] getTaskResultPositions(final String jobUuid) throws JobPersistenceException {
    final int[] result = getPositions(jobUuid, PersistenceObjectType.TASK_RESULT);
    if (debugEnabled) log.debug("positions of results for job uuid={} : {}", jobUuid, StringUtils.buildString(", ", "{", "}", result));
    return result;
  }

  @Override
  public void deleteJob(final String jobUuid) throws JobPersistenceException {
    try {
      if (debugEnabled) log.debug("deleting job with uuid = {}", jobUuid);
      final Path jobDir = getSubDir(jobUuid);
      if (Files.exists(jobDir)) Files.walkFileTree(jobDir, new FileUtils.DeleteFileVisitor());
    } catch (final Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public boolean isJobPersisted(final String jobUuid) throws JobPersistenceException {
    try {
      final Path path = getPathFor(getSubDir(jobUuid), PersistenceObjectType.JOB_HEADER, -1, false);
      return (path != null) && Files.exists(path);
    } catch (final IOException e) {
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
      final Path jobDir = getSubDir(jobUuid);
      if (!Files.exists(jobDir)) positions = new int[0];
      else {
        final List<Path> list = getPathsFor(jobDir, type);
        positions = new int[list.size()];
        int count = 0;
        for (Path path : list) {
          final String s = pathname(path.getFileName());
          final String prefix = getPrefixForType(type);
          final String s2 = s.substring(prefix.length(), s.length() - DEFAULT_EXTENSION.length());
          try {
            positions[count++] = Integer.valueOf(s2);
          } catch (final Exception e) {
            if (debugEnabled) log.debug(String.format("positions of %s for job %s (path=%s, s=%s, prefix=%s, s2=%s) : %s", type, jobUuid, path, s, prefix, s2, ExceptionUtils.getStackTrace(e)));
            throw e;
          }
        }
      }
    } catch (final Exception e) {
      throw new JobPersistenceException(e);
    }
    return positions;
  }

  /**
   * Get the file path for the specified job element.
   * @param jobDir tthe path of the job's directory.
   * @param info the information on the file element for which to find a path.
   * @param isTemp whether to return a temporary file path.
   * @return a list of the matching paths in the specified directory.
   * @throws IOException if any I/O error occurs.
   */
  private Path getPathFor(final Path jobDir, final PersistenceInfo info, final boolean isTemp) throws IOException {
    return getPathFor(jobDir, info.getType(), info.getPosition(), isTemp);
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
    final String dir = pathname(jobDir);
    final String ext = isTemp ? TEMP_EXTENSION : DEFAULT_EXTENSION;
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
    final List<Path> result = new ArrayList<>();
    try (final DirectoryStream<Path> ds = Files.newDirectoryStream(jobDir, new DirectoryStream.Filter<Path>() {
      @Override
      public boolean accept(final Path entry) throws IOException {
        final String fileName = pathname(entry.getFileName());
        final String prefix = getPrefixForType(type);
        return fileName.startsWith(prefix) && fileName.endsWith(DEFAULT_EXTENSION);
      }
    })) {
      for (final Path path : ds) {
        if (path != null) result.add(path);
      }
    }
    return result;
  }

  /**
   * Get the file prefix for the given object type.
   * @param type the type of object for which to find a file.
   * @return the prefix as a string.
   */
  private static String getPrefixForType(final PersistenceObjectType type) {
    switch (type) {
      case JOB_HEADER:    return HEADER_PREFIX;
      case DATA_PROVIDER: return DATA_PROVIDER_PREFIX;
      case TASK:          return TASK_PREFIX;
      case TASK_RESULT:   return RESULT_PREFIX;
    }
    return null;
  }

  /** @exclude */
  @Override
  protected JobPersistenceException convertException(final Exception e) {
    return (e instanceof JobPersistenceException) ? (JobPersistenceException) e : new JobPersistenceException(e);
  }
}
