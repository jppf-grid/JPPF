/*
 * JPPF.
 * Copyright (C) 2005-2015 JPPF Team.
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

import java.awt.event.*;
import java.util.*;

import javax.swing.tree.TreePath;

import org.jppf.scripting.*;
import org.jppf.ui.options.*;
import org.jppf.ui.options.xml.OptionDescriptor.ScriptDescriptor;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * A mouse listener which executes a script instead of java code when a {@code mouseClicked()} event occurs.
 * @author Laurent Cohen
 */
public class ScriptedMouseListener implements MouseListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptedMouseListener.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Name of the scripting language to use.
   */
  private final String language;
  /**
   * The actual source of the script to execute.
   */
  private final String script;
  /**
   * The unique id of this scripted listener.
   */
  private final String uuid = new JPPFUuid().toString();
  /**
   * Contains the text of the script, after its first execution.
   */
  private String scriptText;
  /**
   * 
   */
  private final AbstractOption option;

  /**
   * Initialize this listener with a specified script language and script source.
   * @param option the ui element on which the listener is set.
   * @param language the name of the scripting language to use.
   * @param content the actual source of the script to execute.
   */
  public ScriptedMouseListener(final AbstractOption option, final String language, final String content) {
    this.language = language;
    this.script = content;
    this.option = option;
  }

  /**
   * Method called when a mouse event occurs.
   * This method actually executes the script that is specified in the XML document
   * from which the UI component was built.
   * @param event the event encapsulating the source of the event.
   * @param eventType the type of mouse event, "clicked", "pressed", "released", "entered" or "exited".
   */
  private void invokeScript(final MouseEvent event, final String eventType) {
    if (scriptText == null) {
      TreePath path = option.getPath();
      StringBuilder sb = new StringBuilder();
      for (Object o : path.getPath()) {
        OptionElement elt = (OptionElement) o;
        for (ScriptDescriptor desc : elt.getScripts()) {
          if (language.equals(desc.language)) sb.append(desc.content).append('\n');
        }
      }
      sb.append(script);
      scriptText = sb.toString();
    }
    Map<String, Object> variables = new HashMap<>();
    variables.put("root", option.getRoot());
    variables.put("option", option);
    variables.put("event", event);
    variables.put("eventType", eventType);
    ScriptRunner runner = null;
    try {
      runner = ScriptRunnerFactory.getScriptRunner(this.language);
      long start = System.currentTimeMillis();
      runner.evaluate(uuid, scriptText, variables);
      long elapsed = System.currentTimeMillis() - start;
      StringBuilder sb = new StringBuilder("executed ").append(language).append(" script in ").append(elapsed).append(" ms for [").append(option).append(']');
      if (debugEnabled) log.debug(sb.toString());
    } catch (JPPFScriptingException e) {
      log.error("Error while executing script for " + option + "\nScript = \n" + scriptText, e);
    } finally {
      ScriptRunnerFactory.releaseScriptRunner(runner);
    }
  }

  @Override
  public void mouseClicked(final MouseEvent event) {
    invokeScript(event, "clicked");
  }

  @Override
  public void mousePressed(final MouseEvent event) {
    //invokeScript(event, "pressed");
  }

  @Override
  public void mouseReleased(final MouseEvent event) {
    //invokeScript(event, "released");
  }

  @Override
  public void mouseEntered(final MouseEvent event) {
    invokeScript(event, "entered");
  }

  @Override
  public void mouseExited(final MouseEvent event) {
    invokeScript(event, "exited");
  }
}
