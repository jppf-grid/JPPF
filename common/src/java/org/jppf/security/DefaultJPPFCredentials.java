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

import org.jppf.utils.JPPFUuid;

/**
 * Default implementation of the JPPFCredentials interface provided for convenience.
 * All the canXXX() methods of this class return true.
 * Other implementations can extennd this class and override any method.
 * @author Laurent Cohen
 */
public class DefaultJPPFCredentials implements JPPFCredentials
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
	private JPPFSignature sig = null;

	/**
	 * No default instantiation allowed fdor this class.
	 */
	private DefaultJPPFCredentials()
	{
	}

	/**
	 * Create a JPPF signature witht he specified parameters.
	 * @param uuid uuid for the owner of these credentials.
	 * @param id identifier for the owner of these credentials.
	 * @param sig the signature associated with these credentials.
	 */
	public DefaultJPPFCredentials(String uuid, String id, JPPFSignature sig)
	{
		this.uuid = uuid;
		this.id = id;
		this.sig = sig;
	}

	/**
	 * This method always returns true.
	 * @param credentials not used.
	 * @return true.
	 * @see org.jppf.security.JPPFCredentials#canAdministrate(org.jppf.security.JPPFCredentials)
	 */
	public boolean canAdministrate(JPPFCredentials credentials)
	{
		return true;
	}

	/**
	 * This method always returns true.
	 * @param credentials not used.
	 * @return true.
	 * @see org.jppf.security.JPPFCredentials#canExecute(org.jppf.security.JPPFCredentials)
	 */
	public boolean canExecute(JPPFCredentials credentials)
	{
		return true;
	}

	/**
	 * This method always returns true.
	 * @param credentials not used.
	 * @return true.
	 * @see org.jppf.security.JPPFCredentials#canReceive(org.jppf.security.JPPFCredentials)
	 */
	public boolean canReceive(JPPFCredentials credentials)
	{
		return true;
	}

	/**
	 * This method always returns true.
	 * @param credentials not used.
	 * @return true.
	 * @see org.jppf.security.JPPFCredentials#canSend(org.jppf.security.JPPFCredentials)
	 */
	public boolean canSend(JPPFCredentials credentials)
	{
		return true;
	}

	/**
	 * Get the identifier for these credentials' owner. It doesn't have to be universally unique,
	 * and is used to identify the entity that makes use of the framework. The entity can be an organization,
	 * user, software component, etc...
	 * @return the identifier as a String.
	 * @see org.jppf.security.JPPFCredentials#getIdentifier()
	 */
	public String getIdentifier()
	{
		return id;
	}

	/**
	 * Get the signature associated with these credentials.
	 * @return a <code>JPPFSignature</code> instance.
	 * @see org.jppf.security.JPPFCredentials#getSignature()
	 */
	public JPPFSignature getSignature()
	{
		return sig;
	}

	/**
	 * Get the unique universal identifier of the component that owns these credentials.
	 * @return the uuid as a String.
	 * @see org.jppf.security.JPPFCredentials#getUuid()
	 */
	public String getUuid()
	{
		return uuid;
	}

	/**
	 * Get a string representation of this instance.
	 * @return a string displaying information about these credentials.
	 * @see java.lang.Object#toString()
	 */
	public String toString()
	{
		return "DefaultCredentials id=" + id;
	}
}
