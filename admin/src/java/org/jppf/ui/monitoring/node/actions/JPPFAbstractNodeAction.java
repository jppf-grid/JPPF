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

package org.jppf.ui.monitoring.node.actions;

import javax.swing.*;

import org.jppf.ui.monitoring.node.TopologyData;
import org.jppf.ui.utils.GuiUtils;
import org.slf4j.*;

/**
 * Abstract superclass for popup menu actions used in the ui.
 * @author Laurent Cohen
 */
public abstract class JPPFAbstractNodeAction extends AbstractAction
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFAbstractNodeAction.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * JMX connections this action applies to.
	 */
	protected TopologyData[] dataArray = null;

	/**
	 * Initialize this action with the specified JMX connections.
	 * @param dataArray - the information on the nodes this action applies to.
	 */
	protected JPPFAbstractNodeAction(final TopologyData...dataArray)
	{
		this.dataArray = dataArray;
	}

	/**
	 * Set the icon for this action using the specified image file name.
	 * @param name the name of the icon image file.
	 */
	protected void setupIcon(final String name)
	{
		if (name != null) putValue(Action.SMALL_ICON, GuiUtils.loadIcon(name));
	}
}
