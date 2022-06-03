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

package org.jppf.scripting;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.script.*;

import org.jppf.utils.*;
import org.jppf.utils.collections.SoftReferenceValuesMap;
import org.slf4j.*;

/**
 * ScriptRunner wrapper around a JSR-223 compliant script engine.
 * @author Laurent Cohen
 * @exclude
 */
class ScriptRunnerImpl implements ScriptRunner {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptRunnerImpl.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Map of precompiled scripts.
   */
  protected final Map<String, CompiledScript> map = new SoftReferenceValuesMap<>();
  /**
   * The script engine provided by the {@code javax.script} APIs.
   */
  protected ScriptEngine engine;
  /**
   * Flag inidicating whether the engine lookup has already failed.
   */
  protected boolean engineNotFound;
  /**
   * The language supported by this script engine.
   */
  protected final String language;
  /**
   * 
   */
  private final AtomicInteger cacheHits = new AtomicInteger(0);
  /**
   * 
   */
  private final AtomicInteger cacheMisses = new AtomicInteger(0);
  /**
   * 
   */
  private final AtomicInteger cacheRequests = new AtomicInteger(0);

  /**
   * Create a script runner witht he specified language.
   * @param language the language supported by this script engine.
   * @throws JPPFScriptingException if the engine oculd not be created.
   */
  ScriptRunnerImpl(final String language) throws JPPFScriptingException {
    this.language = language;
    this.engine = createEngine();
  }

  /**
   * Get the script engine according tot he given name.
   * The engine is lazily instantiated upon the first invocation of this method.
   * @return a {@link ScriptEngine} instance.
   * @throws JPPFScriptingException if the engine oculd not be created.
   */
  private ScriptEngine createEngine () throws JPPFScriptingException {
    final ScriptEngine engine = new ScriptEngineManager().getEngineByName(language);
    if (engine == null) {
      engineNotFound = true;
      throw new JPPFScriptingException("an engine could not be instantiated for script language '" + language + "'");
    }
    return engine;
  }

  @Override
  public Object evaluate(final String script, final Map<String, Object> variables) throws JPPFScriptingException {
    return evaluate(null, script, variables);
  }

  @Override
  public Object evaluate(final String scriptId, final String script, final Map<String, Object> variables) throws JPPFScriptingException {
    if (engine == null) return null;
    final Bindings bindings = engine.createBindings();
    if (variables != null) bindings.putAll(variables);
    CompiledScript cs = null;
    if ((scriptId != null) && (engine instanceof Compilable)) {
      final String key = new StringBuilder().append(language).append(':').append(scriptId).toString();
      cs = map.get(key);
      cacheRequests.incrementAndGet();
      if (cs == null) {
        cacheMisses.incrementAndGet();
        try {
          cs = ((Compilable) engine).compile(script);
          if (cs != null) map.put(key, cs);
        } catch (final Exception e) {
          throw buildScriptingException(e);
        }
      } else cacheHits.incrementAndGet();
    }
    if (debugEnabled) log.debug("script cache statistics for '{}': requests={}, hits={}, misses={}", language, cacheRequests.get(), cacheHits.get(), cacheMisses.get());
    try {
      final Object res = (cs != null) ? cs.eval(bindings) : engine.eval(script, bindings);
      return res;
    } catch(final Exception e) {
      throw buildScriptingException(e);
    }
  }

  @Override
  public void init() {
  }

  @Override
  public void cleanup() {
    map.clear();
  }

  @Override
  public String getLanguage() {
    return language;
  }

  /**
   * Build an exception from a throwable raised by the script engine.<br>
   * I noticed that the Rhino engine throws {@code EcmaError}s which are not serializable.
   * This causes problems if the EcmaError is set as a JPPF task's throwable via {@code Task.setThrowable(Throwable)}.
   * @param t the exception from the script engine.
   * @return a {@link JPPFScriptingException} instance.
   */
  private static JPPFScriptingException buildScriptingException(final Throwable t) {
    final JPPFScriptingException jfe = new JPPFScriptingException(ExceptionUtils.getMessage(t));
    jfe.setStackTrace(t.getStackTrace());
    return jfe;
  }
}
