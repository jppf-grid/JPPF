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

import org.apache.commons.logging.*;
import org.jppf.server.nio.ChannelWrapper;


/**
 * Context associated with a channel serving tasks to a local (in-VM) node.
 * @author Laurent Cohen
 */
public class LocalNodeContext extends AbstractNodeContext
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(LocalNodeContext.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();

	/**
	 * {@inheritDoc}.
	 */
	public AbstractNodeMessage newMessage()
	{
		return new LocalNodeMessage();
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean writeMessage(ChannelWrapper<?> channel) throws Exception
	{
		boolean b = super.writeMessage(channel);
		if (debugEnabled) log.debug("wrote " + nodeMessage + " to " + channel);
		((LocalNodeWrapperHandler) channel).setMessage((LocalNodeMessage) nodeMessage);
		return b;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setNodeMessage(AbstractNodeMessage nodeMessage, ChannelWrapper<?> channel)
	{
		super.setNodeMessage(nodeMessage, channel);
		((LocalNodeWrapperHandler) channel).wakeUp();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setState(NodeState state)
	{
		if (NodeState.SENDING_BUNDLE.equals(this.state) && NodeState.IDLE.equals(state))
		{
			log.debug("debug stack", new Exception());
			int breakpoint = 0;
		}
		super.setState(state);
	}
}
