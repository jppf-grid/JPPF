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
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Specfications for the script to execute.
   */
  protected ScriptDefinition scriptSpec;

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
    this.scriptSpec = new ScriptDefinition(language, script, reusableId, bindings);
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
    this.scriptSpec = new ScriptDefinition(language, FileUtils.readTextFile(scriptReader), reusableId, bindings);
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
    this.scriptSpec = new ScriptDefinition(language, FileUtils.readTextFile(scriptFile), reusableId, bindings);
  }

  @Override
  public void run() {
    try {
      final Map<String, Object> bnd = new HashMap<>();
      bnd.put("jppfTask", this);
      @SuppressWarnings("unchecked")
      final T result = (T) scriptSpec.evaluate(bnd);
      // may have been set from the script
      if ((getResult() == null) && (result != null)) setResult(result);
    } catch(final JPPFScriptingException e) {
      setThrowable(e);
    }
  }

  /**
   * Get the JSR 223 script language to use.
   * @return the script language a s a script.
   */
  public String getLanguage() {
    return scriptSpec.getLanguage();
  }

  /**
   * Get the script to execute from this task.
   * @return the script source as a string.
   */
  public String getScript() {
    return scriptSpec.getScript();
  }

  /**
   * Get the unique identifier for the script.
   * @return the identifier as a string.
   */
  public String getReusableId() {
    return scriptSpec.getId();
  }

  /**
   * Get the user-defined variable bindings.
   * @return a map of variable names to their value.
   */
  public Map<String, Object> getBindings() {
    return scriptSpec.getBindings();
  }

  /**
   * Add the specified variable to the user-defined bindings.
   * @param name the name of the variable binding to add.
   * @param value the value of the variable.
   * @return this task, for method chaining.
   */
  public ScriptedTask<T> addBinding(final String name, final Object value) {
    scriptSpec.addBinding(name, value);
    return this;
  }

  /**
   * Remove the specified variable from the user-defined bindings.
   * @param name the name of the variable binding to remove.
   * @return the value of the variable, or {@code null} if the variable was not bound.
   */
  public Object removeBinding(final String name) {
    return scriptSpec.removeBinding(name);
  }
}
