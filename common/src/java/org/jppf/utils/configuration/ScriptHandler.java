/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * The regex pattern for identifying scripted property values.
   */
  private static final Pattern SCRIPT_PATTERN = Pattern.compile("\\$script(?:\\:([^:]*?))?(?:\\:(.*?))?\\{(.*?)\\}\\$");
  /**
   * Inline script source type.
   */
  private static final String INLINE = "inline";
  /**
   * File script source type.
   */
  private static final String FILE = "file";
  /**
   * URL script source type.
   */
  private static final String URL = "url";
  /**
   * Name of the property which sets the default script language to use.
   */
  private static final String DEFAULT_SOURCE_TYPE = INLINE;
  /**
   * The properties to evaluate.
   */
  private TypedProperties config = null;
  /**
   * The default script language.
   */
  private String defaultLanguage = "javascript";
  /**
   * The bindings provide variables available to the script engine during execution of the script.
   */
  private final Map<String, Object> bindings = new HashMap<>();

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
  public void process(final TypedProperties props) {
    this.config = props;
    bindings.put("thisProperties", config);

    boolean hasDefaultScriptLanguage = config.containsKey(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName());
    String value = config.get(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE);
    value = evaluate(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName(), value).trim();
    if ("".equals(value)) value = "javascript";
    if (hasDefaultScriptLanguage) props.set(JPPFProperties.SCRIPT_DEFAULT_LANGUAGE, value);
    defaultLanguage = value;

    for (String name: config.stringPropertyNames()) {
      if (JPPFProperties.SCRIPT_DEFAULT_LANGUAGE.getName().equals(name)) continue;
      value = config.getString(name);
      config.setString(name, evaluate(name, value));
    }
    bindings.clear();
  }

  /**
   * Evaluate the value of the specified property by replacing each script expression with its result.
   * @param name the name of the property to evaluate.
   * @param value the value of the property to evaluate.
   * @return a string where all script expressions are replaced with their value.
   */
  private String evaluate(final String name, final String value) {
    if (value == null) return null;
    Matcher matcher = SCRIPT_PATTERN.matcher(value);
    StringBuilder sb = new StringBuilder();
    int pos = 0;
    int matches = 0;
    while (matcher.find()) {
      matches++;
      if (matcher.start() > pos) sb.append(value.substring(pos, matcher.start()));
      String matched = value.substring(matcher.start(), matcher.end());
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
        String script = loadScript(name, language, type, source);
        if (script == null) result = matched;
        else {
          ScriptRunner runner = ScriptRunnerFactory.getScriptRunner(language);
          if (runner == null) {
            log.warn("property '{}' : could not obtain a '{}' script engine", name, language);
            result = matched;
          } else {
            try {
              Object res = runner.evaluate(script, bindings);
              result = res == null ? null : res.toString();
            } catch(Exception e) {
              String message = "property '{}' : error evaluating a '{}' script from source type '{}', script is {}, exception is: {}";
              if (debugEnabled) log.warn(message, new Object[] {name, language, type, script, ExceptionUtils.getStackTrace(e)});
              else log.warn(message, new Object[] {name, language, type, ExceptionUtils.getMessage(e)});
              result = matched;
            } finally {
              ScriptRunnerFactory.releaseScriptRunner(runner);
            }
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
  private String loadScript(final String name, final String language, final String type, final String source) {
    String script = null;
    switch(type) {
      case INLINE:
        script = source;
        break;
      case FILE:
        try {
          script = FileUtils.readTextFile(source);
        } catch(Exception e) {
          log.warn("property '{}' : a '{}' script could not be read from the file '{}', exception is: {}", new Object[] {name, language, source, ExceptionUtils.getMessage(e)});
        }
        break;
      case URL:
        try {
          Location<?> location = new URLLocation(source);
          Reader reader = new InputStreamReader(location.getInputStream());
          script = FileUtils.readTextFile(reader);
        } catch (Exception e) {
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
