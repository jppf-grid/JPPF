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
package org.jppf.security;

import java.io.Serializable;
import org.jppf.utils.JPPFUuid;

/**
 * <p>This interface encapsulates the required information and behaviour to manage
 * how a component (driver, node or client) interacts with the others, in terms
 * of authorizations and clearance.
 * <p>Organizations  - or users or entities - willing to participate in a JPPF grid network
 * must have an implementation of this interface provided with every component they send
 * onto the network. If it is not present, any component of this organization should be denied access.
 * @author Laurent Cohen
 */
public final class JPPFSecurityContext implements Serializable
{
	/**
	 * Uuid for the owner of these credentials.
	 */
	private String uuid = new JPPFUuid().toString();
	/**
	 * Identifier for the owner of these credentials.
	 */
	private String id = new JPPFUuid().toString();
	/**
	 * The signature associated with these credentials.
	 */
	private JPPFCredentials credentials = null;
	/**
	 * Security domain these credentials relate to.
	 */
	private String domain = null;

	/**
	 * No default instantiation allowed for this class.
	 */
	private JPPFSecurityContext()
	{
	}

	/**
	 * Create a JPPF signature witht he specified parameters.
	 * @param uuid uuid for the owner of these credentials.
	 * @param id identifier for the owner of these credentials.
	 * @param credentials the signature associated with these credentials.
	 */
	public JPPFSecurityContext(String uuid, String id, JPPFCredentials credentials)
	{
		this.uuid = uuid;
		this.id = id;
		this.credentials = credentials;
	}

	/**
	 * Get the unique universal identifier of the component that owns these credentials.
	 * @return the uuid as a String.
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Get the identifier for these credentials' owner. It doesn't have to be universally unique,
	 * and is used to identify the entity that makes use of the framework. The entity can be an organization,
	 * user, software component, etc...
	 * @return the identifier as a String.
	 */
	public String getIdentifier()
	{
		return id;
	}

	/**
	 * Get the signature for these credentials.
	 * @return a <code>JPPFCredentials</code> instance.
	 */
	public JPPFCredentials getSignature()
	{
		return credentials;
	}

	/**
	 * Determine whether the owner of these credentials can execute tasks from another
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can execute the entity's tasks, false otherwise.
	 */
	public boolean canExecute(JPPFSecurityContext credentials)
	{
		return true;
	}

	/**
	 * Determine whether the owner of these credentials can send tasks to another
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can send tasks to the other entity, false otherwise.
	 */
	public boolean canSend(JPPFSecurityContext credentials)
	{
		return true;
	}

	/**
	 * Determine whether the owner of these credentials can receive tasks from another
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can receive tasks from the other entity, false otherwise.
	 */
	public boolean canReceive(JPPFSecurityContext credentials)
	{
		return true;
	}

	/**
	 * Determine whether the owner of these credentials can administrate an other
	 * entity on the network.
	 * @param credentials the entity's security credentials.
	 * @return true if the owner can administrate the other entity, false otherwise.
	 */
	public boolean canAdministrate(JPPFSecurityContext credentials)
	{
		return true;
	}

	/**
	 * Get a string representation of this instance.
	 * @return a string displaying information about these credentials.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "JPPF Credentials id=" + id;
	}

	/**
	 * Get the security domain these credentials relate to.
	 * @return the domain as a string.
	 */
	public String getDomain()
	{
		return domain;
	}

	/**
	 * Set the security domain these credentials relate to.
	 * @param domain the domain as a string.
	 */
	public void setDomain(String domain)
	{
		this.domain = domain;
	}
}
