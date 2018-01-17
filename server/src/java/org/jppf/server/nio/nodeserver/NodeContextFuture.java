/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

import java.util.concurrent.FutureTask;

import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Future associated with a context which handles the job cancellation.
 */
class NodeContextFuture extends FutureTask<Object> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(NodeContextFuture.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node context.
   */
  private final AbstractNodeContext context; 
  /**
   * Dummy runnable used for bundle execution.
   */
  private static final Runnable NOOP_RUNNABLE = new Runnable() {
    @Override
    public void run() {
    }
  };

  /**
   * Initialize with the specified runnable and result object.
   * @param context the node context.
   */
  public NodeContextFuture(final AbstractNodeContext context) {
    super(NOOP_RUNNABLE, null);
    this.context = context;
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("cancelling " + context + ", isCancelled()=" + isCancelled());
    if (isDone()) return false;
    if (isCancelled()) return true;
    if (context.bundle == null) return false;
    try {
      context.bundle.cancel();
      context.cancelJob(context.bundle.getClientJob().getUuid(), false);
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    } finally {
      return super.cancel(false);
    }
  }
}
