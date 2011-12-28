/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.node.initialization;

import java.util.*;

import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Instances of this class load and invoke node initialization hooks defined via their SPI definition.
 * @author Laurent Cohen
 */
public class InitializationHooksHandler
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(InitializationHooksHandler.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The list of hooks loaded via the SPI mechanism.
   */
  private final List<InitializationHook> hooks = new LinkedList<InitializationHook>();
  /**
   * The initial configuration, such as read from the config file or configuration input source.
   */
  private final UnmodifiableTypedProperties initialConfiguration;

  /**
   * Initialize this hooks handler with the initial JPPF configuration.
   * @param initialConfiguration the initial configuration, such as read from the config file or configuration input source.
   */
  public InitializationHooksHandler(final TypedProperties initialConfiguration)
  {
    this.initialConfiguration = new UnmodifiableTypedProperties(initialConfiguration);
  }

  /**
   * Load all initialization hooks found in the class path via a service definition.
   */
  public void loadHooks()
  {
    Iterator<InitializationHook> it = ServiceFinder.lookupProviders(InitializationHook.class);
    while (it.hasNext())
    {
      try
      {
        InitializationHook hook = it.next();
        hooks.add(hook);
        if (debugEnabled) log.debug("successfully added initialization hook " + hook.getClass().getName());
      }
      catch(Error e)
      {
        log.error(e.getMessage(), e);
      }
    }
  }
 
  /**
   * Call the <code>initializing()</code> method of each hook.
   * Any exception is caught and logged.
   */
  public void callHooks()
  {
    for (InitializationHook hook: hooks)
    {
      try
      {
        hook.initializing(initialConfiguration);
      }
      catch (Exception e)
      {
        log.error("exception caught when running initialization hook '" + hook.getClass().getName() + "' : ", e);
      }
    }
  }
}
