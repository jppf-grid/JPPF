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
