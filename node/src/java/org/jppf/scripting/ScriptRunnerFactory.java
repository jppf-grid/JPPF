/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

import org.slf4j.*;

/**
 * Factory used to instantiate script runners.
 * @author Laurent Cohen
 */
public final class ScriptRunnerFactory {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptRunnerFactory.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Mapping of languages to associated pool of engines.
   */
  private static final Map<String, ScriptRunnerPool> runnerMap = new HashMap<>();

  /**
   * Instantiation of this class is not allowed.
   */
  private ScriptRunnerFactory() {
  }

  /**
   * Get a new script runner instance based on the specified script language.
   * @param language the name of the script language to use.
   * @return A <code>ScriptRunner</code> instance, or null if no known script engine
   * exists for the specified language.
   */
  public static ScriptRunner getScriptRunner(final String language) {
    if (language == null) return null;
    ScriptRunnerPool pool = null;
    synchronized(runnerMap) {
      pool = runnerMap.get(language);
      if (pool == null) {
        pool = new ScriptRunnerPool(language);
        runnerMap.put(language, pool);
      }
      return pool == null ? null : pool.get();
    }
  }

  /**
   * Release the specified runner back into its pool when no longer used.
   * @param runner the script runner to release.
   */
  public static void releaseScriptRunner(final ScriptRunner runner) {
    if (runner != null) {
      ScriptRunnerPool pool = null;
      synchronized(runnerMap) {
        pool = runnerMap.get(runner.getLanguage());
      }
      if (pool != null) pool.put(runner);
    }
  }
}
