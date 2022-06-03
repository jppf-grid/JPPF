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
   * The script specification.
   */
  protected final ScriptDefinition spec;
  /**
   * Flag set if an error is raised while executing the script,
   * to avoid evaluating the script again.
   */
  protected boolean evaluationError = false;

  /**
   * Initialize this policy with a script language and a script.
   * @param language the script language to use.
   * @param script the script to execute.
   */
  public ScriptedPolicy(final String language, final String script) {
    this.spec = new ScriptDefinition(language, script);
  }

  /**
   * Initialize this policy with a script language and a script read from a reader.
   * @param language the script language to use.
   * @param scriptReader a reader from which to read the script to execute.
   * @throws IOException if any error occurs while reading the script from the reader.
   */
  public ScriptedPolicy(final String language, final Reader scriptReader) throws IOException {
    this.spec = new ScriptDefinition(language, scriptReader);
  }

  /**
   * Initialize this policy with a script language and a script read from a file.
   * @param language the script language to use.
   * @param scriptFile the file containing the script to execute.
   * @throws IOException if any error occurs while reading the script from the reader.
   */
  public ScriptedPolicy(final String language, final File scriptFile) throws IOException {
    this.spec = new ScriptDefinition(language, scriptFile);
  }

  @Override
  public boolean accepts(final PropertiesCollection<String> info) {
    if (evaluationError) return false;
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
    try {
      final Object result = spec.evaluate(variables);
      if (!(result instanceof Boolean)) throw new JPPFException("result of scripted policy should be a boolean but instead is " + result);
      return (Boolean) result;
    } catch (final Exception|NoClassDefFoundError e) {
      evaluationError = true;
      log.error("exception occurred evaluating scripted policy: {}\npolicy is\n{}", ExceptionUtils.getStackTrace(e), this);
    }
    return false;
  }

  @Override
  public String toString(final int n) {
    final StringBuilder sb = new StringBuilder(indent(n)).append("<Script language=\"").append(spec.getLanguage()).append("\"><![CDATA[\n");
    sb.append(spec.getScript()).append('\n');
    return sb.append(indent(n)).append("]]></Script>\n").toString();
  }
}
