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
package org.jppf.server;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.jppf.utils.JPPFByteArrayOutputStream;

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
	private ByteArrayOutputStream output = new JPPFByteArrayOutputStream();

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
