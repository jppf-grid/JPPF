/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jppf.security;

import java.security.*;
import java.util.*;

import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFPermissions extends PermissionCollection
{
	/**
	 * Logger for this class.
	 */
	private static Logger log = LoggerFactory.getLogger(JPPFPolicy.class);
	/**
	 * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The list of permissions in this collection.
	 */
	private List<Permission> permissions = new Vector<Permission>();

	/**
	 * Adds a permission object to the current collection of permission objects.
	 * @param permission the Permission object to add.
	 * @see java.security.PermissionCollection#add(java.security.Permission)
	 */
	public synchronized void add(Permission permission)
	{
		if (permission == null) return;
		permissions.add(permission);
	}

	/**
	 * Returns an enumeration of all the Permission objects in the collection.
	 * @return an enumeration of all the Permissions.
	 * @see java.security.PermissionCollection#elements()
	 */
	public synchronized Enumeration<Permission> elements()
	{
		return new Enumerator();
	}

	/**
	 * Checks to see if the specified permission is implied by the collection of Permission objects held in this PermissionCollection.
	 * @param permission the Permission object to compare.
	 * @return true if "permission" is implied by the permissions in the collection, false if not.
	 * @see java.security.PermissionCollection#implies(java.security.Permission)
	 */
	public synchronized boolean implies(Permission permission)
	{
		if (permission instanceof RuntimePermission)
		{
			RuntimePermission rtp = (RuntimePermission) permission;
			String actions = rtp.getActions();
			if ((actions != null) && (actions.indexOf("exitVM") >= 0))
			{
				int breakpoint = 0;
			}
		}
		List<Permission> perms = Collections.unmodifiableList(permissions);
		for (Permission p: perms)
		{
			if (p.implies(permission)) return true;
		}
		return false;
	}

	/**
	 * Marks this PermissionCollection object as "readonly". After a PermissionCollection object is marked as readonly,
	 * no new Permission objects can be added to it using add. 
	 * @see java.security.PermissionCollection#setReadOnly()
	 */
	public void setReadOnly()
	{
		//super.setReadOnly();
	}

	/**
	 * Enumerator for the permissions in the collection.
	 */
	private class Enumerator implements Enumeration<Permission>
	{
		/**
		 * Index of the current enumrated element.
		 */
		private int index = 0;
		/**
		 * Total number of elements.
		 */
		private int count = 0;
		/**
		 * The list of permissions in this collection.
		 */
		private List<Permission> enumPermissions = null;

		/**
		 * Default constructor.
		 */
		public Enumerator()
		{
			synchronized(JPPFPermissions.this)
			{
				enumPermissions = new Vector<Permission>();
				enumPermissions.addAll(permissions);
			}
			//enumPermissions = permissions;
			count = enumPermissions.size();
		}

		/**
		 * Test if this enumeration contains more elements. 
		 * @return true if and only if this enumeration object contains at least one more element to provide; false otherwise.
		 * @see java.util.Enumeration#hasMoreElements()
		 */
		public boolean hasMoreElements()
		{
			return count > index;
		}

		/**
		 * Returns the next element of this enumeration if this enumeration object has at least one more element to provide.
		 * @return the next element of this enumeration.
		 * @throws NoSuchElementException - if no more elements exist.
		 * @see java.util.Enumeration#nextElement()
		 */
		public Permission nextElement() throws NoSuchElementException
		{
			if (!hasMoreElements()) throw new NoSuchElementException("no more element in this enumeration");
			return enumPermissions.get(index++);
		}
	}
}
