/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.load.balancer.persistence;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.persistence.AbstractFilePersistence;
import org.jppf.serialization.JPPFSerializationHelper;
import org.jppf.utils.DeleteFileVisitor;
import org.slf4j.*;

/**
 * File-based persistence implementation for the state of the JPPF load-balancers.
 * It relies on a directory structure is as follows:
 * <p><pre class="jppf_pre">
 * persistence_root
 * |_channel_identifier<sub>1</sub>
 * | |_algorithm<sub>1</sub>.data
 * | |_...
 * | |_algorithm<sub>P1</sub>.data
 * |_...
 * |_channel_identifier<sub>n</sub>
 *   |_algorithm<sub>1</sub>.data
 *   |_...
 *   |_algorithm<sub>Pn</sub>.data</pre>
 *
 * <p>Where:
 * <ul style="margin-top: 0px">
 *   <li>Each <code>channel_identifier<sub>i</sub></code> represents a hash of a string concatenated from various properties of the channel.
 *     A channel represents a connection between a node and a driver for server-side load-balancing, or between a driver and a client for client-side load-balancing.
 *     This id is unique for each channel
 *     and resilient over restarts of both related peers, contrary to their uuids, which are recreated each time a component starts. Using a hash also ensures that it
 *     can be used a s a valid folder name in a file system</li>
 *   <li>Each <code>algorithm<sub>i</sub></code> prefix is the hash of the related load-balancing algorithm name. Again, it ensures it can be used to form a valid file name</li>
 *   <li>Each <code>algorithm<sub>i</sub></code>.data file represents the serialized state of the related load-balancer</li>
 * </ul>
 * @author Laurent Cohen
 * @since 6.0
 */
public class FileLoadBalancerPersistence extends AbstractFilePersistence<LoadBalancerPersistenceInfo, LoadBalancerPersistenceException> implements LoadBalancerPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(FileLoadBalancerPersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * The default root path if none is specified.
   */
  public static final String DEFAULT_ROOT = "lb_persistence";
  /**
   * The number of persistence operations, including load, store, delete and list, that have started but not yet completed.
   */
  private final AtomicInteger uncompletedOperations = new AtomicInteger(0);

  /**
   * Initialize this persistence with the root path {@link #DEFAULT_ROOT} under the current user directory.
   */
  public FileLoadBalancerPersistence() {
    this(DEFAULT_ROOT);
  }

  /**
   * Initialize this persistence with the specified path as root directory.
   * @param paths the root directory for this persistence.
   */
  public FileLoadBalancerPersistence(final String... paths) {
    super(paths.length > 0 ? paths : new String[] { DEFAULT_ROOT });
  }

  @Override
  public void store(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    try {
      if (debugEnabled) log.debug("storing {}", info);
      final Path nodeDir = getSubDir(info.getChannelID());
      checkDirectory(nodeDir);
      final Path path = getBundlerPath(nodeDir, info.getAlgorithmID(), false);
      final Path tmpPath = getBundlerPath(nodeDir, info.getAlgorithmID(), true);
      try (final BufferedOutputStream out = new BufferedOutputStream(Files.newOutputStream(tmpPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
        info.serializeToStream(out);
      }
      Files.move(tmpPath, path, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (final Exception e) {
      throw new LoadBalancerPersistenceException(e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
  }

  @Override
  public Object load(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    try {
      if (debugEnabled) log.debug("loading {}", info);
      final Path nodeDir = getSubDir(info.getChannelID());
      if (!Files.exists(nodeDir)) return null;
      final Path path = getBundlerPath(nodeDir, info.getAlgorithmID(), false);
      if (!Files.exists(path)) return null;
      try (final InputStream is = new BufferedInputStream(Files.newInputStream(path, StandardOpenOption.READ))) {
        return JPPFSerializationHelper.deserialize(is);
      }
    } catch (final Exception e) {
      throw new LoadBalancerPersistenceException(e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
  }

  @Override
  public void delete(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    try {
      if (debugEnabled) log.debug("deleting bundlers for {}", info);
      if ((info == null) || ((info.getChannelID() == null) && (info.getAlgorithmID() == null))) {
        if (Files.exists(rootPath)) Files.walkFileTree(rootPath, new DeleteFileVisitor(null, dir -> !isSameFile(rootPath, dir)));
      } else if (info.getAlgorithmID() == null) {
        final Path channelDir = getSubDir(info.getChannelID());
        if (Files.exists(channelDir)) Files.walkFileTree(channelDir, new DeleteFileVisitor());
      } else if (info.getChannelID() == null) {
        final String filename = info.getAlgorithmID() + DEFAULT_EXTENSION;
        Files.walkFileTree(rootPath, new DeleteFileVisitor(file -> filename.equals(file.getFileName().toString()), dir -> !isSameFile(rootPath, dir)));
      } else {
        final Path path = getBundlerPath(getSubDir(info.getChannelID()), info.getAlgorithmID(), false);
        if (debugEnabled) log.debug("deleting path = {}", path);
        Files.deleteIfExists(path);
        deleteIfEmpty(getSubDir(info.getChannelID()));
      }
      if (debugEnabled) log.debug("delete done for {}", info);
    } catch (final Exception e) {
      throw new LoadBalancerPersistenceException(e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
  }

  @Override
  public List<String> list(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    try {
      final List<String> result = new ArrayList<>();
      if ((info == null) || ((info.getChannelID() == null) && (info.getAlgorithmID() == null))) {
        if (Files.exists(rootPath)) {
          try (final DirectoryStream<Path> nodeDS = Files.newDirectoryStream(rootPath, new DirectoryFilter())) {
            for (final Path path : nodeDS) {
              if (path != null) result.add(path.getFileName().toString());
            }
          }
        }
      } else if (info.getAlgorithmID() == null) {
        if (traceEnabled) log.trace("listing algos for request={}", info);
        if (Files.exists(rootPath)) {
          final Path channelDir = getSubDir(info.getChannelID());
          if (traceEnabled) log.trace("listing algos in {}", channelDir);
          if (Files.exists(channelDir)) {
            if (traceEnabled) log.trace("listing algos in existing {}", channelDir);
            try (final DirectoryStream<Path> channelDS = Files.newDirectoryStream(channelDir, entry -> {
                if (traceEnabled) log.trace("filter checking {}", entry);
                return !Files.isDirectory(entry) && pathname(entry).endsWith(DEFAULT_EXTENSION);
              }
            )) {
              for (final Path path : channelDS) {
                if (path != null) {
                  final String name = pathname(path.getFileName());
                  final String algo = name.substring(0, name.length() - DEFAULT_EXTENSION.length());
                  if (traceEnabled) log.trace("algo is {} for path={}", algo, path);
                  result.add(algo);
                }
              }
            }
          }
        }
      } else if (info.getChannelID() == null) {
        try (final DirectoryStream<Path> channelDS = Files.newDirectoryStream(rootPath, new DirectoryFilter())) {
          for (final Path channel : channelDS) {
            if (channel != null) {
              final Path algoPath = getBundlerPath(channel, info.getAlgorithmID(), false);
              if (Files.exists(algoPath)) result.add(pathname(channel.getFileName()));
            }
          }
        }
      } else {
        final Path path = getBundlerPath(getSubDir(info.getChannelID()), info.getAlgorithmID(), false);
        if (Files.exists(path)) result.add(info.getAlgorithmID());
      }
      if (debugEnabled) log.debug("identifiers of persisted bundler states: {} for request={}", result, info);
      return result;
    } catch (final Exception e) {
      throw new LoadBalancerPersistenceException(e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
  }

  /**
   * Get the path to the file containing a bundler's data.
   * @param channelDir the direcory of the node for which to get a path.
   * @param algorithm the load balancer algorithm name.
   * @param temp whether to return a temporary file path.
   * @return a {@link Path} instance.
   */
  private Path getBundlerPath(final Path channelDir, final String algorithm, final boolean temp) {
    return Paths.get(pathname(channelDir), algorithm + (temp ? TEMP_EXTENSION : DEFAULT_EXTENSION));
  }

  /** @exclude */
  @Override
  protected LoadBalancerPersistenceException convertException(final Exception e) {
    return (e instanceof LoadBalancerPersistenceException) ? (LoadBalancerPersistenceException) e : new LoadBalancerPersistenceException(e);
  }

  @Override
  public int getUncompletedOperations() {
    return uncompletedOperations.get();
  }
}
