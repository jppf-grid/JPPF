/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
