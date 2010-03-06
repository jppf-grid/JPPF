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

package org.jppf.server.mina.classloader;

import org.jppf.server.nio.NioObject;

/**
 * Message handled by the class server.
 * @author Laurent Cohen
 */
public class ClassServerMessage
{
	/**
	 * Contains the length of the actual message.
	 */
	public NioObject lengthObject = null;
	/**
	 * Contains the actual message.
	 */
	public NioObject message = null;
}
