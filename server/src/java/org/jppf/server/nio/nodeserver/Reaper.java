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

package org.jppf.server.nio.nodeserver;

import java.nio.channels.*;
import java.util.TimerTask;

import org.apache.commons.logging.*;
import org.jppf.server.nio.NioContext;
import org.jppf.utils.NetworkUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class Reaper extends TimerTask
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(Reaper.class);
	/**
	 * Determines whether debug-level logging is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The selector to check.
	 */
	private Selector selector = null;

	/**
	 * Initialize this reaper with the specified selector.
	 * @param selector the selector to check.
	 */
	public Reaper(Selector selector)
	{
		this.selector = selector;
	}

	/**
	 * Perform this task.
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		try
		{
			Exception ex = null;
			SelectionKey[] keys = selector.keys().toArray(new SelectionKey[0]);
			for (SelectionKey key: keys)
			{
				try
				{
					if (!NetworkUtils.isKeyValid(key))
					{
						NioContext ctx = (NioContext) key.attachment();
						ctx.handleException((SocketChannel) key.channel());
					}
				}
				catch(Exception e)
				{
					if (ex == null) ex = e;
				}
			}
			if (ex != null) throw ex;
		}
		catch(Exception e)
		{
			if (debugEnabled) log.debug(e.getMessage(), e);
			else log.error(e);
		}
	}
}
