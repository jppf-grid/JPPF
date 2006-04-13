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
package org.jppf.server.protocol;

import java.util.*;

/**
 * Request header for an admin request.
 * @author Laurent Cohen
 */
public class AdminRequest extends JPPFRequestHeader
{
	/**
	 * Admin command for scheduled shutdown of the server.
	 */
	public static final String SHUTDOWN = "shutdown";
	/**
	 * Admin command for scheduled shutdown and restart of the server.
	 */
	public static final String SHUTDOWN_RESTART = "shutdown.restart";
	/**
	 * Admin command for scheduled shutdown and restart of the server.
	 */
	public static final String CHANGE_PASSWORD = "change.pwd";
	/**
	 * Admin command for setting the size of the task bundles used by the server and nodes.
	 */
	public static final String CHANGE_SETTINGS = "set.bundle.size";
	/**
	 * Parameter name for the administration command to perform.
	 */
	public static final String COMMAND_PARAM = "command";
	/**
	 * Parameter name for the key, in encrypted format, used to decrypt the password.
	 */
	public static final String KEY_PARAM = "key";
	/**
	 * Parameter name for the administration password in encrypted format.
	 */
	public static final String PASSWORD_PARAM = "pwd";
	/**
	 * Parameter name for the new administration password in encrypted format, for password change.
	 */
	public static final String NEW_PASSWORD_PARAM = "pwd.new";
	/**
	 * Parameter name for the delay before shutting down the server.
	 */
	public static final String SHUTDOWN_DELAY_PARAM = "shutdown.delay";
	/**
	 * Parameter name for the delay before restarting the server.
	 */
	public static final String RESTART_DELAY_PARAM = "restart.delay";
	/**
	 * Parameter name for the response message to this request.
	 */
	public static final String RESPONSE_PARAM = "response";
	/**
	 * Parameter name for the size of the task bundles used by the server and nodes.
	 */
	public static final String BUNDLE_SIZE_PARAM = "bundle.size";
	/**
	 * Map holding the parameters of the request.
	 */
	private Map<String, Object> parameters = new HashMap<String, Object>();

	/**
	 * Set a parameter of this request.
	 * @param name the name of the parameter to set.
	 * @param value the value of the parameter to set.
	 */
	public void setParameter(String name, Object value)
	{
		parameters.put(name, value);
	}

	/**
	 * Get the value of a parameter of this request.
	 * @param name the name of the parameter to get.
	 * @return the value of the parameter to set.
	 */
	public Object getParameter(String name)
	{
		return parameters.get(name);
	}
}
