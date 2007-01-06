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
package org.jppf.ui.options.xml;

import java.util.*;

/**
 * Utility class used to collect error and warning messages for various operations,
 * including XML parsing and validation.
 * @author Laurent Cohen
 */
public class JPPFErrorReporter
{
	/**
	 * A list of collected warnings.
	 */
	public List<String> warnings = new ArrayList<String>();
	/**
	 * A list of collected errors.
	 */
	public List<String> errors = new ArrayList<String>();
	/**
	 * A list of fatal (non recoverable) errors.
	 */
	public List<String> fatalErrors = new ArrayList<String>();
	/**
	 * A name used to identify this error reporter.
	 */
	public String name = null;

	/**
	 * Initialize this error reporter with the specified name.
	 * @param name a name used to identify this error reporter.
	 */
	public JPPFErrorReporter(String name)
	{
		this.name = name;
	}

	/**
	 * Get a string concatenating all the error messages.
	 * @return a string.
	 */
	public String allErrorsAsStrings()
	{
		return concatenateMessages(errors);
	}

	/**
	 * Get a string concatenating all the fatal error messages.
	 * @return a string.
	 */
	public String allFatalErrorsAsStrings()
	{
		return concatenateMessages(fatalErrors);
	}

	/**
	 * Get a string concatenating all the warning messages.
	 * @return a string.
	 */
	public String allWarningsAsStrings()
	{
		return concatenateMessages(warnings);
	}

	/**
	 * Concatenate all string messages in a list into a single string.
	 * @param list the list to get the messages to concatenate from.
	 * @return a concatenation of all messages in the list, separated by new lines.
	 */
	protected String concatenateMessages(List<String> list)
	{
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<list.size(); i++)
		{
			if (i > 0) sb.append("\n");
			sb.append(list.get(i));
		}
		return sb.toString();
	}

	//public class 
}
