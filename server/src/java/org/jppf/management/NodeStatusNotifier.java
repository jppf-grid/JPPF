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

package org.jppf.management;

import org.jppf.node.event.*;
import org.jppf.utils.LocalizationUtils;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeStatusNotifier implements NodeLifeCycleListener
{
	/**
	 * Base name used for localization lookups.
	 */
	private static final String I18N_BASE = "org.jppf.server.i18n.messages";
	/**
	 * The mbean that provides information on the node's state.
	 */
	private final JPPFNodeAdmin nodeAdmin;

	/**
	 * Initialize this notifier with the specified node admin mbean.
	 * @param nodeAdmin the mbean that provides information on the node's state.
	 */
	public NodeStatusNotifier(JPPFNodeAdmin nodeAdmin)
	{
		this.nodeAdmin = nodeAdmin;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nodeStarting(NodeLifeCycleEvent event)
	{
		synchronized(nodeAdmin)
		{
			nodeAdmin.getNodeState().setConnectionStatus(localize("node.connected"));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void nodeEnding(NodeLifeCycleEvent event)
	{
		synchronized(nodeAdmin)
		{
			nodeAdmin.getNodeState().setConnectionStatus(localize("node.disconnected"));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void jobStarting(NodeLifeCycleEvent event)
	{
		synchronized(nodeAdmin)
		{
			nodeAdmin.getNodeState().setExecutionStatus(localize("node.executing"));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void jobEnding(NodeLifeCycleEvent event)
	{
		synchronized(nodeAdmin)
		{
			nodeAdmin.getNodeState().setExecutionStatus(localize("node.idle"));
		}
	}

	/**
	 * Get a localized message given its unique name and the current locale.
	 * @param message the unique name of the localized message.
	 * @return a message in the current locale, or the default locale 
	 * if the localization for the current locale is not found. 
	 */
	private static String localize(String message)
	{
		return LocalizationUtils.getLocalized(I18N_BASE, message);
	}
}
