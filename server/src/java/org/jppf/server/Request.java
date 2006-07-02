/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.server;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Represent a request to be or been received.
 * It follow the same strategy of JPPFBuffer, but this is designed to run 
 * with nonblocking io.
 * @author Domingos Creado
 */
public class Request {
	
	/**
	 * Request creation timestamp.
	 */
	private long start = System.currentTimeMillis();
	/**
	 * Size of the request data in bytes.
	 */
	private int size;
	/**
	 * Buffer used to transfer the request data to and from streams.
	 */
	private ByteBuffer buffer;
	/**
	 * Stream where the result of the request is stored.
	 */
	private ByteArrayOutputStream output = new ByteArrayOutputStream();

	/**
	 * Initialize an instance of this class.
	 */
	public Request()
	{
	}

	/**
	 * Get the request creation timestamp.
	 * @return the timestamp in milliseconds as a long value.
	 */
	public long getStart(){
		return start;
	}
	
	/**
	 * Get the stream where the result of the request is stored.
	 * @return a <code>ByteArrayOutputStream</code> instance.
	 */
	public ByteArrayOutputStream getOutput() {
		return output;
	}

	/**
	 * Get the buffer used to transfer the request data to and from streams.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public ByteBuffer getBuffer() {
		return buffer;
	}

	/**
	 * Set the buffer used to transfer the request data to and from streams.
	 * @param buffer a <code>ByteBuffer</code> instance.
	 */
	public void setBuffer(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	/**
	 * Get the size of the request data in bytes.
	 * @return the size as an int value.
	 */
	public int getSize() {
		return size;
	}

	/**
	 * Set the size of the request data in bytes.
	 * @param size the size as an int value.
	 */
	public void setSize(int size) {
		this.size = size;
	}
}