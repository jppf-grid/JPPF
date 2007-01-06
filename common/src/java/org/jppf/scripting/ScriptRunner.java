/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
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
	 * @param script a string containing the script to evaluate.
	 * @param variables a mapping of objects to add the scope of the script.
	 * @return the result of the evaluation as an object. The actual type of the result
	 * depends on the scripting engine that is used.
	 * @throws JPPFScriptingException if an error occurs while evaluating the script.
	 */
	Object evaluate(String script, Map<String, Object> variables) throws JPPFScriptingException;
	/**
	 * Initialize the execution environment.
	 */
	void init();
	/**
	 * Perform cleanup after we're done using this script runner.
	 */
	void cleanup();
}
