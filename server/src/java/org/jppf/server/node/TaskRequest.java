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
package org.jppf.server.node;

import java.nio.ByteBuffer;
import org.jppf.server.*;

/**
 * Instances of this class encapsulate execution requests to send to the nodes.
 * @author Domingos Creado
 */
class TaskRequest {
	/**
	 * The request data. 
	 */
	private Request request;
	/**
	 * Buffer used to send the request data over a socket channel.
	 */
	private ByteBuffer sending;
	/**
	 * Container for the tasks and associated metadata.
	 */
	private JPPFTaskBundle bundle;
	/**
	 * Length in bytes of the request data to send or receive.
	 */
	private long bundleBytes;

	/**
	 * Initialize this task request with the specified request data,
	 * sending buffer, bundle data and bundle bytes length.
	 * @param request the request data.
	 * @param sending buffer used to send the request data over a socket channel.
	 * @param bundle container for the tasks and associated metadata.
	 * @param bundleBytes length in bytes of the request data to send or recieve.
	 */
	public TaskRequest(Request request, ByteBuffer sending,
			JPPFTaskBundle bundle, long bundleBytes) {
		super();
		this.bundle = bundle;
		this.request = request;
		this.sending = sending;
		this.bundleBytes = bundleBytes;
	}

	/**
	 * Get the request data of this task request.
	 * @return a <code>Request</code> instance.
	 */
	public Request getRequest() {
		return request;
	}

	/**
	 * Get the buffer used to send the request data over a socket channel.
	 * @return a <code>ByteBuffer</code> instance.
	 */
	public ByteBuffer getSending() {
		return sending;
	}

	/**
	 * Get the container for the tasks and associated metadata.
	 * @return a <code>JPPFTaskBundle</code> instance.
	 */
	public JPPFTaskBundle getBundle() {
		return bundle;
	}

	/**
	 * Get the length in bytes of the request data to send or receive.
	 * @return the length as a long value.
	 */
	public long getBundleBytes() {
		return bundleBytes;
	}
}