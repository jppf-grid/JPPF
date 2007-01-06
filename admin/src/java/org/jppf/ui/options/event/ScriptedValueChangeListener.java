/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2007 JPPF Team.
 * http://www.jppf.org
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

import java.util.*;
import javax.swing.tree.TreePath;
import org.jppf.scripting.*;
import org.jppf.ui.options.OptionElement;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;

/**
 * Implementation of ValueChangeListener for script-based event listeners.
 * @author Laurent Cohen
 */
public class ScriptedValueChangeListener implements ValueChangeListener
{
	/**
	 * Name of the scripting language to use.
	 */
	private String language = null;
	/**
	 * The actual source of the script to execute.
	 */
	private String script = null;
	/**
	 * A wrapper around the scripting engine.
	 */
	private ScriptRunner runner = null;

	/**
	 * Initialize this listener with a specified scriot alnguage and script source.
	 * @param language the name of the scripting language to use.
	 * @param content the actual source of the script to execute.
	 */
	public ScriptedValueChangeListener(String language, String content)
	{
		this.language = language;
		this.script = content;
		runner = ScriptRunnerFactory.makeScriptRunner(this.language);
	}

	/**
	 * Method called when the value of an option has changed.
	 * This method actually executes the script that is specified in the XML document
	 * fropm which the UI component was built.
	 * @param event the event encapsulating the source of the event.
	 * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
	 */
	public void valueChanged(ValueChangeEvent event)
	{
		OptionElement option = event.getOption();
		TreePath path = option.getPath();
		StringBuilder sb = new StringBuilder();
		// add the scripts defined in the option and all its ancestors to the one in the listener.
		// only scripts written in the same scripting language are added.
		for (Object o: path.getPath())
		{
			OptionElement elt = (OptionElement) o;
			for (ScriptDescriptor desc: elt.getScripts())
			{
				if (language.equals(desc.language))
				{
					sb.append(desc.source).append("\n");
				}
			}
		}
		sb.append(script);
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("root", option.getRoot());
		variables.put("option", option);
		try
		{
			runner.evaluate(sb.toString(), variables);
		}
		catch(JPPFScriptingException e)
		{
			e.printStackTrace();
		}
	}
}
