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

import java.awt.event.ActionEvent;
import java.util.*;
import org.jppf.server.protocol.AdminRequest;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.*;
import org.jppf.ui.options.event.*;

/**
 * The admin panel.
 * @author Laurent Cohen
 */
public class AdminOptionsPanel extends OptionPanel
{
	/**
	 * Action associated with the button to shutdown and/or restart the server.
	 */
	public static class ShutdownRestartAction extends OptionAction
	{
		/**
		 * Perform the action.
		 * @param event not used.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event)
		{
			Option elt = (Option) option.findElement("../Shutdown delay");
			Number n = (Number) elt.getValue();
			long shutdownDelay = n.longValue();
			long restartDelay = 0L;
			String command = null;
			elt = (Option) option.findElement("../Restart");
			if ((Boolean) elt.getValue())
			{
				elt = (Option) option.findElement("../Restart delay");
				n = (Number) elt.getValue();
				restartDelay = n.longValue();
				command = AdminRequest.SHUTDOWN_RESTART;
			}
			else command = AdminRequest.SHUTDOWN;
			elt = (Option) option.findElement("/Admin password/actualPwd");
			String pwd = (String) elt.getValue();
			String msg = StatsHandler.getInstance().requestShutdownRestart(pwd, command, shutdownDelay, restartDelay);
			((AbstractOption) option.findElement("/msgText")).setValue(msg);
		}
	}

	/**
	 * Action associated with the button to change the administrator password.
	 */
	public static class ChangePasswordAction extends OptionAction
	{
		/**
		 * Perform the action.
		 * @param event not used.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event)
		{
			AbstractOption elt = (AbstractOption) option.findElement("/Admin password/actualPwd");
			String pwd = (String) elt.getValue();
			elt = (AbstractOption) option.findElement("/Admin password/newPwd");
			String newPwd = (String) elt.getValue();
			elt = (AbstractOption) option.findElement("/Admin password/confirmPwd");
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
	}

	/**
	 * Value change listener for the checkbox that controls whether
	 * the server should be restarted after shutdown. 
	 */
	public static class RestartCheckboxListener implements ValueChangeListener
	{
		/**
		 * Invoked when the state of the check box has changed.
		 * @param event not used.
		 * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
		 */
		public void valueChanged(ValueChangeEvent event)
		{
			Option restart = (Option) event.getOption().findElement("../Restart delay");
			restart.setEnabled((Boolean) event.getOption().getValue());
		}
	}

	/**
	 * Action associated with the button to refresh the bundle size from the server.
	 */
	public static class RefreshSettingsAction extends OptionAction
	{
		/**
		 * Perform the action.
		 * @param event not used.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event)
		{
			int n = StatsHandler.getInstance().getLatestStats().bundleSize;
			if (n <= 0) return;
			AbstractOption elt = (AbstractOption) option.findElement("/Config/bundleSize");
			elt.setValue(new Long(n));
		}
	}

	/**
	 * Action associated with the button to apply a new bundle size to the server settings.
	 */
	public static class ApplySettingsAction extends OptionAction
	{
		/**
		 * Perform the action.
		 * @param event not used.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event)
		{
			AbstractOption elt = (AbstractOption) option.findElement("/Admin password/actualPwd");
			String pwd = (String) elt.getValue();
			Map<String, Object> params = new HashMap<String, Object>();
			elt = (AbstractOption) option.findElement("/Config/bundleSize");
			params.put(AdminRequest.BUNDLE_SIZE_PARAM, elt.getValue());
			String msg = StatsHandler.getInstance().changeSettings(pwd, params);
			if (msg != null) ((AbstractOption) option.findElement("/msgText")).setValue(msg);
		}
	}
}
