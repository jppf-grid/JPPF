/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
 *
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
package org.jppf.ui.options;

import javax.swing.JComponent;

/**
 * An option that displays a UI component created through a Java class.
 * @author Laurent Cohen
 */
public class JavaOption extends AbstractOption
{
	/**
	 * The fully qualified class name of the UI component to instantiate.
	 */
	protected String className = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public JavaOption()
	{
	}

	/**
	 * Initialize this boolean option with the specified parameters.
	 * @param name this component's name.
	 * @param className the fully qualified class name of the UI component to instantiate.
	 */
	public JavaOption(String name, String className)
	{
		this.name = name;
		this.className = className;
		createUI();
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		try
		{
			UIComponent = (JComponent) Class.forName(className).newInstance();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Propagate the state changes of the underlying checkbox to the listeners to this option.
	 * @see org.jppf.ui.options.AbstractOption#setupValueChangeNotifications()
	 */
	protected void setupValueChangeNotifications()
	{
	}


	/**
	 * Enable or disable this option.
	 * @param enabled true to enable this option, false to disable it.
	 * @see org.jppf.ui.options.Option#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled)
	{
	}

	/**
	 * Get the fully qualified class name of the UI component to instantiate.
	 * @return the clas name as a string.
	 */
	public synchronized String getClassName()
	{
		return className;
	}

	/**
	 * Set the fully qualified class name of the UI component to instantiate.
	 * @param className the clas name as a string.
	 */
	public synchronized void setClassName(String className)
	{
		this.className = className;
	}
}
