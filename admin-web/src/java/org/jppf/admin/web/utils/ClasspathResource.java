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

package org.jppf.admin.web.utils;

import java.io.*;

import org.apache.wicket.request.resource.ResourceStreamResource;
import org.apache.wicket.request.resource.caching.IResourceCachingStrategy;
import org.apache.wicket.util.resource.*;
import org.apache.wicket.util.time.Time;

/**
 * A resource loaded from the classpath.
 * @author Laurent Cohen
 */
public class ClasspathResource extends ResourceStreamResource {
  /**
   * Path to the resource in the classpath.
   */
  private final String path;

  /**
   * @param path the path to the resource in the classpath.
   */
  public ClasspathResource(final String path) {
    this.path = path;
    setFileName(new File(path).getName());
  }

  @Override
  protected IResourceStream getResourceStream() {
    return new ClasspathResourceStream(path);
  }

  @Override
  protected IResourceCachingStrategy getCachingStrategy() {
    return super.getCachingStrategy();
  }

  /**
   * Resource stream associated with a {@link ClasspathResource}.
   */
  public static class ClasspathResourceStream extends AbstractResourceStream {
    /**
     * Path to the resource in the classpath.
     */
    private final String path;
    /**
     * Last modified date, equal to the creation date.
     */
    private final Time lastModified = Time.millis(System.currentTimeMillis());

    /**
     * @param path the path to the resource in the classpath.
     */
    public ClasspathResourceStream(final String path) {
      this.path = path;
    }

    @Override
    public InputStream getInputStream() throws ResourceStreamNotFoundException {
      final InputStream is = getClass().getClassLoader().getResourceAsStream(path);
      if (is == null) throw new ResourceStreamNotFoundException();
      return is;
    }

    @Override
    public void close() throws IOException {
    }

    @Override
    public Time lastModifiedTime() {
      return lastModified;
    }
  }
}
