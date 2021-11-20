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

package org.jppf.server.nio.client;

import java.util.List;

import org.jppf.nio.StatelessNioServer;
import org.jppf.server.protocol.*;
import org.slf4j.*;

/**
 * Completion listener that is used to notify that results were received from a node,
 * and they should be sent back to the client.
 * @author Laurent Cohen
 */
public class CompletionListener implements ServerTaskBundleClient.CompletionListener {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(CompletionListener.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The client channel.
   */
  private final AsyncClientContext context;

  /**
   * Initialize this completion listener with the specified channel.
   * @param context the client channel.
   */
  public CompletionListener(final AsyncClientContext context) {
    if (context == null) throw new IllegalArgumentException("channel is null");
    this.context = context;
  }

  @Override
  public void taskCompleted(final ServerTaskBundleClient bundle, final List<ServerTask> results) {
    if (bundle == null) throw new IllegalStateException("bundler is null");
    if (!isChannelValid()) {
      //if (debugEnabled) log.debug("channel is invalid: {}", context);
      log.warn("attempt to send bundle to invalid channel {}\nbundle = {}", context, bundle);
      try {
        context.server.getMessageHandler().jobResultsSent(context, bundle);
      } catch (final Exception e) {
        log.error("Error sending job results for {}:\n", bundle, e);
      }
      return;
    }
    if (results.isEmpty()) {
      if (debugEnabled) log.debug("empty results list for bundle {}", bundle);
      return;
    }
    if (debugEnabled) log.debug("*** returning " + results.size() + " results for client bundle " + bundle + "(cancelled=" + bundle.isCancelled() + ')');
    if (bundle.isCancelled()) bundle.removeCompletionListener(this);
    else {
      try {
        context.server.getMessageHandler().sendJobResults(context, bundle);
      } catch (final Exception e) {
        log.error("Error sending job results for {}:\n", bundle, e);
      }
    }
  }

  @Override
  public void bundleEnded(final ServerTaskBundleClient bundle) {
    if (debugEnabled) log.debug("bundle ended: {}", bundle);
  }

  /**
   * Determine whether the channel is valid at the time this method is called.
   * @return {@code true} if the channel is valid, {@code false} otherwise.
   */
  private boolean isChannelValid() {
    return StatelessNioServer.isKeyValid(context.getSelectionKey());
  }
}
