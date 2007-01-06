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
package org.jppf.utils;

import java.io.ByteArrayOutputStream;

/**
 * Extension of {@link java.io.ByteArrayOutputStream ByteArrayOutputStream}, providing
 * a faster toByteArray() method that does not involve copying its internal buffer.
 * @author Laurent Cohen
 */
public class JPPFByteArrayOutputStream extends ByteArrayOutputStream
{
	/**
	 * Override of <code>toByteArray()</code> that returns a reference to the internal buffer
	 * instead of copy of it, significantly increasing the performance of this operation.
	 * @return the content of the stream as an array of bytes.
	 * @see java.io.ByteArrayOutputStream#toByteArray()
	 */
	public synchronized byte[] toByteArray()
	{
		return buf;
	}
}
