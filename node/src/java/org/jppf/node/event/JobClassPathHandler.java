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

package org.jppf.node.event;

import java.io.File;
import java.net.URL;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.location.*;
import org.jppf.node.Node;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This NodeLifeCycleListener implementations parses the classpath associated with a job
 *  and add its elemenents to the task class laoder.
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

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event) {
    Thread.setDefaultUncaughtExceptionHandler(new JPPFDefaultUncaughtExceptionHandler());
    Node node = event.getNode();
    if (!node.isOffline()) return;
    ClassPath classpath = event.getJob().getSLA().getClassPath();
    if (node.isAndroid()) {
      //node.resetTaskClassLoader(classpath);
    } else {
      if (log.isTraceEnabled()) log.trace(StringUtils.printClassLoaderHierarchy(event.getTaskClassLoader()));
      AbstractJPPFClassLoader cl = event.getTaskClassLoader();
      if ((classpath != null) && (classpath.isForceClassLoaderReset() || !classpath.isEmpty())) cl = node.resetTaskClassLoader();
      if ((classpath != null) && !classpath.isEmpty()) {
        for (ClassPathElement elt: classpath) {
          boolean validated = false;
          try {
            validated = elt.validate();
          } catch (Throwable t) {
            String format = "exception occurred during validation of classpath element '{}' : {}";
            if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(t));
            else log.warn(format, elt, ExceptionUtils.getMessage(t));
          }
          if (!validated) continue;
          URL url = null;
          Location<?> local = elt.getLocalLocation();
          Location<?> remote = elt.getRemoteLocation();
          try {
            if (remote != local) local.copyTo(remote);
            if (remote instanceof MemoryLocation) {
              cl.getResourceCache().registerResource(elt.getName(), remote);
              url = cl.getResourceCache().getResourceURL(elt.getName());
            } else if (remote instanceof FileLocation) {
              File file = new File(((FileLocation) remote).getPath());
              if (file.exists()) url = file.toURI().toURL();
            } else if (remote instanceof URLLocation) url = ((URLLocation) remote).getPath();
          } catch (Exception e) {
            String format = "exception occurred during processing of classpath element '{}' : {}";
            if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(e));
            else log.warn(format, elt, ExceptionUtils.getMessage(e));
          }
          if (url != null) cl.addURL(url);
        }
        classpath.clear();
      }
    }
  }
}
