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

package org.jppf.server.nio.multiplexer;

import java.nio.channels.SelectionKey;

import org.apache.commons.logging.*;
import org.jppf.server.nio.AbstractSocketChannelHandler;

/**
 * 
 * @author Laurent Cohen
 */
public class MultiplexerChannelInitializer implements Runnable 
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(MultiplexerChannelInitializer.class);
	/**
	 * Determines whether the debug level is enabled in the logging configuration, without the cost of a method call.
	 */
	private boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The key associated with the initial connection.
	 */
	private SelectionKey initialKey = null;
	/**
	 * Wrapper for the new connection to establish.
	 */
	private AbstractSocketChannelHandler channelHandler = null;

	/**
	 * Instantiate this initializer with the specified parameters.
	 * @param initialKey the key associated with the initial connection.
	 * @param channelHandler wrapper for the new connection to establish.
	 */
	public MultiplexerChannelInitializer(SelectionKey initialKey, AbstractSocketChannelHandler channelHandler)
	{
		this.initialKey = initialKey;
		this.channelHandler = channelHandler;
	}

	/**
	 * Perform the channel initialization.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			channelHandler.init();
		}
		catch(Exception e)
		{
			log.error(e.getMessage(), e);
		}
	}
}
