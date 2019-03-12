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

package org.jppf.node.event;

import java.io.File;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.location.*;
import org.jppf.node.Node;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This NodeLifeCycleListener implementations parses the classpath associated with a job
 * and adds its elemenents to the task class loader.
 * @author Laurent Cohen
 * @exclude
 */
public class JobClassPathHandler extends NodeLifeCycleListenerAdapter {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(JobClassPathHandler.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * 
   */
  private static final AtomicLong SEQUENCE = new AtomicLong(0L);

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
    final ClassPath classpath = event.getJob().getSLA().getClassPath();
    if (classpath == null) return;
    final Node node = event.getNode();
    if (node.isAndroid()) return;
    if (log.isTraceEnabled()) log.trace(StringUtils.printClassLoaderHierarchy(event.getTaskClassLoader()));
    AbstractJPPFClassLoader cl = event.getTaskClassLoader();
    if (classpath.isForceClassLoaderReset() || !classpath.isEmpty()) cl = (AbstractJPPFClassLoader) node.resetTaskClassLoader(event.getJob());
    if (!classpath.isEmpty()) {
      for (final ClassPathElement elt: classpath) {
        boolean validated = false;
        try {
          validated = elt.validate();
        } catch (final Throwable t) {
          final String format = "exception occurred during validation of classpath element '{}' : {}";
          if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(t));
          else log.warn(format, elt, ExceptionUtils.getMessage(t));
        }
        if (!validated) continue;
        URL url = null;
        final Location<?> source = elt.getSourceLocation();
        final Location<?> target = elt.getTargetLocation();
        if (debugEnabled) log.debug("processing classpath element with [source = {}; target = {}]", source, target);
        try {
          if (target != source) {
            final String path = getFilePath(target);
            if ((path == null) || (elt.isCopyToExistingFile() || !new File(path).exists())) {
              if (debugEnabled) log.debug("copying {} to {}", source, target);
              source.copyTo(target);
            }
          }
          if (target instanceof MemoryLocation) {
            final String name = Long.toString(SEQUENCE.incrementAndGet());
            cl.getResourceCache().registerResource(name, target);
            url = cl.getResourceCache().getResourceURL(name);
          } else if (target instanceof FileLocation) {
            final File file = new File(((FileLocation) target).getPath());
            if (file.exists()) url = file.toURI().toURL();
          } else if (target instanceof URLLocation) url = ((URLLocation) target).getPath();
        } catch (final Exception e) {
          final String format = "exception occurred during processing of classpath element '{}' : {}";
          if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(e));
          else log.warn(format, elt, ExceptionUtils.getMessage(e));
        }
        if (url != null) cl.addURL(url);
      }
      //classpath.clear();
    }
  }

  /**
   * Get the file path of the specified location, if applicable.
   * @param location the location to check.
   * @return the file path, or {@code null} if the location does not point to a file.
   */
  private static String getFilePath(final Location<?> location) {
    if (location instanceof FileLocation) return ((FileLocation) location).getPath();
    if (location instanceof URLLocation) {
      final URL url = ((URLLocation) location).getPath();
      if ("file".equalsIgnoreCase(url.getProtocol())) return url.getPath();
    }
    return null;
  }
}
