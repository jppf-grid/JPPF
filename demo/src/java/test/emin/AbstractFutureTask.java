/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.emin;

import java.util.concurrent.*;

import org.jppf.node.protocol.AbstractTask;

/**
 * @param <T> .
 */
public abstract class AbstractFutureTask<T> extends AbstractTask<T> implements Future<T> {
  /** */
  private boolean cancelled = false;
  /** */
  private boolean done = false;
  /** */
  private boolean interruptible = false;

  @Override
  public final void run() {
    try {
      execute();
    } finally {
      synchronized(this) {
        done = true;
      }
    }
  }

  /**
   *
   */
  public abstract void execute();

  @Override
  public synchronized boolean cancel(final boolean mayInterruptIfRunning) {
    if (!cancelled && !done) {
      cancelled = true;
      done = true;
      if (mayInterruptIfRunning && interruptible) Thread.currentThread().interrupt();
      return true;
    }
    return false;
  }

  @Override
  public synchronized boolean isCancelled() {
    return cancelled;
  }

  @Override
  public synchronized boolean isDone() {
    return done;
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return getResult();
  }

  @Override
  public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return getResult();
  }

  /**
   * @return .
   */
  public boolean isInterruptible() {
    return interruptible;
  }

  /**
   * @param interruptible .
   */
  public void setInterruptible(final boolean interruptible) {
    this.interruptible = interruptible;
  }
}
