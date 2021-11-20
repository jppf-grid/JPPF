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

package org.jppf.node.policy;

import java.io.*;
import java.net.URL;

import org.jppf.JPPFException;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Utility methods to resolve, parse and manipulate execution policies.
 * @author Laurent Cohen
 * @exclude
 */
public class PolicyUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(PolicyUtils.class);

  /**
   * Create the execution policy form the defintiion.
   * @param props the datasource properties.
   * @param propertyName the identifier of the datasource in the configuration.
   * @return an {@link ExecutionPolicy} instance, or {@code null} if none could be created.
   */
  public static  String resolvePolicy(final TypedProperties props, final String propertyName) {
    String policy = null;
    try {
      final String s = props.getString(propertyName);
      if (s == null) return null;
      final String[] tokens = s.split("\\|");
      for (int i=0; i<tokens.length; i++) tokens[i] = tokens[i].trim();
      final String type;
      final String source;
      if (tokens.length >= 2) {
        type = tokens[0].toLowerCase();
        source = tokens[1];
      } else {
        type = "inline";
        source = tokens[0];
      }
      try (final Reader reader = getPolicyReader(type, source)) {
        policy = FileUtils.readTextFile(reader);
      }
    } catch (final Exception e) {
      log.error("error resolving the execution policy for property name = {}\n{}", propertyName, ExceptionUtils.getStackTrace(e));
    }
    return policy;
  }

  /**
   * Get a reader for the execution policy based on the type and source.
   * @param type the type of source: one of "inline", "file", "url".
   * @param source the source for the policy, its smeaning depends on the type.
   * @return an execution policy parsed from the source.
   * @throws Exception if any error occurs.
   */
  private static Reader getPolicyReader(final String type, final String source) throws Exception {
    Reader reader = null;
    switch(type) {
      case "inline":
        reader = new StringReader(source);
        break;

      case "file":
        reader = FileUtils.getFileReader(source);
        break;

      case "url":
        final URL url = new URL(source);
        reader = new InputStreamReader(url.openConnection().getInputStream(), "utf-8");
        break;

      default:
        throw new JPPFException("unknown soure type '" + type + "' for execution policy '" + source + "'");
    }
    return reader;
  }
}
