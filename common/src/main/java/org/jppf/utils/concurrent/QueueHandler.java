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

/**
 * Instances of this class handle an optionally bounded queue of elements.
 * <p>It may, upon request, spawn one or more dequeuing threads that will process elements according to a processing function supplied by the consumer.
 * <p>It also maintains basic statistics about the queue size, peak size, number of queued and rejected elements.
 * @param <E> the type of the elements in the queue.
 * @author Laurent Cohen
 */
public interface QueueHandler<E> {

  /**
   * Add the specified element to the <i>tail</i> of the queue.
   * @param element the element to add.
   * @return {@code true} if the element was successfully added, {@code false} otherwise.
   */
  boolean offer(E element);

  /**
   * Add the specified element to the <i>head</i> of the queue.
   * @param element the element to add.
   * @return {@code true} if the element was successfully added, {@code false} otherwise.
   */
  boolean offerToHead(E element);

  /**
   * Add the specified element to the <i>tail</i> of the queue, waiting if necessary for available space in the queue.
   * @param element the element to add.
   * @throws Exception if any error occurs.
   */
  void put(E element) throws Exception;

  /**
   * Add the specified element to the <i>head</i> of the queue, waiting if necessary for available space in the queue.
   * @param element the element to add.
   * @throws Exception if any error occurs.
   */
  void putToHead(E element) throws Exception;

  /**
   * Get the head element from the queue.
   * @return the head element, or {@code null} if the queue is empty.
   */
  E poll();

  /**
   * Get the head element from the queue, waiting if necessary for an element to becomes available.
   * @return the head element.
   * @throws Exception if any error occurs.
   */
  E take() throws Exception;

  /**
   * Determine whether the queue is empty.
   * @return {@code true} if the queue is empty, {@code false} otherwise.
   */
  boolean isEmpty();

  /**
   * Get the size of the queue.
   * @return the queue size as an {@code int}.
   */
  int size();

  /**
   * Get the peak queue size.
   * @return the peak queue size as an int.
   */
  int getPeakSize();

  /**
   * Get the count elements that were sucessfully queued.
   * @return the count as a {@code long} value.
   */
  long getQueuedElements();

  /**
   * Get the count elements that were rejected.
   * @return the count as a {@code long} value.
   */
  long getRejectedElements();

  /**
   * Close this queue handler.
   */
  void close();

  /**
   * An operation applied to each element taken from the queue by the dequeuer thread(s).
   * @param <E> the type of elements to handle.
   */
  @FunctionalInterface
  public interface ElementHandler<E> {
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

  /**
   * Create a builder to construct and configure a {@code QueueHandler}.
   * @return A build object ot construct and configure a {@code QueueHandler}.
   * @param <E> the type of elements in the queue.
   */
  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  /**
   * Builder class for {@link QueueHandler} instances.
   * @param <E> the type of the elements in the queue.
   * @see QueueHandler#builder()
   */
  public static class Builder<E> {
    /**
     * AN optional operation to perform on the elements taken from the queue.
     */
    private ElementHandler<E> handler;
    /**
     * The name assigned to the dequeuer thread.
     */
    private String name;
    /**
     * The number of dequeing threads.
     */
    private int nbThreads;
    /**
     * The queue capacity.
     */
    private int capacity;
    /**
     * An optional callback invoked when a new peak queue size is reached.
     */
    private PeakSizeUpdateCallback peakSizeUpdateCallback;

    /**
     * Default constructor.
     */
    private Builder() {
    }

    /**
     * Set the name of the dequeuer threads, or the name prefix if there are more than one thread.
     * @param name the name to set.
     * @return this builder, for method call chaining.
     */
    public Builder<E> named(final String name) {
      this.name = name;
      return this;
    }
    
    /**
     * Set the number of dequeuer threads to 1.
     * @return this builder, for method call chaining.
     */
    public Builder<E> usingSingleDequuerThread() {
      this.nbThreads = 1;
      return this;
    }
    
    /**
     * Set the number of dequeuer threads. If <= 0, then no dequeuing occurs.
     * @param nbThreads the number of dequeueing threads.
     * @return this builder, for method call chaining.
     */
    public Builder<E> usingDequuerThreads(final int nbThreads) {
      this.nbThreads = nbThreads;
      return this;
    }
    
    /**
     * Set the capacity (maximum size) of the queue. If <= 0, then the queue is unbounded.
     * @param capacity the number of dequeueing threads.
     * @return this builder, for method call chaining.
     */
    public Builder<E> withCapacity(final int capacity) {
      this.capacity = capacity;
      return this;
    }
    
    /**
     * Set the element handler used by the dequeuer threads, if any.
     * @param handler the handler to set.
     * @return this builder, for method call chaining.
     */
    public Builder<E> handlingElementsAs(final ElementHandler<E> handler) {
      this.handler = handler;
      return this;
    }
    
    /**
     * Set the function invoked whenever a new peak queue size is reached.
     * @param peakSizeUpdateCallback the function to set.
     * @return this builder, for method call chaining.
     */
    public Builder<E> handlingPeakSizeAs(final PeakSizeUpdateCallback peakSizeUpdateCallback) {
      this.peakSizeUpdateCallback = peakSizeUpdateCallback;
      return this;
    }

    /**
     * COnstruct and configure a {@link QueueHandler} instance using previously set configuration pproperties.
     * @return a new {@code QUeueuHandler} instance.
     */
    public QueueHandler<E> build() {
      final QueueHandlerImpl<E> queueHandler;
      if (capacity <= 0) queueHandler = new QueueHandlerImpl<>(name, handler);
      else queueHandler = new QueueHandlerImpl<>(name, capacity, handler);
      queueHandler.setPeakSizeUpdateCallback(peakSizeUpdateCallback);
      if (nbThreads > 0) queueHandler.startDequeuer(nbThreads);
      return queueHandler;
    }
  }
}