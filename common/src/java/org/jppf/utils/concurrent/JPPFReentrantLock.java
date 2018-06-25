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

package org.jppf.utils.concurrent;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.*;

import org.jppf.utils.SystemUtils;

/**
 * A non-fair lock implementation that allows assigning a readable and identifiable name and {@code toString()}.
 * @author Laurent Cohen
 */
public class JPPFReentrantLock implements Lock, Serializable {
  /**
   * Explicit serial version UID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Name given to this lock.
   */
  private final String name;
  /**
   * Synchronizer providing all implementation mechanics
   */
  private final Synchronizer synchronizer;

  /**
   * Creates an instance of {@code JPPFReentrantLock}.
   */
  public JPPFReentrantLock() {
    this(null);
  }

  /**
   * Creates an instance of {@code JPPFReentrantLock}.
   * @param name the name.
   */
  public JPPFReentrantLock(final String name) {
    synchronizer = new Synchronizer(name);
    final String s = SystemUtils.getSystemIdentity(this);
    this.name = (name == null) ? s : s + " [" + name + "]";
  }

  @Override
  public void lock() {
    synchronizer.acquire(1);
  }

  @Override
  public void lockInterruptibly() throws InterruptedException {
    synchronizer.acquireInterruptibly(1);
  }

  @Override
  public boolean tryLock() {
    return synchronizer.tryAcquire(1);
  }

  @Override
  public boolean tryLock(final long timeout, final TimeUnit unit) throws InterruptedException {
    return synchronizer.tryAcquireNanos(1, unit.toNanos(timeout));
  }

  @Override
  public void unlock() {
    synchronizer.release(1);
  }

  @Override
  public Condition newCondition() {
    return synchronizer.newCondition();
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * Object to which locking and synchronization operations are delegated.
   */
  static class Synchronizer extends AbstractQueuedSynchronizer {
    /**
     * Name given to this synchronizer.
     */
    final String name;

    /**
     * Creates an instance of {@code JPPFReentrantLock}.
     * @param name the name.
     */
    Synchronizer(final String name) {
      final String s = SystemUtils.getSystemIdentity(this);
      this.name = (name == null) ? s : s + " [" + name + "]";
    }

    @Override
    public boolean tryAcquire(final int arg) {
      final Thread current = Thread.currentThread();
      final int currentState = getState();
      if (currentState == 0) {
        if (compareAndSetState(0, 1)) {
          setExclusiveOwnerThread(current);
          return true;
        }
      } else if (getExclusiveOwnerThread() == current) {
        setState(currentState + 1);
        return true;
      }
      return false;
    }

    @Override
    public boolean tryRelease(final int arg) {
      if (Thread.currentThread() != getExclusiveOwnerThread()) throw new IllegalMonitorStateException("thread " + Thread.currentThread() + " does not own this lock");
      final int newState = getState() - 1;
      if (newState == 0) {
        setExclusiveOwnerThread(null);
        setState(0);
        return true;
      }
      setState(newState);
      return false;
    }

    @Override
    protected boolean isHeldExclusively() {
      return getExclusiveOwnerThread() == Thread.currentThread();
    }

    /**
     * @return a {@link Condition} instance.
     */
    Condition newCondition() {
      return new ConditionObject();
    }

    @Override
    public String toString() {
      return name;
    }
  }
}
