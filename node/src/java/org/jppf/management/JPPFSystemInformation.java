/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

package org.jppf.management;

import java.io.Serializable;

import org.jppf.utils.*;

/**
 * This class encapsulates the system information for a node.<br>
 * It includes:
 * <ul>
 * <li>System properties, including -X flags</li>
 * <li>Runtime information such as available processors and memory usage</li>
 * <li>Environment variables</li>
 * </ul>
 * @author Laurent Cohen
 */
public class JPPFSystemInformation implements Serializable
{
	/**
	 * Map holding the system properties.
	 */
	private TypedProperties system = null;
	/**
	 * Map holding the runtime information
	 */
	private TypedProperties runtime = null;
	/**
	 * Map holding the environment variables.
	 */
	private TypedProperties env = null;
	/**
	 * Map holding the JPPF configuration properties.
	 */
	private TypedProperties jppf = null;

	/**
	 * Get the map holding the system properties.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getSystem()
	{
		return system;
	}

	/**
	 * Get the map holding the runtime information
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getRuntime()
	{
		return runtime;
	}

	/**
	 * Get the map holding the environment variables.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getEnv()
	{
		return env;
	}

	/**
	 * Get the nap holding the JPPF configuration properties.
	 * @return a <code>TypedProperties</code> instance.
	 */
	public TypedProperties getJppf()
	{
		return jppf;
	}

	/**
	 * Populate this node information object.
	 */
	public void populate()
	{
		system = SystemUtils.getSystemProperties();
		runtime = SystemUtils.getRuntimeInformation();
		env = SystemUtils.getEnvironment();
		jppf = JPPFConfiguration.getProperties();
	}
}
