/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.node.protocol;

import java.io.*;
import java.util.*;

import org.jppf.scripting.*;
import org.jppf.utils.FileUtils;

/**
 * A task implementation which executes a script in a specified JSR 223 ({@code javax.script} APIs) script language.
 * <p>In addition to the user-specified bindings, this task always provides to the script engine a reference to itself
 * with the name 'jppfTask'.
 * <p>A reusableId can be specified via the constructors, indicating that, if the script engine has that capability,
 * compiled scripts will be stored and reused, to avoid compiling the same scripts repeatedly.
 * @param <T> the type of result returned by this task.
 * @author Laurent Cohen
 */
public class ScriptedTask<T> extends AbstractTask<T> {
  /**
   * The JSR 223 script language to use.
   */
  protected String language;
  /**
   * The script to execute from this task.
   */
  protected String script;
  /**
   * Unique identifier for the script, which allows reusing a compiled version if one has already been produced.
   */
  protected String reusableId;
  /**
   * The bindings provide variables available to the script engine during execution of the script.
   */
  protected Map<String, Object> bindings = new HashMap<>();

  /**
   * Default constructor provided as a convenience for subclassing.
   */
  protected ScriptedTask() {
  }

  /**
   * Initialize this task with the specified script language, script provided as a string,
   * and a set of variable bindings to be used in the scripts.
   * @param language the JSR 223 script language to use.
   * @param script the script to execute from this task.
   * @param reusableId a unique identifier for the script, which allows reusing a compiled version if one has already been produced.
   * @param bindings a set of variables that will be available to the script.
   * @throws IllegalArgumentException if {@code language} or {@code script} is {@code null}.
   */
  public ScriptedTask(final String language, final String script, final String reusableId, final Map<String, Object> bindings) throws IllegalArgumentException {
    if (language == null) throw new IllegalArgumentException("the script language cannot be null");
    if (script == null) throw new IllegalArgumentException("the script source cannot be null");
    this.language = language;
    this.script = script;
    this.reusableId = reusableId;
    if (bindings != null) this.bindings.putAll(bindings);
  }

  /**
   * Initialize this task with the specified script language, script provided from a {@link Reader},
   * and a set of variable bindings to be used in the scripts.
   * @param language the JSR 223 script language to use.
   * @param scriptReader a reader form which to read the script source.
   * @param reusableId a unique identifier for the script, which allows reusing a compiled version if one has already been produced.
   * @param bindings a set of variables that will be available to the script.
   * @throws IllegalArgumentException if {@code language} or {@code script} is {@code null}.
   * @throws IOException if an I/O error occurs while reading the script source.
   */
  public ScriptedTask(final String language, final Reader scriptReader, final String reusableId, final Map<String, Object> bindings)
      throws IllegalArgumentException, IOException {
    this(language, FileUtils.readTextFile(scriptReader), reusableId, bindings);
  }

  /**
   * Initialize this task with the specified script language, script provided as a file,
   * and a set of variable bindings to be used in the scripts.
   * @param language the JSR 223 script language to use.
   * @param scriptFile a file from which to read the script source.
   * @param reusableId a unique identifier for the script, which allows reusing a compiled version if one has already been produced.
   * @param bindings a set of variables that will be available to the script.
   * @throws IllegalArgumentException if {@code language} or {@code script} is {@code null}.
   * @throws IOException if an I/O error occurs while reading the script source.
   */
  public ScriptedTask(final String language, final File scriptFile, final String reusableId, final Map<String, Object> bindings)
      throws IllegalArgumentException, IOException {
    this(language, FileUtils.readTextFile(scriptFile), reusableId, bindings);
  }

  @Override
  public void run() {
    ScriptRunner runner = null;
    try {
      runner = ScriptRunnerFactory.getScriptRunner(language);
      if (runner != null) {
        Map<String, Object> bnd = new HashMap<>(bindings);
        bnd.put("jppfTask", this);
        T result = (T) runner.evaluate(reusableId, script, bnd);
        if (result != null) {
          System.out.println("language=" + language + ", result=" + result + ", class=" + result.getClass().getName());
        }
        // may have been set from the script
        if ((getResult() == null) && (result != null)) setResult(result);
      }
    } catch(Exception e) {
      setThrowable(e);
    } finally {
      ScriptRunnerFactory.releaseScriptRunner(runner);
    }
  }

  /**
   * Get the JSR 223 script language to use.
   * @return the script language a s a script.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Get the script to execute from this task.
   * @return the script source as a string.
   */
  public String getScript() {
    return script;
  }

  /**
   * Get the unique identifier for the script.
   * @return the identifier as a string.
   */
  public String getReusableId() {
    return reusableId;
  }

  /**
   * Get the user-defined variable bindings.
   * @return a map of variable names to their value.
   */
  public Map<String, Object> getBindings() {
    return bindings;
  }

  /**
   * Add the specified variable to the user-defined bindings.
   * @param name the name of the variable binding to add.
   * @param value the value of the variable.
   */
  public void addBinding(final String name, final Object value) {
    bindings.put(name, value);
  }

  /**
   * Remove the specified variable from the user-defined bindings.
   * @param name the name of the variable binding to remove.
   * @return the value of the variable, or {@code null} if the variable was not bound.
   */
  public Object removeBinding(final String name) {
    return bindings.remove(name);
  }
}
