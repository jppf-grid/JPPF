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
import java.net.URL;
import java.util.Properties;
import java.util.regex.Pattern;

import org.jppf.location.URLLocation;
import org.jppf.utils.*;
import org.jppf.utils.streams.StreamUtils;
import org.slf4j.*;

/**
 * This class loads and resolves the includes in a configuration file or source.
 * @author Laurent Cohen
 * @exclude
 */
public class PropertiesLoader {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PropertiesLoader.class);
  /**
   * White space pattern.
   */
  private static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");
  /**
   * Include pattern: a line that starts with '#!include' followed by white space, case-insensitive.
   */
  private static final Pattern INCLUDE_PATTERN = Pattern.compile("^#\\!include\\s+.*$");
  /**
   * The string which indicates that an include must be read from a specified
   * {@link JPPFConfiguration.ConfigurationSource} or {@link JPPFConfiguration.ConfigurationSourceReader}.
   */
  private static final String CLASS_SRC = "class";
  /**
   * The string which indicates an include must be read from a file.
   */
  private static final String FILE_SRC = "file";
  /**
   * The string which indicates an include must be read from a URL.
   */
  private static final String URL_SRC = "url";
  /**
   * Empty string constant.
   */
  private static final String EMPTY_STRING = "";
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);

  /**
   * Loads the properties from the specified reader into the specified Properties object.
   * @param props the properties object into which the rpoerties are kept.
   * @param reader the reader to get the properties from.
   * @throws IOException if any error occurs.
   */
  public void load(final Properties props, final Reader reader) throws IOException {
    final BufferedReader breader = (reader instanceof BufferedReader) ? (BufferedReader) reader : new BufferedReader(reader);
    String list = EMPTY_STRING;
    try {
      list = load(breader);
      try (Reader r = new StringReader(list)) {
        props.load(r);
      }
    } catch (final StackOverflowError e) {
      final String msg = "There is a problem in the configuration: it has cyclic include statements leading to " + ExceptionUtils.getMessage(e);
      System.err.println(msg);
      if (debugEnabled) log.debug(msg, e);
      else log.error(msg);
      props.put("jppf.configuration.error", msg);
    }
  }

  /**
   * Loads the properties from the specified reader into the specified Properties object.
   * @param reader the reader to get the properties from.
   * @return a list of strings representing a line in a properties file.
   * @throws IOException if any error occurs.
   */
  private String load(final BufferedReader reader) throws IOException {
    final StringBuilder sb = new StringBuilder();
    String line = null;
    while ((line = reader.readLine()) != null) {
      line = line.trim();
      if (line.length() <= 0) continue;
      if (INCLUDE_PATTERN.matcher(line).matches()) sb.append(readInclude(line));
      else if ((line.charAt(0) == '#') || (line.charAt(0) == '!')) continue;
      else sb.append(line).append('\n');
    }
    return sb.toString();
  }

  /**
   * Parse and process an include statement read from a properties source.
   * @param include the line containng the include.
   * @return a String with the content of the source specified by the include.
   */
  private String readInclude(final String include) {
    final String[] tokens = WHITE_SPACE_PATTERN.split(include, 3);
    if ((tokens == null) || (tokens.length < 3)) {
      log.warn("could not process include '{}' : not enough arguments", include);
      return EMPTY_STRING;
    }
    String result = null;
    BufferedReader reader = null;
    try {
      switch(tokens[1].toLowerCase()) {
        case FILE_SRC:
          reader = new BufferedReader(FileUtils.getFileReader(tokens[2]));
          break;
        case URL_SRC:
          final URL url = new URL(tokens[2]);
          reader = new BufferedReader(new InputStreamReader(new URLLocation(url).getInputStream()));
          break;
        case CLASS_SRC:
          reader = new BufferedReader(JPPFConfiguration.getConfigurationSourceReader(tokens[2]));
          break;
        default:
          log.warn("invalid source type '{}' specified in include statement '{}'", tokens[1], include);
          return EMPTY_STRING;
      }
      result = load(reader);
    } catch(final Exception e) {
      log.warn("Could not read [{}] '{}' specified in include statement '{}', reason: {}", new Object[] {tokens[1], tokens[2], include, ExceptionUtils.getMessage(e)});
      return EMPTY_STRING;
    } finally {
      if (reader != null) StreamUtils.closeSilent(reader);
    }
    return result == null ? EMPTY_STRING : result;
  }
}
