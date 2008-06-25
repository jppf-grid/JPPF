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

package org.jppf.io;

import java.nio.ByteBuffer;

/**
 * This interface represents an abstraction of any source of incoming data.
 * @author Laurent Cohen
 */
public interface InputSource extends IO
{
	/**
	 * Read data from this input source into an array of bytes.
	 * @param data the buffer into which to write.
	 * @param offset the position in the buffer where to start storing the data.
	 * @param len the size in bytes of the data to read.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 */
	int read(byte[] data, int offset, int len) throws Exception;

	/**
	 * Read data from this input source into a byte buffer.
	 * @param data the buffer into which to write.
	 * @return the number of bytes actually read, or -1 if end of stream was reached.
	 * @throws Exception if an IO error occurs.
	 */
	int read(ByteBuffer data) throws Exception;

	/**
	 * Read an int value from this input source.
	 * @return the value read, or -1 if an end of file condition was reached. 
	 * @throws Exception if an IO error occurs.
	 */
	int readInt() throws Exception;
	/**
	 * Skip <cpde>n</copde> bytes of data form this input source.
	 * @param n the number of bytes to skip.
	 * @return the number of bytes actually skipped.
	 * @throws Exception if an IO error occurs.
	 */
	int skip(int n) throws Exception;
}
