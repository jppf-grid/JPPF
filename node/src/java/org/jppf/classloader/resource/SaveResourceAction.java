/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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
package org.jppf.classloader.resource;

import java.io.File;
import java.security.PrivilegedAction;
import java.util.List;

import org.jppf.location.*;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;

/**
 * Privileged action wrapper for saving a resource definition to a temporary file.
 * @exclude
 */
public class SaveResourceAction implements PrivilegedAction<Location>
{
  /**
   * Indicates storage is on the file system, with fallback to memory storage.
   */
  private static final String FILE_STORAGE = "file";
  /**
   * Indicates storage is in memory, with no fallback.
   */
  private static final String MEMORY_STORAGE = "memory";
  /**
   * Determines whether resources should be stored in memory.
   */
  private static final boolean IS_MEMORY_STORAGE = isMemoryStorageType();
  /**
   * The name of the temp folder in which to save the file.
   */
  private List<String> tmpDirs = null;
  /**
   * The original name of the resource to find.
   */
  private String name = null;
  /**
   * The resource definition to save.
   */
  private final byte[] definition;
  /**
   * An eventually resulting exception.
   */
  private Exception exception = null;

  /**
   * Initialize this action with the specified resource definition.
   * @param tmpDirs the name of the temp folder in which to save the file.
   * @param name the original name of the resource to find.
   * @param definition the resource definition to save.
   */
  public SaveResourceAction(final List<String> tmpDirs, final String name, final byte[] definition)
  {
    this.tmpDirs = tmpDirs;
    this.name = name;
    this.definition = definition;
  }

  @Override
  public Location run()
  {
    Location resource = null;
    if (!IS_MEMORY_STORAGE) resource = saveToFileResource();
    if (resource == null) resource = saveToMemoryResource();
    return resource;
  }

  /**
   * Save the resource to a temporary file.
   * @return an instance of {@link FileResource}.
   */
  private Location saveToFileResource()
  {
    Location resource = null;
    File tmp = null;
    try
    {
      for (String s: tmpDirs)
      {
        File f = new File(s, name);
        if (!f.exists())
        {
          tmp = f;
          break;
        }
      }
      if (tmp == null)
      {
        String dir = tmpDirs.get(0) + '_' + tmpDirs.size();
        File f = new File(dir + File.separator);
        FileUtils.mkdirs(f);
        f.deleteOnExit();
        tmp = new File(f, name);
        tmpDirs.add(dir);
      }
      FileUtils.mkdirs(tmp);
      tmp.deleteOnExit();
      FileUtils.writeBytesToFile(definition, tmp);
      resource = new FileLocation(tmp);
      exception = null;
    }
    catch(Exception e)
    {
      exception = e;
      if ((tmp != null) && tmp.exists())
      {
        tmp.delete();
        tmp = null;
      }
    }
    return resource;
  }

  /**
   * Save the resource to memory.
   * @return an instance of {@link MemoryResource}.
   */
  private Location saveToMemoryResource()
  {
    Location resource = null;
    try
    {
      resource = new MemoryLocation(definition);
      exception = null;
    }
    catch (Exception e)
    {
      exception = e;
    }
    return resource;
  }

  /**
   * Get the resulting exception.
   * @return an <code>Exception</code> or null if no exception was raised.
   */
  public Exception getException()
  {
    return exception;
  }

  /**
   * Determine if resources should be stored in memory.
   * @return <code>true</code> if resources should be stored in memory, <code>false</code> otherwise.
   */
  private static boolean isMemoryStorageType()
  {
    String s = JPPFConfiguration.get(JPPFProperties.RESOURCE_CACHE_STORAGE);
    return MEMORY_STORAGE.equalsIgnoreCase(s);
  }
}
