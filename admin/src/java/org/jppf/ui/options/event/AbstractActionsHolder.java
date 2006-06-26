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
package org.jppf.ui.options.event;

import java.lang.reflect.Method;
import java.util.*;
import org.apache.log4j.Logger;
import org.jppf.ui.options.*;
import org.jppf.ui.options.factory.OptionsHandler;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractActionsHolder  implements ValueChangeListener
{
	/**
	 * Log4j logger for this class.
	 */
	private static Logger log = Logger.getLogger(OptionsHandler.class);
	/**
	 * The option that triggered the action
	 */
	protected Option option = null;
	/**
	 * Mapping of option name to the method to invoke when the option's value changes.
	 */
	protected Map<String, Method> methodsMap = new HashMap<String, Method>();

	/**
	 * Performs initializations.
	 */
	public AbstractActionsHolder()
	{
		initializeMethodMap();
	}
	
	/**
	 * Initialize the mapping of option name to the method to invoke when the option's value changes.
	 */
	protected abstract void initializeMethodMap();

	/**
	 * Add the mapping of an option to a method.
	 * @param optionName the name of the option to add the mapping for.
	 * @param methodName the name of the method to map.
	 */
	public void addMapping(String optionName, String methodName)
	{
		try
		{
			Method[] methods = getClass().getMethods();
			for (Method m: methods)
			{
				if (m.getName().equals(methodName) &&
					Void.TYPE.equals(m.getReturnType()) &&
					((m.getParameterTypes() == null) || (m.getParameterTypes().length == 0)))
				{
					methodsMap.put(optionName, m);
				}
			}
		}
		catch(Exception e)
		{
			System.err.println(e.getMessage());
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * Invoked when the selection in the combo box has changed.
	 * @param event not used.
	 * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
	 */
	public void valueChanged(ValueChangeEvent event)
	{
		this.option = (Option) event.getOption();
		try
		{
			Method m = methodsMap.get(event.getOption().getName());
			if (m != null) m.invoke(this, (Object[]) null);
		}
		catch(Exception e)
		{
			OptionsPage main = (OptionsPage) event.getOption().getPath().getPathComponent(0);
			String msg = "Error occurred for "+event.getOption()+"in panel ["+main.getName()+"] : '"+e.getMessage()+"'";
			System.err.println(msg);
			log.error(msg, e);
		}
	}
}
