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
package org.jppf.security;

import java.security.*;

/**
 * Security policy for JPPF Nodes.
 * @author Laurent Cohen
 */
public class JPPFPolicy extends Policy
{
	/**
	 * Reference to the permissions collection for a JPPF node.
	 */
	//private Permissions jppfPermissions = null;

	/**
	 * Initialize this policy.
	 */
	public JPPFPolicy()
	{
		PermissionsFactory.getPermissions();
	}
	
	
	/**
	 * Get the permissions for this policy.
	 * @param codesource not used.
	 * @return a collection of permissions.
	 * @see java.security.Policy#getPermissions(java.security.CodeSource)
	 */
	public PermissionCollection getPermissions(CodeSource codesource)
	{
		return PermissionsFactory.getPermissions();
	}

	/**
	 * This method does nothing.
	 * @see java.security.Policy#refresh()
	 */
	public void refresh()
	{
	}


	/**
	 * Get the permissions for a specified protection domain.
	 * @param domain the protection domain to get the permissions for.
	 * @return a <code>PermissionCollection</code> instance.
	 * @see java.security.Policy#getPermissions(java.security.ProtectionDomain)
	 */
	public PermissionCollection getPermissions(ProtectionDomain domain)
	{
		return PermissionsFactory.getPermissions();
	}
}
