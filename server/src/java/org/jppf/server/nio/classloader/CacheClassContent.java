/*
 * JPPF.
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
package org.jppf.server.nio.classloader;

/**
 * This class encapsulates the content oif a class cache entry.
 * @author Domingos Creado
 */
public class CacheClassContent
{
	/**
	 * The actual content of this element.
	 */
	private byte[] content;

	/**
	 * Initialize this content with the specified data.
	 * @param content the data as an array of bytes.
	 */
	public CacheClassContent(byte[] content)
	{
		super();
		this.content = content;
	}

	/**
	 * Get the actual content of this element.
	 * @return the data as an array of bytes.
	 */
	public byte[] getContent()
	{
		return content;
	}
}
