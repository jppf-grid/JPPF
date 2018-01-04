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

package org.jppf.node.policy;

import java.io.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.scripting.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A policy which executes an aribtrary script in its {@code accepts()} method.
 * @author Laurent Cohen
 */
public class ScriptedPolicy extends ExecutionPolicy {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptedPolicy.class);
  /**
   * The script language to use.
   */
  protected final String language;
  /**
   * A unique id given to this script, to allow caching its compiled version.
   */
  protected final String id;
  /**
   * The script to execute.
   */
  protected String script;
  /**
   * Flag set if an error is raised while executing the script,
   * to avoid evaluating the script again.
   */
  protected boolean evaluationError = false;

  /**
   * Initialize this policy with a script language.
   * @param language the script language to use.
   */
  private ScriptedPolicy(final String language) {
    if (language == null) throw new IllegalArgumentException("the script language cannot be null");
    this.id = JPPFUuid.normalUUID();
    this.language = language;
  }

  /**
   * Initialize this policy with a script language and a script.
   * @param language the script language to use.
   * @param script the script to execute.
   */
  public ScriptedPolicy(final String language, final String script) {
    this(language);
    if (script == null) throw new IllegalArgumentException("the script cannot be null");
    this.script = script;
  }

  /**
   * Initialize this policy with a script language and a script read from a reader.
   * @param language the script language to use.
   * @param scriptReader a reader from which to read the script to execute.
   * @throws IOException if any error occurs while reading the script from the reader.
   */
  public ScriptedPolicy(final String language, final Reader scriptReader) throws IOException {
    this(language);
    if (script == null) throw new IllegalArgumentException("the script cannot be null");
    this.script = FileUtils.readTextFile(scriptReader);
  }

  /**
   * Initialize this policy with a script language and a script read from a file.
   * @param language the script language to use.
   * @param scriptFile the file containing the script to execute.
   * @throws IOException if any error occurs while reading the script from the reader.
   */
  public ScriptedPolicy(final String language, final File scriptFile) throws IOException {
    if (language == null) throw new IllegalArgumentException("script language cannot be null");
    this.language = language;
    this.script = FileUtils.readTextFile(scriptFile);
    this.id = JPPFUuid.normalUUID();
  }

  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    if ((script == null) || evaluationError) return false;
    final Map<String, Object> variables = new HashMap<>();
    variables.put("jppfSystemInfo", info);
    final PolicyContext ctx = getContext();
    if (ctx != null) {
      variables.put("jppfSla", ctx.getSLA());
      variables.put("jppfClientSla", ctx.getClientSLA());
      variables.put("jppfMetadata", ctx.getMetadata());
      variables.put("jppfDispatches", ctx.getJobDispatches());
      variables.put("jppfStats", ctx.getStats());
    }
    ScriptRunner runner = null;
    try {
      runner = ScriptRunnerFactory.getScriptRunner(language);
      if (runner != null) {
        final Object result = runner.evaluate(id, script, variables);
        if (!(result instanceof Boolean)) throw new JPPFException("result of scripted policy should be a boolean but instead is " + result);
        return (Boolean) result;
      }
    } catch (Exception|NoClassDefFoundError e) {
      evaluationError = true;
      log.error("exception occurred evaluating scripted policy: {}\npolicy is\n{}", ExceptionUtils.getStackTrace(e), this);
    } finally {
      if (runner != null) ScriptRunnerFactory.releaseScriptRunner(runner);
    }
    return false;
  }

  @Override
  public String toString(final int n) {
    final StringBuilder sb = new StringBuilder(indent(n)).append("<Script language=\"").append(language).append("\"><![CDATA[\n");
    if (script != null) sb.append(script).append('\n');
    return sb.append(indent(n)).append("]]></Script>\n").toString();
  }
}
