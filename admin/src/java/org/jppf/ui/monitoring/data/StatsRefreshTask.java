/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */

package org.jppf.ui.monitoring.data;

import java.util.TimerTask;

import org.jppf.client.JPPFClientConnection;

/**
 * Instances of this class are tasks run periodically from a timer thread.
 * @author Laurent Cohen
 */
public class StatsRefreshTask extends TimerTask
{
	/**
	 * Client connection ot request the data from.
	 */
	private JPPFClientConnection connection = null;

	/**
	 * Initialize this task with a specified client connection.
	 * @param connection the connection to use to request data.
	 */
	public StatsRefreshTask(JPPFClientConnection connection)
	{
		this.connection = connection;
	}

	/**
	 * Request an update from the JPPF driver.
	 * @see java.util.TimerTask#run()
	 */
	public void run()
	{
		StatsHandler.getInstance().requestUpdate(connection);
	}
}
