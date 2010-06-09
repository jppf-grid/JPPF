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

package org.jppf.server.nio.classloader;

import org.apache.commons.logging.*;
import org.jppf.classloader.LocalClassLoaderWrapperHandler;
import org.jppf.server.nio.ChannelWrapper;

/**
 * Context object associated with a socket channel used by the class server of the JPPF driver. 
 * @author Laurent Cohen
 */
public class LocalClassContext extends ClassContext
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(LocalClassContext.class);
	/**
	 * Determines whther DEBUG logging level is enabled.
	 */
	protected static boolean traceEnabled = log.isTraceEnabled();

	/**
	 * {@inheritDoc}
	 */
	public void serializeResource(ChannelWrapper<?> wrapper) throws Exception
	{
		super.serializeResource(wrapper);
		((LocalClassLoaderWrapperHandler) wrapper).setMessage(message);
	}

	/**
	 * Read data from a channel.
	 * @param wrapper the channel to read the data from.
	 * @return true if all the data has been read, false otherwise.
	 * @throws Exception if an error occurs while reading the data.
	 */
	public boolean readMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		if (traceEnabled) log.trace("reading message for " + wrapper + ", message = " + message);
		LocalClassLoaderWrapperHandler channel = (LocalClassLoaderWrapperHandler) wrapper;
		while ((message == null) || (message.buffer == null) || (message.length <= 0) || (readByteCount < message.length)) channel.goToSleep();
		if (traceEnabled) log.trace("message read for " + wrapper + ", message = " + message);
		channel.wakeUp();
		return true;
	}

	/**
	 * Write data to a channel.
	 * @param wrapper the channel to write the data to.
	 * @return true if all the data has been written, false otherwise.
	 * @throws Exception if an error occurs while writing the data.
	 */
	public boolean writeMessage(ChannelWrapper<?> wrapper) throws Exception
	{
		if (traceEnabled) log.trace("writing message for " + wrapper);
		message.lengthWritten = true;
		writeByteCount = message.length;
		((LocalClassLoaderWrapperHandler) wrapper).wakeUp();
		return true;
	}
}
