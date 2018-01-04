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

package org.jppf.job;

import java.io.*;
import java.util.*;

import org.jppf.node.protocol.JPPFDistributedJob;
import org.jppf.scripting.BaseScriptEvaluator;
import org.slf4j.*;

/**
 * A job selector that uses a script to accept jobs. The script is evaluated using the {@code javax.script.*} API (JSR-223).
 * <p>When the script is executed, it provides access to a variable named "jppfJob" which is an instance of {@link JPPFDistributedJob}.
 * @author Laurent Cohen
 * @since 5.1
 */
public class ScriptedJobSelector extends BaseScriptEvaluator implements JobSelector {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptedJobSelector.class);

  /**
   * Initialize this selector with the specfied language and script.
   * @param language the language of the script to evaluate.
   * @param script the script to evaluate.
   */
  public ScriptedJobSelector(final String language, final String script) {
    super(language, script);
  }

  /**
   * Initialize this selector with the specfied language and script reader.
   * @param language the language of the script to evaluate.
   * @param scriptReader a reader from which to read the script to evaluate.
   * @throws IOException if the script could not be read.
   */
  public ScriptedJobSelector(final String language, final Reader scriptReader) throws IOException {
    super(language, scriptReader);
  }

  /**
   * Initialize this selector with the specfied language and script file.
   * @param language the language of the script to evaluate.
   * @param scriptFile a file from which to read the script to evaluate.
   * @throws IOException if the script could not be read.
   */
  public ScriptedJobSelector(final String language, final File scriptFile) throws IOException {
    super(language, scriptFile);
  }

  @Override
  public boolean accepts(final JPPFDistributedJob job) {
    final Map<String, Object> variables = new HashMap<>();
    variables.put("jppfJob", job);
    final Object result = evaluate(variables);
    if (result instanceof Boolean) return (Boolean) result;
    log.error("result of scripted job selector should be a boolean but instead is " + result);
    return false;
  }
}
