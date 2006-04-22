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
package org.jppf;

/**
 * This error is thrown to notify a node that its code is obsolete and it should dynamically reload itself.
 * @author Laurent Cohen
 */
public class JPPFNodeReloadNotification extends JPPFError
{
	/**
	 * Initialize this notification with a specified message.
	 * @param message a text message indicating the reason for this notification.
	 */
	public JPPFNodeReloadNotification(String message)
	{
		super(message);
	}
}
