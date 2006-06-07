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
 * This class is simply a holder for the actions associated with
 * the bundle size tuning configuration page.
 * @author Laurent Cohen
 */
public class BundleSizeTuningActions
{
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
			AbstractOption elt = (AbstractOption) option.findElement("../../bundleSize");
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
			OptionsPage page = OptionsHandler.getPage("Admin");
			AbstractOption elt = (AbstractOption) page.findAllWithName("actualPwd").get(0);
			String pwd = (String) elt.getValue();
			Map<String, Object> params = new HashMap<String, Object>();
			elt = (AbstractOption) option.findElement("../../bundleSize");
			params.put(AdminRequest.BUNDLE_SIZE_PARAM, elt.getValue());
			params.put(AdminRequest.BUNDLE_TUNING_TYPE_PARAM, "manual");
			String msg = StatsHandler.getInstance().changeSettings(pwd, params);
			if (msg != null) ((AbstractOption) option.findElement("/msgText")).setValue(msg);
		}
	}

	/**
	 * Value change listener for the checkbox that controls whether
	 * the bundle size settings should be set manually or automatically. 
	 */
	public static class ToggleManualAutoListener implements ValueChangeListener
	{
		/**
		 * Invoked when the state of the check box has changed.
		 * @param event the event that triggered the notification..
		 * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
		 */
		public void valueChanged(ValueChangeEvent event)
		{
			boolean b = (Boolean) event.getOption().getValue();
			OptionElement manual = event.getOption().findElement("../ManualConfig");
			OptionElement auto = event.getOption().findElement("../AutoConfig");
			manual.setEnabled(b);
			auto.setEnabled(!b);
		}
	}

	/**
	 * Action associated with the button to apply a new bundle size to the server settings.
	 */
	public static class ApplyAutoSettingsAction extends OptionAction
	{
		/**
		 * Perform the action.
		 * @param event not used.
		 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
		 */
		public void actionPerformed(ActionEvent event)
		{
			OptionsPage page = OptionsHandler.getPage("Admin");
			AbstractOption elt = (AbstractOption) page.findAllWithName("actualPwd").get(0);
			String pwd = (String) elt.getValue();
			Map<String, Object> params = new HashMap<String, Object>();
			params.put(AdminRequest.BUNDLE_TUNING_TYPE_PARAM, "auto");
			elt = (AbstractOption) option.findElement("/").findAllWithName("MinSamplesToAnalyse").get(0);
			params.put(elt.getName(), elt.getValue());
			elt = (AbstractOption) option.findElement("/").findAllWithName("MinSamplesToCheckConvergence").get(0);
			params.put(elt.getName(), elt.getValue());
			elt = (AbstractOption) option.findElement("/").findAllWithName("MaxDeviation").get(0);
			params.put(elt.getName(), elt.getValue());
			elt = (AbstractOption) option.findElement("/").findAllWithName("MaxGuessToStable").get(0);
			params.put(elt.getName(), elt.getValue());
			elt = (AbstractOption) option.findElement("/").findAllWithName("SizeRatioDeviation").get(0);
			params.put(elt.getName(), elt.getValue());
			elt = (AbstractOption) option.findElement("/").findAllWithName("DecreaseRatio").get(0);
			params.put(elt.getName(), elt.getValue());
			params.put("manual", Boolean.FALSE);
			String msg = StatsHandler.getInstance().changeSettings(pwd, params);
			if (msg != null) ((AbstractOption) option.findElement("/msgText")).setValue(msg);
		}
	}
}
