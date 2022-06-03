/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package org.jppf.utils.configuration;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.jppf.location.*;
import org.jppf.scripting.*;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Handles the properties whose value contains one or more expressions of the form:
 * <pre>my.property = $script:language:source_type{script_source}$</pre>
 * Where:<br>
 * <ul>
 *   <li><i>language</i> is the script language to use, such as provided by the javax.script APIs. It defaults to "javascript"</li>
 *   <li><i>source_type</i> determines how to find the script, with possible values "inline", "file" or "url". It defaults to "inline"</li>
 *   <li><i>script_source</i> is either the script expression, or its location, depending on the value of source_type:
 *   <ul>
 *     <li>if source_type = inline, then script_source is the script itself.<br>
 *     for example: <code>my.prop = $script:javascript:inline{"hello" + "world"}$</code></li>
 *     <li>if source_type = file, then script_source is a script file, looked up first in the file system, then in the classpath.<br>
 *     For example: <code>my.prop = $script:javascript:file{/home/me/myscript.js}$</code></li>
 *     <li>if source_type = url, then script_source is a script loaded from a URL,<br>
 *     for example: <code>my.prop = $script:javascript:url{file:///home/me/myscript.js}$</code></li>
 *   </ul>
 *   </li>
 * </ul>
 * 
 * <p>If the language or source type are left unspecified, they will be assigned their default value.
 * For instance the following patterns will all resolve in language = 'javascript' and source_type = 'inline' :<br>
 * <ul>
 *   <li><code>$script{ 2 + 3 }$</code></li>
 *   <li><code>$script:{ 2 + 3 }$</code></li>
 *   <li><code>$script::{ 2 + 3 }$</code></li>
 *   <li><code>$script::inline{ 2 + 3 }$</code></li>
 *   <li><code>$script:javascript{ 2 + 3 }$</code></li>
 *   <li><code>$script:javascript:{ 2 + 3 }$</code></li>
 * </ul>
 *
 * <p>The default script language can be specified with the property 'jppf.script.default.language'.
 * This property is always evaluated first, in case it is also expressed with script expressions (which can only be in javascript).
 * If it is not specified explicitely, it will default to 'javascript'.
 * For example, in the following:
 * <pre>
 * # using javascript expression
 * jppf.script.default.language = $script{ 'groo' + 'vy' }
 * # inline expression using default language 'groovy'
 * my.property = $script{ return 2 + 3 }</pre>
 * The value of 'my.property' will evaluate to 5, resulting from the inline groovy expression 'return 2 + 3'.
 * 
 * <p>The scripts are evaluated after all includes and variable substitutions have been resolved.
 * This will allow the scripts to use a variable binding for the Properties (or {@link TypedProperties} object) being loaded.
 * @author Laurent Cohen
 * @exclude
 */
public class ScriptHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ScriptHandler.class);
  /**
   * The regex pattern for identifying scripted property values.
   */
  public static final Pattern SCRIPT_PATTERN = Pattern.compile("\\$(?:script|S|s)(?:\\:([^:]*?))?(?:\\:(.*?))?\\{(.*?)\\}\\$");
  /**
   * Name of the property which sets the default script language to use.
   */
  private static final String DEFAULT_SOURCE_TYPE = "inline";
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The properties to evaluate.
   */
  private Properties config;
  /**
   * The default script language.
   */
  private String defaultLanguage = "javascript";

  /**
   * Default constructor.
   */
  public ScriptHandler() {
  }

  /**
   * Evaluate all scripted expressions in all values of the specified properties object
   * and replace the values with the results of the evaluations.
   * @param props the properties object to evaluate.
   */
  public void process(final Properties props) {
    this.config = props;
    final Map<String, Object> bindings = new HashMap<>();
    bindings.put("thisProperties", config);
    final boolean hasDefaultScriptLanguage = config.containsKey(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName());
    String value = config.getProperty(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName(), JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getDefaultValue());
    value = evaluate(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName(), value, bindings).trim();
    if ("".equals(value)) value = "javascript";
    if (hasDefaultScriptLanguage) props.setProperty(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName(), value);
    defaultLanguage = value;

    for (String name: config.stringPropertyNames()) {
      if (JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName().equals(name)) continue;
      value = config.getProperty(name);
      config.setProperty(name, evaluate(name, value, bindings));
    }
    bindings.clear();
  }

  /**
   * Evaluate the value of the specified property by replacing each script expression with its result.
   * @param name the name of the property to evaluate.
   * @param value the value of the property to evaluate.
   * @param bindings variable bindings that can be used in the scripts.
   * @return a string where all script expressions are replaced with their value.
   */
  public String evaluate(final String name, final String value, final Map<String, Object> bindings) {
    if (value == null) return null;
    final Matcher matcher = SCRIPT_PATTERN.matcher(value);
    final StringBuilder sb = new StringBuilder();
    int pos = 0;
    int matches = 0;
    while (matcher.find()) {
      matches++;
      if (matcher.start() > pos) sb.append(value.substring(pos, matcher.start()));
      final String matched = value.substring(matcher.start(), matcher.end());
      String result = null;
      String language = matcher.group(1);
      if (language != null) language = language.trim();
      if ((language == null) || "".equals(language)) language = defaultLanguage;
      String type = matcher.group(2);
      if (type != null) type = type.trim().toLowerCase();
      if ((type == null) || "".equals(type)) type = DEFAULT_SOURCE_TYPE;
      String source = matcher.group(3);
      if (source != null) source = source.trim();
      if ((source == null) || "".equals(source)) result = matched;
      else {
        final String script = loadScript(name, language, type, source);
        if (script == null) result = matched;
        else {
          try {
            final Object res = new ScriptDefinition(language, script, bindings).evaluate();
            result = (res == null) ? null : res.toString();
          } catch(final Exception e) {
            final String message = "property '{}' : error evaluating a '{}' script from source type '{}', script is {}, exception is: {}";
            if (debugEnabled) log.warn(message, new Object[] {name, language, type, script, ExceptionUtils.getStackTrace(e)});
            else log.warn(message, new Object[] {name, language, type, script, ExceptionUtils.getMessage(e)});
            result = matched;
          }
        }
      }
      sb.append(result);
      pos = matcher.end();
    }
    if (matches > 0) {
      if (pos < value.length()) sb.append(value.substring(pos));
      return sb.toString();
    }
    return value;
  }

  /**
   * Load the script defined by the specified parameters.
   * @param name the name of the property for which the script is to be loaded.
   * @param language the script language.
   * @param type the type of script source.
   * @param source the script source.
   * @return the text of the script, or {@code null} if th script could not be loaded.
   */
  private static String loadScript(final String name, final String language, final String type, final String source) {
    String script = null;
    final char c = type.charAt(0);
    switch(c) {
      case 'i':
      case 'I':
        script = source;
        break;

      case 'f':
      case 'F':
        try {
          script = FileUtils.readTextFile(source);
        } catch(final Exception e) {
          log.warn("property '{}' : a '{}' script could not be read from the file '{}', exception is: {}", new Object[] {name, language, source, ExceptionUtils.getMessage(e)});
        }
        break;

      case 'u':
      case 'U':
        try {
          final Location<?> location = new URLLocation(source);
          final Reader reader = new InputStreamReader(location.getInputStream());
          script = FileUtils.readTextFile(reader);
        } catch (final Exception e) {
          log.warn("property '{}' : a '{}' script could not be read from the url '{}', exception is: {}", new Object[] {name, language, source, ExceptionUtils.getMessage(e)});
        }
        break;

      default:
        log.warn("property '{}' : a '{}' script could not be read from unkown source type '{}'", new Object[] {name, language, type});
        break;
    }
    return script;
  }
}
