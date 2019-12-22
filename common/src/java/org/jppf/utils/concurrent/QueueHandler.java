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
 * It may, upon request, spawn a dequeuing thread that will process elements according to a processing function supplied by the consumer.
 * @param <E> the type of the elements in the queue.
 * @author Laurent Cohen
 */
public class QueueHandler<E> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(QueueHandler.class);
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
  private final Handler<E> handler;
  /**
   * The thread that reads job notifications fromm the queue and sends them.
   */
  private Thread[] dequeuerThreads;
  /**
   * The name assigned tot he dequeuer thread.
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
   * Whether thie dequeuing thread was started.
   */
  private final AtomicBoolean started = new AtomicBoolean(false);
  /**
   * Whether this queue handler is closed.
   */
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Initialize this queue handler.
   */
  public QueueHandler() {
    this(null, Integer.MAX_VALUE, null);
  }

  /**
   * Initialize this queue handler.
   * @param name the name of the dequeuer thread.
   */
  public QueueHandler(final String name) {
    this(name, Integer.MAX_VALUE, null);
  }

  /**
   * Initialize this queue handler.
   * @param name the name of the dequeuer thread.
   * @param handler the hanler for dequeued elements.
   */
  public QueueHandler(final String name, final Handler<E> handler) {
    this(name, Integer.MAX_VALUE, handler);
  }

  /**
   * Initialize this queue handler.
   * @param name the name of the dequeuer thread.
   * @param capacity the capacity of the queue.
   */
  public QueueHandler(final String name, final int capacity) {
    this(name, capacity, null);
  }

  /**
   * Initialize this queue handler.
   * @param name the name of the dequeuer thread.
   * @param capacity the capacity of the queue.
   * @param handler the hanler for dequeued elements.
   */
  public QueueHandler(final String name, final int capacity, final Handler<E> handler) {
    this.name = (name == null) ? getClass().getSimpleName() + "-" + instanceCount.incrementAndGet() : name;
    queue = new LinkedBlockingDeque<>(capacity);
    this.handler = handler;
  }

  /**
   * Add the specified element to the <i>tail</i> of the queue.
   * @param element the element to add.
   * @return {@code true} if the element was successfully added, {@code false} otherwise.
   */
  public boolean offer(final E element) {
    final boolean success = queue.offer(element);
    checkQueueSize();
    return success;
  }

  /**
   * Add the specified element to the <i>head</i> of the queue.
   * @param element the element to add.
   * @return {@code true} if the element was successfully added, {@code false} otherwise.
   */
  public boolean offerToHead(final E element) {
    final boolean success = queue.offerFirst(element);
    checkQueueSize();
    return success;
  }

  /**
   * Add the specified element to the <i>tail</i> of the queue, waiting if necessary for available space in the queue.
   * @param element the element to add.
   * @throws Exception if any error occurs.
   */
  public void put(final E element) throws Exception {
    queue.put(element);
    checkQueueSize();
  }

  /**
   * Add the specified element to the <i>head</i> of the queue, waiting if necessary for available space in the queue.
   * @param element the element to add.
   * @throws Exception if any error occurs.
   */
  public void putToHead(final E element) throws Exception {
    queue.putFirst(element);
    checkQueueSize();
  }

  /**
   * Check the queue size and performs and update of the peak size if appropriate.
   */
  private void checkQueueSize() {
    final int n = queue.size();
    if (peakSize.compareAndSet(Operator.LESS_THAN, n) && (peakSizeUpdateCallback != null)) {
      peakSizeUpdateCallback.newPeakSize(n);
    }
  }

  /**
   * Get the head element from the queue.
   * @return the head element, or {@code null} if the queue is empty.
   */
  public E poll() {
    return queue.poll();
  }

  /**
   * Get the head element from the queue, waiting if necessary for an element to becomes available.
   * @return the head element.
   * @throws Exception if any error occurs.
   */
  public E take() throws Exception {
    return queue.take();
  }

  /**
   * Determine whether the queue is empty.
   * @return {@code true} if the queue is empty, {@code false} otherwise.
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Get the size of the queue.
   * @return the queue size as an {@code int}.
   */
  public int size() {
    return queue.size();
  }

  /**
   * Get the peak queue size.
   * @return the peak queue size as an int.
   */
  public int getPeakSize() {
    return this.peakSize.get();
  }

  /**
   * Set the peak queue size callback.
   * @param peakSizeUpdateCallback the callback to set.
   * @return this queue handler, for method call chaining.
   */
  public QueueHandler<E> setPeakSizeUpdateCallback(final PeakSizeUpdateCallback peakSizeUpdateCallback) {
    this.peakSizeUpdateCallback = peakSizeUpdateCallback;
    return this;
  }

  /**
   * Start the dequeuer thread. It the thread was already started, this method has no effect.
   * @return this queue handler, for method call chaining.
   */
  public QueueHandler<E> startDequeuer() {
    return startDequeuer(1);
  }

  /**
   * Start the dequeuer thread. It the thread was already started, this method has no effect.
   * @param nbThreads the number of threads to start.
   * @return this queue handler, for method call chaining.
   */
  public QueueHandler<E> startDequeuer(final int nbThreads) {
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
   * CLose this queue handler.
   */
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

  /**
   * An operation applied to each element taken from the queue by the dequeuer thread.
   * @param <E> the type of elements to handle.
   */
  @FunctionalInterface
  public interface Handler<E> {
    /**
     * Handle an element.
     * @param element the object to handle.
     * @throws Exception if any error occurs.
     */
    void handle(E element) throws Exception;
  }

  /**
   * Callback invoked when a new peak queue size is reached.
   */
  @FunctionalInterface
  public interface PeakSizeUpdateCallback {
    /**
     * Called when a new peak queue size is reached.
     * @param n the new peak size.
     */
    void newPeakSize(int n);
  }
}
