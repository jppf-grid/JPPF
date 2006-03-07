/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of
 * the GNU General Public License as published by the Free Software Foundation;
 * either version 2 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program;
 * if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307 USA
 */
package org.jppf.server;

import java.util.concurrent.*;
import org.apache.log4j.Logger;
import org.jppf.server.protocol.JPPFTaskWrapper;
import static org.jppf.server.JPPFStatsUpdater.*;

/**
 * Implementation of a generic blocking queue, to allow asynchronous access from a large number of threads.
 * @author Laurent Cohen
 */
public class JPPFQueue
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(JPPFQueue.class);
	/**
	 * Executable tasks queue, available for execution nodes to pick from.
	 * This queue behaves as a FIFO queue and is thread-safe for atomic <code>add()</code> and <code>take()</code> operations.
	 */
	private BlockingQueue<JPPFTaskWrapper> queue = new LinkedBlockingQueue<JPPFTaskWrapper>();

	/**
	 * Add an object to the queue.
	 * @param wrapper the object to add to the queue.
	 */
	public void addObject(JPPFTaskWrapper wrapper)
	{
		wrapper.setQueueEntryTime(System.currentTimeMillis());
		queue.add(wrapper);
		taskInQueue();
	}
	
	/**
	 * Get the next object in the queue. This method waits until the queue has at least one object.
	 * @return the most recent object that was added to the queue.
	 */
	public JPPFTaskWrapper nextObject()
	{
		try
		{
			JPPFTaskWrapper wrapper = queue.take();
			taskOutOfQueue(System.currentTimeMillis() - wrapper.getQueueEntryTime());
			return wrapper;
		}
		catch(InterruptedException e)
		{
			log.error(e.getMessage(), e);
			return null;
		}
	}
}
