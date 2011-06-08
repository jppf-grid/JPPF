/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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

import java.util.Random;

/**
 * Common abstract superclass for objects that establish a connection with a remote socket.
 * @author Laurent Cohen
 */
public abstract class AbstractSocketInitializer implements SocketInitializer
{
	/**
	 * Determines whether any connection attempt succeeded.
	 */
	protected boolean successfull = false;
	/**
	 * Current number of connection attempts.
	 */
	protected int attemptCount = 0;
	/**
	 * The socket wrapper to initialize.
	 */
	protected SocketWrapper socketWrapper = null;
	/**
	 * Used to compute a random start delay for this node.
	 */
	protected Random rand = new Random(System.currentTimeMillis());
	/**
	 * Determine whether this socket initializer has been intentionally closed. 
	 */
	protected boolean closed = false;
	/**
	 * Name given to this initializer.
	 */
	protected String name = "";

	/**
	 * Determine whether this socket initializer has been intentionally closed. 
	 * @return true if this socket initializer has been intentionally closed, false otherwise.
	 * @see org.jppf.comm.socket.SocketInitializer#isClosed()
	 */
	public boolean isClosed()
	{
		return closed;
	}

	/**
	 * Determine whether any connection attempt succeeded.
	 * @return true if any attempt was successfull, false otherwise.
	 * @see org.jppf.comm.socket.SocketInitializer#isSuccessfull()
	 */
	public boolean isSuccessfull()
	{
		return successfull;
	}

	/**
	 * Get the name given to this initializer.
	 * @return the name as a string.
	 * @see org.jppf.comm.socket.SocketInitializer#getName()
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Set the name given to this initializer.
	 * @param name the name as a string.
	 * @see org.jppf.comm.socket.SocketInitializer#setName(java.lang.String)
	 */
	public void setName(String name)
	{
		this.name = name;
	}
}
