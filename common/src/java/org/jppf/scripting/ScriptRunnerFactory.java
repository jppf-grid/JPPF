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

/**
 * Factory used to instantiate script runners.
 * @author Laurent Cohen
 */
public final class ScriptRunnerFactory
{
	/**
	 * Instantiation of this class is not allowed.
	 */
	private ScriptRunnerFactory()
	{
	}

	/**
	 * Instantiate a script runner based on the specified script language.
	 * @param language the name of the script language to use.
	 * @return A <code>ScriptRunner</code> instance, or null if no known sciprt runner
	 * exists for the specified language.
	 */
	public static ScriptRunner makeScriptRunner(String language)
	{
		if ("javascript".equalsIgnoreCase(language)) return new RhinoScriptRunner();
		else if ("groovy".equalsIgnoreCase(language)) return new GroovyScriptRunner();
		return null;
	}
}
