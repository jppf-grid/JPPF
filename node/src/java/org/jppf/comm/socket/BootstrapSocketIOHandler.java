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
 * An {@link IOHandler} implementation that delegates I/O to a {@link SocketWrapper}.
 * @author Laurent Cohen
 */
public class BootstrapSocketIOHandler implements IOHandler
{
	/**
	 * The socket wrapper that handles the I/O.
	 */
	protected SocketWrapper socketWrapper = null;

	/**
	 * Initialize this handler with the specified socket wrapper.
	 * @param socketWrapper the socket wrapper that handles the I/O.
	 */
	public BootstrapSocketIOHandler(SocketWrapper socketWrapper)
	{
		this.socketWrapper = socketWrapper;
	}

	/**
	 * Read the next block of data from the channel.
	 * @return the data read and its length as a {@link JPPFBuffer} instance.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.node.IOHandler#read()
	 */
	public JPPFBuffer read() throws Exception
	{
		return socketWrapper.receiveBytes(0);
	}

	/**
	 * {@inheritDoc}
	 */
	public void writeInt(int value) throws Exception
	{
		socketWrapper.writeInt(value);
	}

	/**
	 * {@inheritDoc}
	 */
	public void write(byte[] data, int offset, int len) throws Exception
	{
		socketWrapper.write(data, offset, len);
	}

	/**
	 * Flush this handler.
	 * @throws Exception if any error occurs.
	 * @see org.jppf.server.node.IOHandler#flush()
	 */
	public void flush() throws Exception
	{
		socketWrapper.flush();
	}
}
