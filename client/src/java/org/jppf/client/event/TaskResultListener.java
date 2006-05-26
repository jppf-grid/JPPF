/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
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
package org.jppf.client.event;

import java.util.EventListener;

/**
 * Listener interface for receiving notifications of task results received from the server.
 * @author Laurent Cohen
 */
public interface TaskResultListener extends EventListener
{
	/**
	 * Called to notify that that results of number of tasks have been received from the server.
	 * @param event the event that encapsulates the tasks that were received and related information.
	 */
	void resultsReceived(TaskResultEvent event);
}
