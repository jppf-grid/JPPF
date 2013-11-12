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

package org.jppf.server.nio.nodeserver;

import org.jppf.execute.JPPFFutureTask;
import org.jppf.utils.ExceptionUtils;

/**
 * Fuiture associated with a context which handles the job cancellation.
 */
class NodeContextFuture extends JPPFFutureTask<Object> {
  /**
   * The node context.
   */
  private final AbstractNodeContext context; 
  /**
   * Initialize witht he specified runnable and result object.
   * @param context the node context.
   */
  public NodeContextFuture(final AbstractNodeContext context) {
    super(AbstractNodeContext.NOOP_RUNNABLE, null);
    this.context = context;
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (AbstractNodeContext.debugEnabled) AbstractNodeContext.log.debug("cancelling " + context + ", isCancelled()=" + isCancelled());
    if (isDone()) return false;
    if (isCancelled()) return true;
    if (context.bundle == null) return false;
    try {
      context.bundle.cancel();
      context.cancelJob(context.bundle.getClientJob().getUuid(), false);
    } catch (Exception e) {
      if (AbstractNodeContext.debugEnabled) AbstractNodeContext.log.debug(e.getMessage(), e);
      else AbstractNodeContext.log.warn(ExceptionUtils.getMessage(e));
    } finally {
      return super.cancel(false);
    }
  }
}