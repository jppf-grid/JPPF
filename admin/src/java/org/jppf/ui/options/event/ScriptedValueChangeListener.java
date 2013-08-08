/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
 * http://www.jppf.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
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
import org.jppf.utils.JPPFUuid;
import org.slf4j.*;

/**
 * Implementation of ValueChangeListener for script-based event listeners.
 * @author Laurent Cohen
 */
public class ScriptedValueChangeListener implements ValueChangeListener
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptedValueChangeListener.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
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
   * The unique id of this scripted listener.
   */
  private String uuid = new JPPFUuid().toString();
  /**
   * Contains the text of the script, after its first execution.
   */
  private String scriptText = null;

  /**
   * Initialize this listener with a specified script language and script source.
   * @param language the name of the scripting language to use.
   * @param content the actual source of the script to execute.
   */
  public ScriptedValueChangeListener(final String language, final String content)
  {
    this.language = language;
    this.script = content;
    runner = ScriptRunnerFactory.makeScriptRunner(this.language);
  }

  /**
   * Method called when the value of an option has changed.
   * This method actually executes the script that is specified in the XML document
   * from which the UI component was built.
   * @param event the event encapsulating the source of the event.
   * @see org.jppf.ui.options.event.ValueChangeListener#valueChanged(org.jppf.ui.options.event.ValueChangeEvent)
   */
  @Override
  public void valueChanged(final ValueChangeEvent event)
  {
    OptionElement option = event.getOption();
    if (scriptText == null)
    {
      TreePath path = option.getPath();
      StringBuilder sb = new StringBuilder();
      // add the scripts defined in the option and all its ancestors to the one in the listener.
      // only scripts written in the same scripting language are added.
      for (Object o: path.getPath())
      {
        OptionElement elt = (OptionElement) o;
        for (ScriptDescriptor desc: elt.getScripts())
        {
          if (language.equals(desc.language)) sb.append(desc.content).append('\n');
        }
      }
      sb.append(script);
      scriptText = sb.toString();
    }
    Map<String, Object> variables = new HashMap<>();
    variables.put("root", option.getRoot());
    variables.put("option", option);
    try
    {
      long start = System.currentTimeMillis();
      runner.evaluate(uuid, scriptText, variables);
      long elapsed = System.currentTimeMillis() - start;
      StringBuilder sb = new StringBuilder("executed ").append(language).append(" script in ").append(elapsed).append(" ms for [").append(option).append(']');
      if (debugEnabled) log.debug(sb.toString());
      //System.out.println(sb.toString());
    }
    catch(JPPFScriptingException e)
    {
      //e.printStackTrace();
      log.error("Error while executing script for " + option + "\nScript = \n" + scriptText, e);
    }
  }
}
