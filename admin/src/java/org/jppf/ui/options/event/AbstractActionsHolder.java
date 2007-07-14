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
package org.jppf.ui.options.event;

import java.lang.reflect.Method;
import java.util.*;

import org.apache.commons.logging.*;
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
	private static Log log = LogFactory.getLog(OptionsHandler.class);
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
