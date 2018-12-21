/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

package org.jppf.server.nio.nodeserver.async;

import java.util.concurrent.FutureTask;

import org.jppf.server.protocol.ServerTaskBundleNode;
import org.jppf.utils.ExceptionUtils;
import org.slf4j.*;

/**
 * Future associated with a context which handles the job cancellation.
 */
public class AsyncNodeContextFuture extends FutureTask<Object> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AsyncNodeContextFuture.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The node context.
   */
  private final AsyncNodeContext context;
  /**
   * The node bundle to process.
   */
  private final ServerTaskBundleNode bundle;
  /**
   * Dummy runnable used for bundle execution.
   */
  private static final Runnable NOOP_RUNNABLE = () -> {};

  /**
   * Initialize witht he specified runnable and result object.
   * @param context the node context.
   * @param bundle the node bundle to process.
   */
  public AsyncNodeContextFuture(final AsyncNodeContext context, final ServerTaskBundleNode bundle) {
    super(NOOP_RUNNABLE, null);
    this.context = context;
    this.bundle = bundle;
  }

  @Override
  public boolean cancel(final boolean mayInterruptIfRunning) {
    if (debugEnabled) log.debug("cancelling {}, isCancelled()={}", context, isCancelled());
    if (isCancelled()) return true;
    if (isDone()) return false;
    if (bundle == null) return false;
    try {
      bundle.cancel();
      context.cancelJob(bundle.getClientJob().getUuid(), false);
    } catch (final Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      else log.warn(ExceptionUtils.getMessage(e));
    } finally {
      return super.cancel(false);
    }
  }
}
