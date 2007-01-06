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

import java.security.*;

/**
 * Security policy for JPPF Nodes.
 * @author Laurent Cohen
 */
public class JPPFPolicy extends Policy
{
	/**
	 * ClassLoader used to retrieve the policy file.
	 */
	private ClassLoader classLoader = null;

	/**
	 * Initialize this policy.
	 * @param classLoader the <code>ClassLoader</code> used to retrieve the policy file
	 * through a call to <code>getResourceAsStream(String)</code>; may be null.
	 * @see java.lang.ClassLoader#getResourceAsStream(String).
	 */
	public JPPFPolicy(ClassLoader classLoader)
	{
		this.classLoader = classLoader;
		PermissionsFactory.getPermissions(classLoader);
	}
	
	
	/**
	 * Get the permissions for this policy.
	 * @param codesource not used.
	 * @return a collection of permissions.
	 * @see java.security.Policy#getPermissions(java.security.CodeSource)
	 */
	public PermissionCollection getPermissions(CodeSource codesource)
	{
		return PermissionsFactory.getPermissions(classLoader);
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
		return PermissionsFactory.getPermissions(classLoader);
	}
}
