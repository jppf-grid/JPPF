/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

import java.util.Map;


/**
 * Common interface for all script runners, allowing access to a scripting engine.
 * @author Laurent Cohen
 */
public interface ScriptRunner
{
	/**
	 * Evaluate the script specified as input and get the evaluation result.
	 * @param script - a string containing the script to evaluate.
	 * @param variables - a mapping of objects to variable names, added within the scope of the script.
	 * @return the result of the evaluation as an object. The actual type of the result
	 * depends on the scripting engine that is used.
	 * @throws JPPFScriptingException if an error occurs while evaluating the script.
	 */
	Object evaluate(String script, Map<String, Object> variables) throws JPPFScriptingException;
	/**
	 * Evaluate the script specified as input and get the evaluation result.
	 * @param scriptId - a unique identifer for the script, to be used if the engine generates compiled code
	 * which can be later retrieved through this id.
	 * @param script - a string containing the script to evaluate.
	 * @param variables - a mapping of objects to variable names, added within the scope of the script.
	 * @return the result of the evaluation as an object. The actual type of the result
	 * depends on the scripting engine that is used.
	 * @throws JPPFScriptingException if an error occurs while evaluating the script.
	 */
	Object evaluate(String scriptId, String script, Map<String, Object> variables) throws JPPFScriptingException;
	/**
	 * Initialize the execution environment.
	 */
	void init();
	/**
	 * Perform cleanup after we're done using this script runner.
	 */
	void cleanup();
}
