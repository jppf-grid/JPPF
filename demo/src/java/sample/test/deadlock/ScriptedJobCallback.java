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

package sample.test.deadlock;

import java.util.*;

import org.jppf.client.JPPFJob;
import org.jppf.scripting.*;
import org.jppf.utils.*;

/** */
public class ScriptedJobCallback extends JobStreamingCallback.Adapter {
  /** */
  final ScriptRunner runner;
  /** */
  final String jobCreatedScript, jobCompletedScript;

  /** */
  public ScriptedJobCallback() {
    runner = ScriptRunnerFactory.getScriptRunner("javascript");
    TypedProperties config = JPPFConfiguration.getProperties();
    jobCreatedScript = config.getString("deadlock.script.created");
    jobCompletedScript = config.getString("deadlock.script.completed");
  }

  @Override
  public void jobCreated(final JPPFJob job) {
    if (jobCreatedScript != null) runScript(job, "deadlock.script.created", jobCreatedScript);
  }

  @Override
  public void jobCompleted(final JPPFJob job, final JobStreamImpl jobStream) {
    if (jobCompletedScript != null) runScript(job, "deadlock.script.completed", jobCompletedScript);
  }

  /**
   * 
   * @param job .
   * @param scriptID .
   * @param script .
   */
  private void runScript(final JPPFJob job, final String scriptID, final String script) {
    Map<String, Object> variables = new HashMap<>();
    variables.put("job", job);
    try {
      runner.evaluate(scriptID, script, variables);
    } catch (JPPFScriptingException e) {
      e.printStackTrace();
    }
  }
}
