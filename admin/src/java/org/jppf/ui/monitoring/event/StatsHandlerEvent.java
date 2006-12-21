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
package org.jppf.ui.monitoring.event;

import java.util.EventObject;

import org.jppf.ui.monitoring.data.StatsHandler;

/**
 * Event sent when the stats data has changed.
 * @author Laurent Cohen
 */
public class StatsHandlerEvent extends EventObject
{
	/**
	 * Enumeration of the types of events.
	 */
	public enum Type
	{
		/**
		 * Update with a new data snapashot.
		 */
		UPDATE,
		/**
		 * The whole dataset shall be reset. 
		 */
		RESET
	}
	/**
	 * The type of this event.
	 */
	private Type type = Type.UPDATE;

	/**
	 * Initialize this event with a specified source <code>StatsHandler</code>.
	 * @param source the stats formatter whose data has changed.
	 * @param type the type of this event.
	 */
	public StatsHandlerEvent(StatsHandler source, Type type)
	{
		super(source);
		this.type = type;
	}
	
	/**
	 * Get the <code>StatsHandler</code> source of this event.
	 * @return a <code>StatsHandler</code> instance.
	 */
	public StatsHandler getStatsFormatter()
	{
		return (StatsHandler) getSource();
	}

	/**
	 * Get the type of this event.
	 * @return the type as a typesafe <code>Type</code> enumerated value.
	 */
	public Type getType()
	{
		return type;
	}
}
