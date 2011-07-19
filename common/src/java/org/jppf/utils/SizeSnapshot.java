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

package org.jppf.utils;

/**
 * Utility class for collecting size statistics.
 * @author Laurent Cohen
 */
public class SizeSnapshot
{
	/**
	 * The title given to this snapshot.
	 */
	private String title = "";
	/**
	 * The minimum recorded size.
	 */
	private int min = Integer.MAX_VALUE;
	/**
	 * The minimum recorded size.
	 */
	private int max = 0;
	/**
	 * The minimum recorded size.
	 */
	private int total = 0;
	/**
	 * The minimum recorded size.
	 */
	private int current = 0;
	/**
	 * The minimum recorded size.
	 */
	private int average = 0;

	/**
	 * Default constructor.
	 */
	public SizeSnapshot()
	{
	}

	/**
	 * Default constructor.
	 * @param title the title given to this snapshot.
	 */
	public SizeSnapshot(String title)
	{
		this.title = title;
	}
}
