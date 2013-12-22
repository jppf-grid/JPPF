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
package org.jppf.scripting;

import java.util.*;

import org.jppf.utils.*;
import org.jppf.utils.collections.*;
import org.slf4j.*;

/**
 * Factory used to instantiate script runners.
 * @author Laurent Cohen
 */
public final class ScriptRunnerFactory
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptRunnerFactory.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * An association of script languages with the corresponding discovered runners.
   */
  private static final CollectionMap<String, ScriptRunner> runners = loadRunners();

  /**
   * Instantiation of this class is not allowed.
   */
  private ScriptRunnerFactory()
  {
  }

  /**
   * Instantiate a script runner based on the specified script language.
   * @param language the name of the script language to use.
   * @return A <code>ScriptRunner</code> instance, or null if no known script runner
   * exists for the specified language.
   * @deprecated this method is not actually creating a script runner, but providing a cached instance.
   * {@link #getScriptRunner(String)} should be used instead for semantic consistency. 
   */
  public static ScriptRunner makeScriptRunner(final String language)
  {
    return getScriptRunner(language);
  }

  /**
   * Get a script runner based on the specified script language.
   * @param language the name of the script language to use.
   * @return A <code>ScriptRunner</code> instance, or null if no known script runner
   * exists for the specified language.
   */
  public static ScriptRunner getScriptRunner(final String language)
  {
    Collection<ScriptRunner> c = runners.getValues(language);
    if (c == null) return null;
    return c.iterator().next();
  }

  /**
   * Load all configured script runner via SPI.
   * @return a mapping of language names to script runners.
   */
  private static CollectionMap<String, ScriptRunner> loadRunners()
  {
    CollectionMap<String, ScriptRunner> map = new SetHashMap<>();
    try
    {
      ServiceFinder sf = new ServiceFinder();
      List<ScriptRunner> list = sf.findProviders(ScriptRunner.class);
      for (ScriptRunner r: list) map.putValue(r.getLanguage(), r);
    }
    catch (Exception e)
    {
      log.error("error loading script runners via SPI: {}", ExceptionUtils.getStackTrace(e));
    }
    return map;
  }
}
