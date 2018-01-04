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

package org.jppf.ui.utils;

import org.jppf.node.policy.PolicyParser;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * 
 * @author Laurent Cohen
 */
public class NodeFilterUtils {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeFilterUtils.class);
  /**
   * The default empty execution policy for node filtering from a resource file.
   */
  public static final String DEFAULT_EMPTY_FILTER = loadDefaultEmptyFilter();

  /**
   * Validate an execution policy specified as a String.
   * @param policy the policy to validate.
   * @return a pair holding a flag indicating whther th epolociy is valid and an eventual erorr message.
   */
  public static Pair<Boolean, String> validatePolicy(final String policy) {
    try {
      PolicyParser.validatePolicy(policy);
      return new Pair<>(true, null);
    } catch (final Exception e) {
      return new Pair<>(false, e.getMessage());
    }
  }

  /**
   * Load the default empty execution policy for node filtering from a resource file.
   * @return an XML execution policy as a string.
   */
  private static String loadDefaultEmptyFilter() {
    try {
      return FileUtils.readTextFile("org/jppf/ui/filtering/empty_policy.xml");
    } catch (final Exception e) {
      if (log.isDebugEnabled()) log.debug("Could not load default empty policy", e);
      return "<ExecutionPolicy>\n  \n</ExecutionPolicy>";
    }
  }
}
