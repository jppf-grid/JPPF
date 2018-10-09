/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.io.*;
import java.util.*;

import org.jppf.utils.*;

/**
 * An easy to use wrapper around the JPPF scripting APIs.
 * @author Laurent Cohen
 */
public class ScriptDefinition implements Serializable {
  /**
   * The script language.
   */
  private final String language;
  /**
   * The script content as text.
   */
  private final String script;
  /**
   * Identifier which allows reusing the compiled script.
   */
  private final String id;
  /**
   * Optional variable bindings made available to the script at execution time.
   */
  private final Map<String, Object> bindings;

  /**
   * Initialize this object with the specified language and a script given as text.
   * @param language the script language.
   * @param script the script content as text.
   */
  public ScriptDefinition(final String language, final String script) {
    this(language, script, null, null);
  }

  /**
   * Initialize this object with the specified language, variable bindings and a script given as text.
   * @param language the script language.
   * @param script the script content as text.
   * @param bindings optional variable bindings made available to the script at execution time.
   */
  public ScriptDefinition(final String language, final String script, final Map<String, Object> bindings) {
    this(language, script, null, bindings);
  }

  /**
   * Initialize this object with the specified language and a script to read from a {@link Reader}.
   * @param language the script language.
   * @param reader a {@link Reader} from which to read the script content.
   * @throws IOException if the script content could not be read from the reader.
   */
  public ScriptDefinition(final String language, final Reader reader) throws IOException {
    this(language, FileUtils.readTextFile(reader), null, null);
  }

  /**
   * Initialize this object with the specified language, variable bindings and a script to read from a {@link Reader}.
   * @param language the script language.
   * @param reader a {@link Reader} from which to read the script content.
   * @param bindings optional variable bindings made available to the script at execution time.
   * @throws IOException if the script content could not be read from the reader.
   */
  public ScriptDefinition(final String language, final Reader reader, final Map<String, Object> bindings) throws IOException {
    this(language, FileUtils.readTextFile(reader), null, bindings);
  }

  /**
   * Initialize this object with the specified language and a script to read from a file.
   * @param language the script language.
   * @param file a file from which to read the script content.
   * @throws IOException if the script content could not be read from the reader.
   */
  public ScriptDefinition(final String language, final File file) throws IOException {
    this(language, FileUtils.readTextFile(file), null, null);
  }

  /**
   * Initialize this object with the specified language, variable bindings and a script to read from a file.
   * @param language the script language.
   * @param file a file from which to read the script content.
   * @param bindings optional variable bindings made available to the script at execution time.
   * @throws IOException if the script content could not be read from the file.
   */
  public ScriptDefinition(final String language, final File file, final Map<String, Object> bindings) throws IOException {
    this(language, FileUtils.readTextFile(file), null, bindings);
  }

  /**
   *
   * @param language the script language.
   * @param script the script content as text.
   * @param id an identifier which allows reusing the compiled script.
   * @param bindings optional variable bindings made available to the script at execution time.
   * @exclude
   */
  public ScriptDefinition(final String language, final String script, final String id, final Map<String, Object> bindings) {
    if (language == null) throw new IllegalArgumentException("the script language cannot be null");
    if (script == null) throw new IllegalArgumentException("the script content cannot be null");
    this.language = language;
    this.script = script;
    this.id = (id == null) ? JPPFUuid.normalUUID() : id;
    this.bindings = (bindings == null) ? new HashMap<>() : new HashMap<>(bindings);
  }

  /**
   * @return the script language.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @return the script content as text.
   */
  public String getScript() {
    return script;
  }

  /**
   * @return the identifier which allows reusing the compiled script.
   * @exclude
   */
  public String getId() {
    return id;
  }

  /**
   * Get the vairable bindings.
   * @return the optional variable bindings made available to the script at execution time.
   */
  public Map<String, Object> getBindings() {
    return new HashMap<>(bindings);
  }

  /**
   * Execute the script and return its execution result, using the initial variable bindings.
   * @return the script execution result, or {@code null} if there is no result.
   * @throws JPPFScriptingException if any error occurs while executing the script.
   */
  public Object evaluate() throws JPPFScriptingException {
    return evaluate(null);
  }

  /**
   * Execute the script and return its execution result, using additional bindings.
   * @param additionalBindings bindings to use in addition to the initial bindings specified in the constructor.
   * These optional bindings are only used for a single script execution and are not kept in this {@code ScriptDefintion}.
   * @return the script execution result, or {@code null} if there is no result.
   * @throws JPPFScriptingException if any error occurs while executing the script.
   */
  public Object evaluate(final Map<String, Object> additionalBindings) throws JPPFScriptingException {
    final Map<String, Object> bnd;
    if (additionalBindings == null) bnd = this.bindings;
    else {
      bnd = new HashMap<>(this.bindings);
      bnd.putAll(additionalBindings);
    }
    Object result = null;
    ScriptRunner runner = null;
    try {
      runner = ScriptRunnerFactory.getScriptRunner(language);
      if (runner == null) throw new JPPFScriptingException("Could not obtain a script runner for '" + language + "' language");
      result = runner.evaluate(id, script, bnd);
    } finally {
      if (runner != null) ScriptRunnerFactory.releaseScriptRunner(runner);
    }
    return result;
  }


  /**
   * Add the specified variable to the user-defined bindings.
   * @param name the name of the variable binding to add.
   * @param value the value of the variable.
   * @return this {@code ScriptDefintion}, for method call chaining.
   */
  public ScriptDefinition addBinding(final String name, final Object value) {
    bindings.put(name, value);
    return this;
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
