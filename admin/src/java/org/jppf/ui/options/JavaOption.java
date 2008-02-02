/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2008 JPPF Team.
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

import java.awt.event.*;

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
	 * Name of the mouseListener to set on this element.
	 */
	protected String mouseListenerClassName = null;

	/**
	 * Constructor provided as a convenience to facilitate the creation of
	 * option elements through reflexion.
	 */
	public JavaOption()
	{
	}

	/**
	 * Create the UI components for this option.
	 */
	public void createUI()
	{
		try
		{
			UIComponent = (JComponent) Class.forName(getClassName()).newInstance();
			if (mouseListenerClassName != null)
			{
				JavaOptionMouseListener ml =
					(JavaOptionMouseListener) Class.forName(mouseListenerClassName).newInstance();
				ml.setOption(this);
				UIComponent.addMouseListener(ml);
			}
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

	/**
	 * Abstract superclass for mouse listeners set on this type of option.
	 */
	public static abstract class JavaOptionMouseListener extends MouseAdapter
	{
		/**
		 * The option on which this listener is set.
		 */
		protected JavaOption option = null;

		/**
		 * Get the option on which this listener is set.
		 * @return a <code>JavaOption</code> instance.
		 */
		public JavaOption getOption()
		{
			return option;
		}

		/**
		 * Set the option on which this listener is set.
		 * @param option a <code>JavaOption</code> instance.
		 */
		public void setOption(JavaOption option)
		{
			this.option = option;
		}
	}

	/**
	 * Get the class name of the mouseListener to set on this element.
	 * @return the class name as a string.
	 */
	public String getMouseListenerClassName()
	{
		return mouseListenerClassName;
	}

	/**
	 * Set the class name of the mouseListener to set on this element.
	 * @param mouseListenerClassName the class name as a string.
	 */
	public void setMouseListenerClassName(String mouseListenerClassName)
	{
		this.mouseListenerClassName = mouseListenerClassName;
		
	}
}
