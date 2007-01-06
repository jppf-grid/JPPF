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

/**
 * buffer for the data read from or written to a socket connection.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFBuffer
{
	/**
	 * The actual buffer, intended to contain a serialize object graph.
	 */
	private byte[] buffer = new byte[0];
	/**
	 * The length of the buffer.
	 */
	private int length = 0;

	/**
	 * Initialize this buffer.
	 */
	public JPPFBuffer()
	{
	}

	/**
	 * Initialize this buffer with the following String.
	 * @param str the string whose contents will be put into this buffer. 
	 */
	public JPPFBuffer(String str)
	{
		this.buffer = str.getBytes();
		this.length = buffer.length;
	}

	
	/**
	 * Initialize this buffer with a specified buffer and buffer length.
	 * @param buffer the buffer to use.
	 * @param length the number of bytes to use in the buffer.
	 */
	public JPPFBuffer(byte[] buffer, int length)
	{
		this.buffer = buffer;
		this.length = length;
	}

	/**
	 * Set the buffered data.
	 * @param buffer an array of bytes containing the data.
	 */
	public void setBuffer(byte[] buffer)
	{
		this.buffer = buffer;
	}

	/**
	 * Get the buffered data.
	 * @return an array of bytes containing the data.
	 */
	public byte[] getBuffer()
	{
		return buffer;
	}
	
	/**
	 * Set the length of the buffered data.
	 * @param length the length as an int.
	 */
	public void setLength(int length)
	{
		this.length = length;
	}
	
	/**
	 * Get the length of the buffered data.
	 * @return the length as an int.
	 */
	public int getLength()
	{
		return length;
	}
}