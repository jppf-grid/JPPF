/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.plugin;

/**
 * Interface for defining a plugin and managing its lifecycle and dependencies.
 * @author Laurent Cohen
 */
public interface JPPFPlugin
{
	/**
	 * Get the id of this plugin. The id should be unique within a single JVM.
	 * @return the id as a string.
	 */
	String getPluginId();
	/**
	 * Start this plugin and eventually the plugins it depends on.
	 */
	void startPlugin();
	/**
	 * Terminate this plugin and free the resources it uses.
	 */
	void endPlugin();
	/**
	 * Get the ids of the plugins this plugin depends on.
	 * @return an array of string ids.
	 */
	String[] getPluginDependencies();
}
