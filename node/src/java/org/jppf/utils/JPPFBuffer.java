/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
