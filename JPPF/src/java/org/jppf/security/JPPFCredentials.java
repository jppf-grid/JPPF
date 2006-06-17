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
package org.jppf.security;

import java.io.Serializable;

/**
 * <p>This interface encapsulates the required information and behaviour to manage
 * how a component (driver, node or client) interacts with the others, in terms
 * of authorizations and clearance.
 * <p>Organizations  - or users or entities - willing to participate in a JPPF grid network
 * must have an implementation of this interface provided with every component they send
 * onto the network. If it is not present, any component of this organization should be denied access.
 * @author Laurent Cohen
 */
public interface JPPFCredentials extends Serializable
{
	/**
	 * Get the unique universal identifier of the component that owns these credentials.
	 * @return the uuid as a String.
	 */
	String getUuid();
	/**
	 * Get the identifier for these credentials' owner. It doesn't have to be universally unique,
	 * and is used to identify the entity that makes use of the framework. The entity can be an organization,
	 * user, software component, etc...
	 * @return the identifier as a String.
	 */
	String getIdentifier();
	/**
	 * Get the signature for these credentials.
	 * @return a <code>JPPFSignature</code> instance.
	 */
	JPPFSignature getSignature();
	/**
	 * Determine whether the owner of these credentials can execute tasks from another
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can execute the entity's tasks, false otherwise.
	 */
	boolean canExecute(JPPFCredentials credentials);
	/**
	 * Determine whether the owner of these credentials can send tasks to another
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can send tasks to the other entity, false otherwise.
	 */
	boolean canSend(JPPFCredentials credentials);
	/**
	 * Determine whether the owner of these credentials can receive tasks from another
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can receive tasks from the other entity, false otherwise.
	 */
	boolean canReceive(JPPFCredentials credentials);
	/**
	 * Determine whether the owner of these credentials can administrate an other
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can administrate the other entity, false otherwise.
	 */
	boolean canAdministrate(JPPFCredentials credentials);
}
