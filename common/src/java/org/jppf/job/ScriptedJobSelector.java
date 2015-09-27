/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

package org.jppf.job;

import java.io.*;
import java.util.*;

import org.jppf.JPPFException;
import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.scripting.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A job selector that uses a script to accept jobs. The script is evaluated using the {@code javax.script.*} API (JSR-223).
 * @author Laurent Cohen
 * @since 5.1
 */
public class ScriptedJobSelector implements JobSelector {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptedJobSelector.class);
  /**
   * The language of the script to evaluate.
   */
  private final String language;
  /**
   * The script to evaluate.
   */
  private final String script;
  /**
   * Auto-generated uuid assigned to the script so its compiled version can be reused repeatedly.
   */
  private final String scriptId = new JPPFUuid().toString();
  /**
   * Flag set if an error is raised while executing the script, to avoid evaluating the script again.
   */
  private transient boolean evaluationError = false;

  /**
   * Initialize this selector with the specfied language and script.
   * @param language the language of the script to evaluate.
   * @param script the script to evaluate.
   */
  public ScriptedJobSelector(final String language, final String script) {
    this.language = language;
    this.script = script;
  }

  /**
   * Initialize this selector with the specfied language and script reader.
   * @param language the language of the script to evaluate.
   * @param scriptReader a reader from which to read the script to evaluate.
   * @throws IOException if the script could not be read.
   */
  public ScriptedJobSelector(final String language, final Reader scriptReader) throws IOException {
    this.language = language;
    this.script = FileUtils.readTextFile(scriptReader);
  }

  /**
   * Initialize this selector with the specfied language and script file.
   * @param language the language of the script to evaluate.
   * @param scriptFile a file from which to read the script to evaluate.
   * @throws IOException if the script could not be read.
   */
  public ScriptedJobSelector(final String language, final File scriptFile) throws IOException {
    this.language = language;
    this.script = FileUtils.readTextFile(scriptFile);
  }

  /**
   * Get the language of the script to evaluate.
   * @return the language as a string.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Get the script to evaluate.
   * @return th script as a string.
   */
  public String getScript() {
    return script;
  }

  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    if ((script == null) || evaluationError) return false;
    Map<String, Object> variables = new HashMap<>();
    variables.put("jppfJob", job);
    ScriptRunner runner = null;
    try {
      runner = ScriptRunnerFactory.getScriptRunner(language);
      if (runner != null) {
        Object result = runner.evaluate(scriptId, script, variables);
        if (!(result instanceof Boolean)) throw new JPPFException("result of scripted job selector should be a boolean but instead is " + result);
        return (Boolean) result;
      }
    } catch (Exception|NoClassDefFoundError e) {
      if (!evaluationError) {
        evaluationError = true;
        log.error(String.format("exception occurred evaluting scripted job selector: %s%nscript language: %s, script content:%n%s",
          ExceptionUtils.getStackTrace(e), language, script));
      }
    } finally {
      if (runner != null) ScriptRunnerFactory.releaseScriptRunner(runner);
    }
    return false;
  }
}
