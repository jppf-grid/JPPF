/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

package org.jppf.classloader;

/**
 * Enumeration of available delegation models for the JPPF class loaders.
 * @author Laurent Cohen
 */
public enum DelegationModel
{
	/**
	 * Standard delegation model to parent first.
	 */
	PARENT_FIRST,
	/**
	 * Delegation model to local URL classpath first.
	 */
	LOCAL_FIRST
}
