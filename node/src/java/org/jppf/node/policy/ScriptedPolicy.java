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

package org.jppf.node.policy;

import java.io.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.node.protocol.*;
import org.jppf.scripting.*;
import org.jppf.utils.*;
import org.jppf.utils.stats.JPPFStatistics;
import org.slf4j.*;

/**
 * A policy which executes an aribtrary script in its {@code accepts()} method.
 * @author Laurent Cohen
 */
public class ScriptedPolicy extends ExecutionPolicy {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptedPolicy.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The job's server side SLA, set at runtime by the server.
   */
  protected transient JobSLA sla;
  /**
   * The job's client side SLA, set at runtime by the server.
   */
  protected transient JobClientSLA clientSla;
  /**
   * The job's metadata, set at runtime by the server.
   */
  protected transient JobMetadata metadata;
  /**
   * Number of nodes the job is already dispatched to.
   */
  protected transient int jobDispatches;
  /**
   * The server statistics.
   */
  protected transient JPPFStatistics stats;
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
  public boolean accepts(final PropertiesCollection info) {
    if ((script == null) || evaluationError) return false;
    Map<String, Object> variables = new HashMap<>();
    variables.put("jppfSystemInfo", info);
    variables.put("jppfSla", sla);
    variables.put("jppfClientSla", clientSla);
    variables.put("jppfMetadata", metadata);
    variables.put("jppfDispatches", jobDispatches);
    variables.put("jppfStats", stats);
    try {
      ScriptRunner runner = ScriptRunnerFactory.getScriptRunner(language);
      Object result = runner.evaluate(id, script, variables);
      if (!(result instanceof Boolean)) throw new JPPFException("result of scripted policy should be a boolean but instead is " + result);
      return (Boolean) result;
    } catch (Exception|NoClassDefFoundError e) {
      evaluationError = true;
      log.error("exception occurred evaluting scripted policy: {}\npolicy is\n{}", ExceptionUtils.getStackTrace(e), this);
    }
    return false;
  }

  /**
   * Print this object to a string.
   * @return an XML string representation of this object
   */
  @Override
  public String toString() {
    synchronized(ExecutionPolicy.class) {
      if (computedToString == null) {
        String ind = indent();
        StringBuilder sb = new StringBuilder().append(ind);
        sb.append("<Script language=\"").append(language).append("\"><![CDATA[\n");
        if (script != null) sb.append(script).append('\n');
        sb.append(ind).append("]]></Script>\n");
        computedToString = sb.toString();
      }
    }
    return computedToString;
  }

  /**
   * Set the parameters used as bound variables in the script.
   * @param sla the job server-side sla.
   * @param clientSla the job client-side sla.
   * @param metadata the job metadata.
   * @param jobDispatches the number of nodes the job is already dispatched to.
   * @param stats the server statistics.
   * @exclude
   */
  public void setVariables(final JobSLA sla, final JobClientSLA clientSla, final JobMetadata metadata, final int jobDispatches, final JPPFStatistics stats) {
    this.sla = sla;
    this.clientSla = clientSla;
    this.metadata = metadata;
    this.jobDispatches = jobDispatches;
    this.stats = stats;
  }
}
