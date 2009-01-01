/*
 * Java Parallel Processing Framework.
 *  Copyright (C) 2005-2009 JPPF Team. 
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.scripting;

import groovy.lang.*;

import java.util.Map;

/**
 * Script runner wrapper around a Groovy script engine.
 * @author Laurent Cohen
 */
public class GroovyScriptRunner implements ScriptRunner
{
	/**
	 * Evaluate the script specified as input and get the evaluation result.
	 * @param script a string containing the script to evaluate.
	 * @param variables a mapping of objects to add the scope of the script.
	 * @return the result of the evaluation as an object.
	 * @throws JPPFScriptingException if an error occurs while evaluating the script.
	 * @see org.jppf.scripting.ScriptRunner#evaluate(java.lang.String, java.util.Map)
	 */
	public Object evaluate(String script, Map<String, Object> variables) throws JPPFScriptingException
	{
		Binding binding = new Binding();
		for (Map.Entry<String, Object> entry: variables.entrySet()) binding.setVariable(entry.getKey(), entry.getValue());
		GroovyShell shell = new GroovyShell(binding);
		return shell.evaluate(script);
	}

	/**
	 * Initialize the execution environment.
	 * @see org.jppf.scripting.ScriptRunner#init()
	 */
	public void init()
	{
	}

	/**
	 * Perform cleanup after we're done using this script runner.
	 * @see org.jppf.scripting.ScriptRunner#cleanup()
	 */
	public void cleanup()
	{
	}
}
