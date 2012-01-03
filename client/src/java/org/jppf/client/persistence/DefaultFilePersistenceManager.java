/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.client.persistence;

import java.io.*;
import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.*;

/**
 * This implementation of {@link JobPersistence} stores jobs on the file system using
 * Java serialization to a file for each job, and uses the jobs uuids as keys to retrieve them.
 * <p>The underlying datastore is a file folder with a flat structure, where each file
 * represents a serialized job.
 * <p>This implementation will use the serialisation scheme, if any, that is configured for the JPPF client.
 * <p>Note that this implementation is very naive and has a sub-optimal performance,
 * as it stores jobs data in a synchronous, blocking way.
 * @author Laurent Cohen
 */
public class DefaultFilePersistenceManager implements JobPersistence<String>
{
  /**
   * The default prefix for the files in the store.
   */
  public static final String DEFAULT_PREFIX = "job";
  /**
   * The default extension for the files in the store.
   */
  public static final String DEFAULT_EXT = ".ser";
  /**
   * The root path for this persistence manager, where the persisted jobs are stored.
   */
  private final File root;
  /**
   * The object used to serialize and deserialize the jobs.
   */
  private final ObjectSerializerImpl serializer = new ObjectSerializerImpl();
  /**
   * The prefix for the names of the files in the store.
   */
  private final String prefix;
  /**
   * The extension for the names of the files in the store.
   */
  private final String ext;

  /**
   * Initialize this persistence manager with the specified root path, using {@link #DEFAULT_PREFIX DEFAULT_PREFIX}
   * as the files prefix and {@link #DEFAULT_EXT DEFAULT_EXT} as the extension.
   * If the root does not exist, the persistence manager attempts to create it.
   * @param root the root path for this persistence manager, where the persisted jobs are stored.
   * @throws IllegalArgumentException if the root path is invalid or could not be created.
   */
  public DefaultFilePersistenceManager(final File root)
  {
    this(root, DEFAULT_PREFIX, DEFAULT_EXT);
  }

  /**
   * Initialize this persistence manager with the specified root path, file prefix and extension.
   * If the root does not exist, the persistence manager attempts to create it.
   * @param root the root path for this persistence manager, where the persisted jobs are stored.
   * @param prefix the prefix for the names of the files in the store.
   * @param ext the extension for the names of the files in the store.
   * @throws IllegalArgumentException if the root path is invalid or could not be created.
   */
  public DefaultFilePersistenceManager(final File root, final String prefix, final String ext)
  {
    if (root == null) throw new NullPointerException("the root path cannot be null");
    this.root = initialize(root);
    this.prefix = (prefix == null) || "".equals(prefix) ? DEFAULT_PREFIX : prefix;
    this.ext = (ext == null) || "".equals(ext) ? null : ext;
  }

  /**
   * Initialize this persistence manager with the specified root path, using &quot;job&quot;
   * as the files prefix and &quot;.ser&quot; as the extension.
   * @param root the root path for this persistence manager, where the persisted jobs are stored.
   * @throws IllegalArgumentException if the root path is invalid or could not be created.
   */
  public DefaultFilePersistenceManager(final String root)
  {
    this(root, DEFAULT_PREFIX, DEFAULT_EXT);
  }

  /**
   * Initialize this persistence manager with the specified root path.
   * @param root the root path for this persistence manager, where the persisted jobs are stored.
   * @param prefix the prefix for the names of the files in the store.
   * @param ext the extension for the names of the files in the store.
   * @throws IllegalArgumentException if the root path is invalid or could not be created.
   */
  public DefaultFilePersistenceManager(final String root, final String prefix, final String ext)
  {
    if (root == null) throw new IllegalArgumentException("the root path cannot be null");
    this.root = initialize(new File(root));
    this.prefix = (prefix == null) || "".equals(prefix) ? DEFAULT_PREFIX : prefix;
    this.ext = (ext == null) || "".equals(ext) ? null : ext;
  }

  /**
   * Initialize this persistence manager with the specified root path.
   * @param root the root path for this persistence manager, where the persisted jobs are stored.
   * @return the root path if it is valid.
   * @throws IllegalArgumentException if the root path is invalid or could not be created.
   */
  private File initialize(final File root)
  {
    if (!root.exists())
    {
      if (!root.mkdirs()) throw new IllegalArgumentException("root path " + root + " could not be created");
    }
    else if (!root.isDirectory()) throw new IllegalArgumentException("root path '" + root.getPath() + "' is not a directory");
    return root;
  }

  /**
   * Compute the key assigned ot the specified job.
   * @param job the job for which to get a key.
   * @return the job's UUID.
   */
  @Override
  public String computeKey(final JPPFJob job)
  {
    return job.getUuid();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized Collection<String> allKeys() throws JobPersistenceException
  {
    File files[] = root.listFiles(new FileFilter()
    {
      @Override
      public boolean accept(final File path)
      {
        if (path.isDirectory()) return false;
        String name = path.getName();
        if (ext == null) return name.startsWith(prefix);
        return name.endsWith(ext) && name.startsWith(prefix);
      }
    });
    List<String> result = new ArrayList<String>(files.length);
    for (File f: files)
    {
      String s = f.getName().substring(prefix.length());
      if (ext != null)
      {
        int idx = s.lastIndexOf(ext);
        s = s.substring(0, idx);
      }
      result.add(s);
    }
    return result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized JPPFJob loadJob(final String key) throws JobPersistenceException
  {
    InputStream is = null;
    try
    {
      is = new BufferedInputStream(new FileInputStream(fileFromKey(key)));
      JPPFJob job = (JPPFJob) serializer.deserialize(is, false);
      int n = 0;
      byte bytes[] = new byte[4];
      int count = 0;
      while (n != -1)
      {
        n = is.read(bytes);
        if (n == -1) break;
        count += n;
        if (count < 4) continue;
        count = 0;
        int size = SerializationUtils.readInt(bytes, 0);
        List<JPPFTask> tasks = new ArrayList<JPPFTask>(size);
        for (int i=0; i<size; i++) tasks.add((JPPFTask) serializer.deserialize(is, false));
        job.getResults().putResults(tasks);
      }
      return job;
    }
    catch (Exception e)
    {
      throw new JobPersistenceException(e);
    }
    finally
    {
      if (is != null)
      {
        try
        {
          is.close();
        }
        catch (IOException e)
        {
          throw new JobPersistenceException(e);
        }
      }
    }
  }

  /**
   * Store the specified job. This implementation serializes the entire job to a file,
   * the first time it is called for a job. On subsequent calls, only the deltas are persisted.
   * @param key the key allowing to locate the job in the persistence store.
   * @param job the job to store.
   * @param tasks the newly received completed tasks, may be used to only store the delta for better performance.
   * @throws JobPersistenceException if any error occurs while storing the job.
   * @see org.jppf.client.persistence.JobPersistence#storeJob(Object, JPPFJob, List)
   */
  @Override
  public synchronized void storeJob(final String key, final JPPFJob job, final List<JPPFTask> tasks) throws JobPersistenceException
  {
    OutputStream os = null;
    try
    {
      File file = fileFromKey(key);
      boolean isNewFile = !file.exists();
      if (isNewFile)
      {
        os = new BufferedOutputStream(new FileOutputStream(file));
        serializer.serialize(job, os);
      }
      else if (!tasks.isEmpty())
      {
        os = new BufferedOutputStream(new FileOutputStream(file, true));
        SerializationUtils.writeInt(tasks.size(), os);
        for (JPPFTask task: tasks) serializer.serialize(task, os);
      }
    }
    catch (Exception e)
    {
      throw new JobPersistenceException(e);
    }
    finally
    {
      if (os != null)
      {
        try
        {
          os.close();
        }
        catch (IOException e)
        {
          throw new JobPersistenceException(e);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public synchronized void deleteJob(final String key) throws JobPersistenceException
  {
    File file = fileFromKey(key);
    if (!file.delete()) throw new JobPersistenceException("could not delete job with key '" + key + '\'');
  }

  /**
   * Generate a file path from the specified job key.
   * @param key the job key to use.
   * @return a file path built form the given key.
   */
  private File fileFromKey(final String key)
  {
    return new File(root, prefix + key + (ext == null ? "" : ext));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName()).append('[');
    sb.append("root=").append(root.getPath());
    sb.append(", file prefix=").append(prefix);
    sb.append(", file extension=").append(ext == null ? "none" : ext);
    sb.append(']');
    return sb.toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void close()
  {
  }
}
