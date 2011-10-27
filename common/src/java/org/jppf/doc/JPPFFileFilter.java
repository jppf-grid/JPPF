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
package org.jppf.doc;

import java.io.File;

import org.jppf.utils.FileUtils;

/**
 * Filter that only accepts files with specified extensions.
 */
public class JPPFFileFilter extends AbstractFileFilter
{
	/**
	 * Default excluded directory names.
	 */
	static final String[] DEFAULT_INCLUDES = { "html", "htm", "php" };

	/**
	 * Initialize a filter accepting all directory names except those excluded by default.
	 */
	public JPPFFileFilter()
	{
		includes = DEFAULT_INCLUDES;
		excludes = null;
	}

	/**
	 * Initialize a filter accepting the specified directory names and excluding those specified by {@link #DEFAULT_INCLUDES DEFAULT_EXCLUDES}.
	 * @param includes the included directory names; if null all are included. Null values are ignored.
	 */
	public JPPFFileFilter(final String[] includes)
	{
		this.includes = includes;
		this.excludes = null;
	}

	/**
	 * Initialize a filter accepting the specified directory names and excluding the specifiied ones.
	 * @param includes the included directory names; if null all are included. Null values are ignored.
	 * @param excludes the excluded directory names; if null none are excluded. Null values are ignored.
	 */
	public JPPFFileFilter(final String[] includes, final String[] excludes)
	{
		this.includes = includes;
		this.excludes = excludes;
	}

	/**
	 * Determine if a file is accepted.
	 * @param path the file path to check.
	 * @return true if the file is accepted, false otherwise.
	 * @see java.io.FileFilter#accept(java.io.File)
	 */
	@Override
	public boolean accept(final File path)
	{
		if (path.isDirectory()) return false;
		String ext = FileUtils.getFileExtension(path);
		return included(ext, true) && !excluded(ext, true);
	}
}
