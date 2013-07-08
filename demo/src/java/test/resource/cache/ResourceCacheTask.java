/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package test.resource.cache;

import java.io.*;

import org.jppf.server.protocol.JPPFTask;
import org.jppf.utils.FileUtils;

/**
 * This class is a template for a standard JPPF task.
 * @author Laurent Cohen
 */
public class ResourceCacheTask extends JPPFTask
{
  /**
   * The duration of this task.
   */
  private final long duration;
  /**
   * 
   */
  private final String resourceName;

  /**
   * Default constructor,
   * @param duration the duration of this task.
   * @param resourceName .
   */
  public ResourceCacheTask(final long duration, final String resourceName)
  {
    this.duration = duration;
    this.resourceName = resourceName;
  }

  @Override
  public void run()
  {
    try
    {
      if (duration >= 1L) Thread.sleep(duration);
      String s = null;
      if (resourceName != null)
      {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
        s = new String(FileUtils.readTextFile(new InputStreamReader(is)));
      }
      setResult("the execution was performed successfully: " + s);
      System.out.print(s);
    }
    catch(Exception e)
    {
      setException(e);
    }
  }
}
