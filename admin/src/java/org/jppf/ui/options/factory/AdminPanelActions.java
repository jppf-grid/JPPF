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
package org.jppf.ui.options.factory;

import org.jppf.server.protocol.AdminRequest;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.*;

/**
 * The admin panel.
 * @author Laurent Cohen
 */
public class AdminPanelActions extends AbstractActionsHolder
{
	/**
	 * Initialize the mapping of an option name to the method to invoke when the option's value changes.
	 * @see org.jppf.ui.options.factory.AbstractActionsHolder#initializeMethodMap()
	 */
	protected void initializeMethodMap()
	{
		addMapping("Perform_Now", "shutdownRestartPressed");
		addMapping("Change_password", "changePasswordPressed");
		addMapping("Restart", "restartFlagChanged");
	}

	/**
	 * Action associated with the button to shutdown and/or restart the server.
	 */
	public void shutdownRestartPressed()
	{
		Option elt = (Option) option.findElement("../Shutdown_delay");
		Number n = (Number) elt.getValue();
		long shutdownDelay = n.longValue();
		long restartDelay = 0L;
		String command = null;
		elt = (Option) option.findElement("../Restart");
		if ((Boolean) elt.getValue())
		{
			elt = (Option) option.findElement("../Restart_delay");
			n = (Number) elt.getValue();
			restartDelay = n.longValue();
			command = AdminRequest.SHUTDOWN_RESTART;
		}
		else command = AdminRequest.SHUTDOWN;
			
		elt = (Option) option.findFirstWithName("/actualPwd");
		String pwd = (String) elt.getValue();
		String msg = StatsHandler.getInstance().requestShutdownRestart(pwd, command, shutdownDelay, restartDelay);
		((AbstractOption) option.findElement("/msgText")).setValue(msg);
	}

	/**
	 * Action associated with the button to change the administrator password.
	 */
	public void changePasswordPressed()
	{
		AbstractOption elt = (AbstractOption) option.findFirstWithName("/actualPwd");
		String pwd = (String) elt.getValue();
		elt = (AbstractOption) option.findFirstWithName("/newPwd");
		String newPwd = (String) elt.getValue();
		elt = (AbstractOption) option.findFirstWithName("/confirmPwd");
		String confirmPwd = (String) elt.getValue();
		if (validateNewPassword(newPwd, confirmPwd))
		{
			String msg = StatsHandler.getInstance().changeAdminPassword(pwd, newPwd);
			((AbstractOption) option.findElement("/msgText")).setValue(msg);
		}
	}

	/**
	 * Perform a validation of the new password before a password change.
	 * @param newPwd the new admin password to set.
	 * @param confirmPwd a confirmation of the new password.
	 * @return true if the new password is valid, false otherwise.
	 */
	public boolean validateNewPassword(String newPwd, String confirmPwd)
	{
		String msg = null;
		if ((newPwd == null) || "".equals(newPwd.trim()))
		{
			msg = "The new password must not be empty, with at least one non-space character";
		}
		else if (!newPwd.equals(confirmPwd))
		{
			msg = "The new password and the confirmation do not match";
		}
		if (msg != null) ((AbstractOption) option.findElement("/msgText")).setValue(msg);
		return msg == null;
	}

	/**
	 * Value change listener for the checkbox that controls whether
	 * the server should be restarted after shutdown. 
	 */
	public void restartFlagChanged()
	{
		Option restart = (Option) option.findElement("../Restart_delay");
		restart.setEnabled((Boolean) option.getValue());
	}
}
