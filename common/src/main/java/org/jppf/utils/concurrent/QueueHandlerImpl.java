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

package org.jppf.utils.concurrent;

import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.jppf.utils.Operator;
import org.slf4j.*;

/**
 * Instances of this class handle an optionally bounded queue of elements.
 * <p>It may, upon request, spawn one or more dequeuing threads that will process elements according to a processing function supplied by the consumer.
 * <p>It also maintains basic statistics about the queue size, peak size, number of queued and rejected elements.
 * @param <E> the type of the elements in the queue.
 * @author Laurent Cohen
 */
class QueueHandlerImpl<E> implements QueueHandler<E> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(QueueHandlerImpl.class);
  /**
   * COunt of instanes of this class.
   */
  private static final AtomicLong instanceCount = new AtomicLong(0L);
  /**
   * The queue to handle.
   */
  private final BlockingDeque<E> queue;
  /**
   * The peak queue size, maintained by this queue handler.
   */
  private final SynchronizedInteger peakSize = new SynchronizedInteger(0);
  /**
   * AN optional operation to perform on the elements taken from the queue.
   */
  private final ElementHandler<E> handler;
  /**
   * The thread that reads job notifications fromm the queue and sends them.
   */
  private Thread[] dequeuerThreads;
  /**
   * The name assigned to the dequeuer thread.
   */
  private final String name;
  /**
   * The number of dequeing threads.
   */
  private int nbThreads;
  /**
   * An optional callback invoked when a new peak queue size is reached.
   */
  private PeakSizeUpdateCallback peakSizeUpdateCallback;
  /**
   * Count of the elements that were sucessfully queued.
   */
  private final AtomicLong queuedElements = new AtomicLong(0L);
  /**
   * Count of the elements that were rejected.
   */
  private final AtomicLong rejectedElements = new AtomicLong(0L);
  /**
   * Whether thie dequeuing thread was started.
   */
  private final AtomicBoolean started = new AtomicBoolean(false);
  /**
   * Whether this queue handler is closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Initialize this queue handler with an unbounded queue.
   * @param name the prefix for the names of the dequeuer threads.
   * @param handler the hanler for dequeued elements.
   */
  QueueHandlerImpl(final String name, final ElementHandler<E> handler) {
    this(name, Integer.MAX_VALUE, handler);
  }

  /**
   * Initialize this queue handler.
   * @param name the name of the dequeuer thread.
   * @param capacity the capacity of the queue.
   * @param handler the hanler for dequeued elements.
   */
  QueueHandlerImpl(final String name, final int capacity, final ElementHandler<E> handler) {
    this.name = (name == null) ? getClass().getSimpleName() + "-" + instanceCount.incrementAndGet() : name;
    queue = new LinkedBlockingDeque<>(capacity);
    this.handler = handler;
  }

  @Override
  public boolean offer(final E element) {
    final boolean success = queue.offer(element);
    checkQueueSize(success);
    return success;
  }

  @Override
  public boolean offerToHead(final E element) {
    final boolean success = queue.offerFirst(element);
    checkQueueSize(success);
    return success;
  }

  @Override
  public void put(final E element) throws Exception {
    queue.put(element);
    checkQueueSize(true);
  }

  @Override
  public void putToHead(final E element) throws Exception {
    queue.putFirst(element);
    checkQueueSize(true);
  }

  /**
   * 
   * @param success whether the element was successfully inserted into the queue.
   */
  private void checkQueueSize(final boolean success) {
    final int n = queue.size();
    if (peakSize.compareAndSet(Operator.LESS_THAN, n) && (peakSizeUpdateCallback != null)) {
      peakSizeUpdateCallback.newPeakSize(n);
    }
    if (success) queuedElements.incrementAndGet();
    else rejectedElements.incrementAndGet();
  }

  @Override
  public E poll() {
    return queue.poll();
  }

  @Override
  public E take() throws Exception {
    return queue.take();
  }

  @Override
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  @Override
  public int size() {
    return queue.size();
  }

  @Override
  public int getPeakSize() {
    return this.peakSize.get();
  }

  @Override
  public long getQueuedElements() {
    return queuedElements.get();
  }

  @Override
  public long getRejectedElements() {
    return rejectedElements.get();
  }

  @Override
  @SuppressWarnings("unchecked")
  public void close() {
    if (closed.compareAndSet(false, true)) {
      if (dequeuerThreads != null) {
        for (int i=0; i<nbThreads; i++) ((DequeuerThread) dequeuerThreads[i]).stopped.set(true);
      }
      queue.clear();
    }
  }

  /**
   * Set the peak queue size callback.
   * @param peakSizeUpdateCallback the callback to set.
   * @return this queue handler, for method call chaining.
   */
  QueueHandlerImpl<E> setPeakSizeUpdateCallback(final PeakSizeUpdateCallback peakSizeUpdateCallback) {
    this.peakSizeUpdateCallback = peakSizeUpdateCallback;
    return this;
  }

  /**
   * Start the specified number of dequeuer threads. It the dequeuing was already started, this method has no effect.
   * @param nbThreads the number of dequeuer threads to start.
   * @return this queue handler, for method call chaining.
   */
  QueueHandlerImpl<E> startDequeuer(final int nbThreads) {
    if (started.compareAndSet(false, true)) {
      this.nbThreads = nbThreads;
      dequeuerThreads = new Thread[nbThreads];
      for (int i=0; i<nbThreads; i++) {
        String threadName = name;
        if (nbThreads > 1) threadName += "-" + (i + 1);
        (dequeuerThreads[i] = new DequeuerThread(threadName)).start();
      }
    }
    return this;
  }

  /**
   * A thread that reads objects fromm the queue and sends them.
   */
  private final class DequeuerThread extends DebuggableThread {
    /**
     * Whether this thread should stop.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * COnstruct this thread.
     * @param name the name of this thread.
     */
    private DequeuerThread(final String name) {
      super(name);
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        while (!stopped.get()) {
          final E element = queue.take();
          if (handler != null) handler.handle(element);
        }
      } catch (final InterruptedException e) {
        if (!stopped.get()) log.error(e.getMessage(), e);
      } catch (final Exception e) {
        log.error(e.getMessage(), e);
      }
    }
  }
}
