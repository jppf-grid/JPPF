/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2009 JPPF Team.
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

import org.apache.commons.logging.*;
import org.jppf.ui.monitoring.node.TopologyData;
import org.jppf.ui.utils.GuiUtils;

/**
 * Abstract superclass for popup menu actions used in the ui.  
 * @author Laurent Cohen
 */
public abstract class JPPFAbstractNodeAction extends AbstractAction
{
	/**
	 * Logger for this class.
	 */
	protected static Log log = LogFactory.getLog(JPPFAbstractNodeAction.class);
	/**
	 * Determines whether debug log statements are enabled.
	 */
	protected static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * JMX connections this action applies to.
	 */
	protected TopologyData[] dataArray = null;

	/**
	 * Initialize this action with the specified JMX connections.
	 * @param dataArray - the information on the nodes this action applies to.
	 */
	protected JPPFAbstractNodeAction(TopologyData...dataArray)
	{
		this.dataArray = dataArray;
	}

	/**
	 * Set the icon for this action using the specified image file name.
	 * @param name the name of the icon image file.
	 */
	protected void setupIcon(String name)
	{
		if (name != null) putValue(Action.SMALL_ICON, GuiUtils.loadIcon(name));
	}
}
