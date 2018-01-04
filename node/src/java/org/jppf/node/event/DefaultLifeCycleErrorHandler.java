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

import static org.jppf.node.event.NodeLifeCycleEventType.*;

import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * The default node life cycle error handler, used when the life cycle listener implementation does not implement {@link NodeLifeCycleErrorHandler}.
 * @author Laurent Cohen
 */
public class DefaultLifeCycleErrorHandler implements NodeLifeCycleErrorHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(LifeCycleEventHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * If <code>true</code> (the default), then {@link Error}s caught in the listeners methods will be propagated, otherwise they will be just logged.
   */
  protected static final boolean propagateErrors = JPPFConfiguration.getProperties().getBoolean("jppf.node.listener.errors.propagate", true);
  /**
   * Mapping of event type to listener method name.
   */
  public static final Map<NodeLifeCycleEventType, String> methodsNamesMap = generateMethodsNamesMap();

  /**
   * {@inheritDoc}
   * <p>
   * This method logs the uncaught throwable, with a level of detail based on the configured logging level. If debug level is enabled, then the full stack trace will be logged, otherwise only the
   * throwable class and its message will be logged.
   * <p>
   * if the value of the boolean configuration property &quot;jppf.node.listener.errors.propagate&quot; is true, and if the throwable is an {@link Error}, then it will be propagated up the call stack.
   */
  @Override
  public void handleError(final NodeLifeCycleListener listener, final NodeLifeCycleEvent event, final Throwable t) {
    final String s = StringUtils.build("error executing ", methodsNamesMap.get(event.getType()), " on an instance of ", listener.getClass(), ", event=", event, ", listener=", listener, " : ");
    if (debugEnabled) log.debug(s, t);
    else log.error(s + ExceptionUtils.getMessage(t));
    if (propagateErrors && (t instanceof Error)) throw (Error) t;
  }

  /**
   * Generate a mapping of event type to listener method name.
   * @return a map of {@link NodeLifeCycleEventType} to associated strings.
   */
  private static Map<NodeLifeCycleEventType, String> generateMethodsNamesMap() {
    final Map<NodeLifeCycleEventType, String> map = new HashMap<>();
    map.put(NODE_STARTING, "nodeStarting()");
    map.put(NODE_ENDING, "nodeEnding()");
    map.put(JOB_HEADER_LOADED, "jobHeaderLoaded()");
    map.put(JOB_STARTING, "jobStarting()");
    map.put(JOB_ENDING, "jobEnding()");
    map.put(BEFORE_NEXT_JOB, "beforeNextJob()");
    return Collections.unmodifiableMap(map);
  }
}
