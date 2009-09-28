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

package org.jppf.utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.*;

/**
 * Formats log records in format [yyyy/MM/dd hh:mm:ss.SSS][LEVEL][package.ClassName.method()]: message.
 * @author Laurent Cohen
 */
public class JPPFLogFormatter extends Formatter
{
	/**
	 * Date format used in log entries.
	 */
	private SimpleDateFormat sdf = new SimpleDateFormat("[yyyy/MM/dd hh:mm:ss.SSS]");

	/**
	 * Format a log record.
	 * @param record the record to format.
	 * @return a string representation of the record according to this formatter.
	 * @see java.util.logging.Formatter#format(java.util.logging.LogRecord)
	 */
	public String format(LogRecord record)
	{
		StringBuilder sb = new StringBuilder();
		sb.append(sdf.format(new Date(record.getMillis())));
		sb.append("[").append(StringUtils.padRight(record.getLevel().getName(), ' ', 7)).append("]");
		sb.append("[");
		/*
		String s = record.getSourceClassName();
		if (s != null) sb.append(s);
		*/
		String s = record.getSourceClassName();
		String shortName = getShortName(s);
		StackTraceElement[] elts = new Throwable().getStackTrace();
		StackTraceElement elt = null;
		for (int i=0; i<elts.length; i++)
		{
			if (getShortName(elts[i].getClassName()).equals(shortName))
			{
				elt = elts[i];
				break;
			}
		}
		if (elt != null)
		{
			sb.append(elt.getClassName());
			if (elt.getMethodName() != null) sb.append(".").append(elt.getMethodName());
			sb.append("(");
			if (elt.getLineNumber() >= 0) sb.append(elt.getLineNumber());
			sb.append(")");
		}
		else
		{
			if (s != null) sb.append(s);
			s = record.getSourceMethodName();
			if (s != null) sb.append(".").append(s).append("()");
		}
		sb.append("]");
		sb.append(": ");
		s = record.getMessage();
		if (s != null) sb.append(s);
		Object[] params = record.getParameters();
		if (params != null) for (Object o: params) sb.append("|").append(o);
		sb.append("\n");
		return sb.toString();
	}

	/**
	 * Get the short name of a class, without the package name.
	 * @param fqn - the fully qualified name of the class. 
	 * @return a string representing the short name of a class.
	 */
	private String getShortName(String fqn)
	{
		int idx = fqn.lastIndexOf('.');
		return idx >= 0 ? fqn.substring(idx+1) : fqn;
	}
}
