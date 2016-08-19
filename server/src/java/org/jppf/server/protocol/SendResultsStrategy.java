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

package org.jppf.server.protocol;

import java.util.Collection;

import org.jppf.node.protocol.SendResultsStrategyConstants;

/**
 * Strategy to determine whether results should be sent immediately.
 * @author Laurent Cohen
 */
public interface SendResultsStrategy {
  /**
   * Determine whether results should be sent immediately.
   * @param bundle the client bundle for which results were recived from a node.
   * @param tasks the task whose results are to be returned.
   * @return <code>true</code> if the results should be sent immediately, <code>false</code> otherwise.
   * @exclude
   */
  boolean sendResults(final ServerTaskBundleClient bundle, final Collection<ServerTask> tasks);

  /**
   * Get the name of this strategy.
   * @return the name as a string.
   * @exclude
   */
  String getName();

  /**
   * Strategy that sends the results everytime time they are received from a node.
   * @exclude
   */
  public static class SendNodeResultsStrategy implements SendResultsStrategy {
    @Override
    public boolean sendResults(final ServerTaskBundleClient bundle, final Collection<ServerTask> tasks) {
      return true;
    }

    @Override
    public String getName() {
      return SendResultsStrategyConstants.NODE_RESULTS;
    }
  }

  /**
   * Strategy that sends the results when they have all been received for the client bundle.
   * @exclude
   */
  public static class SendAllResultsStrategy implements SendResultsStrategy {
    @Override
    public boolean sendResults(final ServerTaskBundleClient bundle, final Collection<ServerTask> tasks) {
      return false;
    }

    @Override
    public String getName() {
      return SendResultsStrategyConstants.ALL_RESULTS;
    }
  }
}
