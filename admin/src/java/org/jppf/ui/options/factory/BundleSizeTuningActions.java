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

import java.util.*;
import org.jppf.server.protocol.AdminRequest;
import org.jppf.ui.monitoring.data.StatsHandler;
import org.jppf.ui.options.*;

/**
 * This class is simply a holder for the actions associated with
 * the bundle size tuning configuration page.
 * @author Laurent Cohen
 */
public class BundleSizeTuningActions extends AbstractActionsHolder
{
	/**
	 * Initialize the mapping of an option name to the method to invoke when the option's value changes.
	 * @see org.jppf.ui.options.factory.AbstractActionsHolder#initializeMethodMap()
	 */
	protected void initializeMethodMap()
	{
		addMapping("Refresh", "refreshSettingsPressed");
		addMapping("ApplyManual", "applyManualSettingsPressed");
		addMapping("Manual", "manualAutoFlagChanged");
		addMapping("ApplyAuto", "applyAutoSettingPressed");
	}

	/**
	 * Action associated with the button to refresh the bundle size from the server.
	 */
	public void refreshSettingsPressed()
	{
		int n = StatsHandler.getInstance().getLatestStats().bundleSize;
		if (n <= 0) return;
		AbstractOption elt = (AbstractOption) option.findFirstWithName("/bundleSize");
		elt.setValue(new Long(n));
	}

	/**
	 * Action associated with the button to apply a new bundle size to the server settings.
	 */
	public void applyManualSettingsPressed()
	{
		OptionsPage page = OptionsHandler.getPage("Admin");
		AbstractOption elt = (AbstractOption) page.findFirstWithName("/actualPwd");
		String pwd = (String) elt.getValue();
		Map<String, Object> params = new HashMap<String, Object>();
		elt = (AbstractOption) option.findFirstWithName("/bundleSize");
		params.put(AdminRequest.BUNDLE_SIZE_PARAM, elt.getValue());
		params.put(AdminRequest.BUNDLE_TUNING_TYPE_PARAM, "manual");
		String msg = StatsHandler.getInstance().changeSettings(pwd, params);
		if (msg != null) ((AbstractOption) option.findElement("/msgText")).setValue(msg);
	}

	/**
	 * Value change listener for the checkbox that controls whether
	 * the bundle size settings should be set manually or automatically. 
	 */
	public void manualAutoFlagChanged()
	{
		boolean b = (Boolean) option.getValue();
		option.findElement("../ManualConfig").setEnabled(b);
		option.findElement("../AutoConfig").setEnabled(!b);
	}

	/**
	 * Action associated with the button to apply a new auto-bundling profile.
	 */
	public void applyAutoSettingPressed()
	{
		OptionsPage page = OptionsHandler.getPage("Admin");
		AbstractOption elt = (AbstractOption) page.findFirstWithName("actualPwd");
		String pwd = (String) elt.getValue();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put(AdminRequest.BUNDLE_TUNING_TYPE_PARAM, "auto");
		elt = (AbstractOption) option.findFirstWithName("/MinSamplesToAnalyse");
		params.put(elt.getName(), elt.getValue());
		elt = (AbstractOption) option.findFirstWithName("/MinSamplesToCheckConvergence");
		params.put(elt.getName(), elt.getValue());
		elt = (AbstractOption) option.findFirstWithName("/MaxDeviation");
		params.put(elt.getName(), elt.getValue());
		elt = (AbstractOption) option.findFirstWithName("/MaxGuessToStable");
		params.put(elt.getName(), elt.getValue());
		elt = (AbstractOption) option.findFirstWithName("/SizeRatioDeviation");
		params.put(elt.getName(), elt.getValue());
		elt = (AbstractOption) option.findFirstWithName("/DecreaseRatio");
		params.put(elt.getName(), elt.getValue());
		params.put("manual", Boolean.FALSE);
		String msg = StatsHandler.getInstance().changeSettings(pwd, params);
		if (msg != null) ((AbstractOption) option.findElement("/msgText")).setValue(msg);
	}
}
