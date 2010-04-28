/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.comm.socket;

import org.jppf.utils.JPPFBuffer;

/**
 * Generic wrapper around a data channel (socket or in-memory). 
 * @author Laurent Cohen
 */
public interface IOHandler
{
	/**
	 * Read the next block of data from the channel.
	 * @return the data read and its length as a {@link JPPFBuffer} instance.
	 * @throws Exception if any error occurs.
	 */
	JPPFBuffer read() throws Exception;
	/**
	 * Write multiple blocks of data to the channel.
	 * @param len the total length of the data to write.
	 * @param data the data to write as an array of byte arrays.
	 * @throws Exception if any error occurs.
	 */
	void write(int len, byte[]...data) throws Exception;
	/**
	 * Flush this handler.
	 * @throws Exception if any error occurs.
	 */
	void flush() throws Exception;
	/**
	 * Write an int value to the channel.
	 * @param value the value to write.
	 * @throws Exception if any error occurs.
	 */
	void writeInt(int value) throws Exception;
	/**
	 * Write a block of data to the channel.
	 * @param data the data to write as an array of bytes.
	 * @param offset the poistion a which to start in the data.
	 * @param len the number of bytes to write.
	 * @throws Exception if any error occurs.
	 */
	void write(byte[] data, int offset, int len) throws Exception;
}
