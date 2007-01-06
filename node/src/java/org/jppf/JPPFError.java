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
package org.jppf;

/**
 * Class of JPPF-specific error that may be caught in special cases.
 * The goal of this class is to provide an unchecked exception, allowing a quick
 * propagation up the call stack, while still allowing it to be caught specifically, in case the
 * application chooses not to exit, in response to the problem.
 * @author Laurent Cohen
 */
public class JPPFError extends Error
{
	/**
	 * Initialize this error with a specified message and cause exception.
	 * @param message the message for this error.
	 * @param cause the cause exception.
	 */
	public JPPFError(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Initialize this error with a specified message.
	 * @param message the message for this error.
	 */
	public JPPFError(String message)
	{
		super(message);
	}

	/**
	 * Initialize this error with a specified cause exception.
	 * @param cause the cause exception.
	 */
	public JPPFError(Throwable cause)
	{
		super(cause);
	}
}
