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

package org.jppf.management;

import java.io.File;
import java.net.URL;

import org.jppf.classloader.AbstractJPPFClassLoader;
import org.jppf.node.Node;
import org.jppf.node.event.*;
import org.jppf.node.protocol.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * This NodeLifeCycleListener implementations is used to update the node state
 * maintained by the node management MBean.
 * @author Laurent Cohen
 * @exclude
 */
public class NodeStatusNotifier extends DefaultLifeCycleErrorHandler implements NodeLifeCycleListener
{
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(NodeStatusNotifier.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static final boolean debugEnabled = log.isDebugEnabled();
  /**
   * Base name used for localization lookups.
   */
  private static final String I18N_BASE = "org.jppf.server.i18n.messages";
  /**
   * The mbean that provides information on the node's state.
   */
  private final JPPFNodeAdmin nodeAdmin;

  /**
   * Initialize this notifier with the specified node admin mbean.
   * @param nodeAdmin the mbean that provides information on the node's state.
   */
  public NodeStatusNotifier(final JPPFNodeAdmin nodeAdmin)
  {
    this.nodeAdmin = nodeAdmin;
  }

  @Override
  public void nodeStarting(final NodeLifeCycleEvent event)
  {
    synchronized(nodeAdmin)
    {
      nodeAdmin.getNodeState().setConnectionStatus(JPPFNodeState.ConnectionState.CONNECTED);
    }
  }

  @Override
  public void nodeEnding(final NodeLifeCycleEvent event)
  {
    synchronized(nodeAdmin)
    {
      nodeAdmin.getNodeState().setConnectionStatus(JPPFNodeState.ConnectionState.DISCONNECTED);
    }
  }

  @Override
  public void jobStarting(final NodeLifeCycleEvent event)
  {
    synchronized(nodeAdmin)
    {
      nodeAdmin.getNodeState().setExecutionStatus(JPPFNodeState.ExecutionState.EXECUTING);
    }
  }

  @Override
  public void jobEnding(final NodeLifeCycleEvent event)
  {
    synchronized(nodeAdmin)
    {
      nodeAdmin.getNodeState().setExecutionStatus(JPPFNodeState.ExecutionState.IDLE);
      int n = event.getTasks().size();
      n += nodeAdmin.getNodeState().getNbTasksExecuted();
      try
      {
        nodeAdmin.setTaskCounter(n);
      }
      catch (Exception e)
      {
        e.printStackTrace();
      }
    }
  }

  /**
   * Get a localized message given its unique name and the current locale.
   * @param message the unique name of the localized message.
   * @return a message in the current locale, or the default locale
   * if the localization for the current locale is not found.
   */
  private static String localize(final String message)
  {
    return LocalizationUtils.getLocalized(I18N_BASE, message);
  }

  @Override
  public void jobHeaderLoaded(final NodeLifeCycleEvent event)
  {
    if (log.isTraceEnabled()) log.trace(StringUtils.printClassLoaderHierarchy(event.getTaskClassLoader()));
    ClassPath classpath = event.getJob().getSLA().getClassPath();
    Node node = event.getNode();
    if ((classpath != null) && !classpath.isEmpty() && node.isOffline())
    {
      AbstractJPPFClassLoader cl = node.resetTaskClassLoader();
      for (ClassPathElement elt: classpath)
      {
        boolean validated = false;
        try
        {
          validated = elt.validate();
        }
        catch (Throwable t)
        {
          String format = "exception occurred during validation of classpath element '{}' : {}";
          if (debugEnabled) log.debug(format, elt, ExceptionUtils.getStackTrace(t));
          else log.warn(format, elt, ExceptionUtils.getMessage(t));
        }
        if (!validated) continue;
        URL url = null;
        Location location = elt.getLocation();
        try
        {
          if (location instanceof MemoryLocation)
          {
            cl.getResourceCache().registerResource(elt.getName(), location);
            url = cl.getResourceCache().getResourceURL(elt.getName());
          }
          else if (location instanceof FileLocation)
          {
            File file = new File(((FileLocation) location).getPath());
            if (file.exists()) url = file.toURI().toURL();
          }
          else if (location instanceof URLLocation) url = ((URLLocation) location).getPath();
        }
        catch (Exception e)
        {
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
