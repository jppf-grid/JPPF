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

package org.jppf.scripting;

import groovy.lang.*;

import java.util.*;

/**
 * Script runner wrapper around a Groovy script engine.
 * @author Laurent Cohen
 */
public class GroovyScriptRunner implements ScriptRunner
{
  /**
   * Mapping of Groovy scripts to their uuid.
   */
  private static Map<String, Script> scriptMap = new HashMap<>();

  /**
   * Evaluate the script specified as input and get the evaluation result.
   * @param script a string containing the script to evaluate.
   * @param variables a mapping of objects to add the scope of the script.
   * @return the result of the evaluation as an object.
   * @throws JPPFScriptingException if an error occurs while evaluating the script.
   * @see org.jppf.scripting.ScriptRunner#evaluate(java.lang.String, java.util.Map)
   */
  @Override
  public Object evaluate(final String script, final Map<String, Object> variables) throws JPPFScriptingException
  {
    return evaluate(null, script, variables);
  }

  /**
   * Evaluate the script specified as input and get the evaluation result.
   * @param scriptId a unique identifier for the script, to be used if the engine generates compiled code
   * which can be later retrieved through this id.
   * @param script a string containing the script to evaluate.
   * @param variables a mapping of objects to variable names, added within the scope of the script.
   * @return the result of the evaluation as an object. The actual type of the result
   * depends on the scripting engine that is used.
   * @throws JPPFScriptingException if an error occurs while evaluating the script.
   * @see org.jppf.scripting.ScriptRunner#evaluate(java.lang.String, java.lang.String, java.util.Map)
   */
  @Override
  public Object evaluate(final String scriptId, final String script, final Map<String, Object> variables) throws JPPFScriptingException
  {
    try
    {
      //GroovyShell shell = new GroovyShell(binding);
      GroovyShell shell = new GroovyShell();
      Script groovyScript = scriptId == null ? null : scriptMap.get(scriptId);
      if (groovyScript == null)
      {
        groovyScript = shell.parse(script);
        if (scriptId != null) scriptMap.put(scriptId, groovyScript);
      }
      Binding binding = new Binding();
      for (Map.Entry<String, Object> entry: variables.entrySet()) binding.setVariable(entry.getKey(), entry.getValue());
      groovyScript.setBinding(binding);
      return groovyScript.run();
    }
    catch(Exception e)
    {
      throw new JPPFScriptingException(e);
    }
  }

  /**
   * Initialize the execution environment.
   * @see org.jppf.scripting.ScriptRunner#init()
   */
  @Override
  public void init()
  {
  }

  /**
   * Perform cleanup after we're done using this script runner.
   * @see org.jppf.scripting.ScriptRunner#cleanup()
   */
  @Override
  public void cleanup()
  {
  }
}
