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

package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

import java.util.Map;
import java.util.concurrent.Future;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * This class manages the batching of class loading requests at regular intervals.
 * @author Laurent Cohen
 * @exclude
 */
public class ClassLoaderRequestHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(ClassLoaderRequestHandler.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Maximum time to wait in nanoseconds before sending the next request.
   */
  private static final long MAX_WAIT = JPPFConfiguration.get(JPPFProperties.NODE_CLASSLOADING_BATCH_PERIOD);
  /**
   * How many nanaoseconds in a millisecond.
   */
  private static final int NANO_RANGE = 1000000;
  /**
   * The batch request to which resource requests are added.
   */
  private CompositeResourceWrapper nextRequest;
  /**
   * Object which sends the class loading requets to the driver and receives the response.
   */
  private ResourceRequestRunner requestRunner;
  /**
   * A task run at constant intervals, which runs the class loading send/receive tto the server and notifies
   * waiting threads when the reponse is received.
   */
  private final PeriodicTask periodicTask = new PeriodicTask();
  /**
   * A thread wrapping the periodic task.
   */
  private Thread periodicThread;
  /**
   * 
   */
  private int peakBatchSize;

  /**
   * Initialize this request handler.
   * @param requestRunner the periodic task submitted to the scheduled executor.
   */
  public ClassLoaderRequestHandler(final ResourceRequestRunner requestRunner) {
    this.nextRequest = newRequest();
    this.requestRunner = requestRunner;
    periodicThread = ThreadUtils.startDaemonThread(periodicTask, "PeriodicTask");
  }

  /**
   * Add a resource request.
   * @param resource the resource request to add.
   * @return a future for getting the respone at a later time.
   */
  public Future<JPPFResourceWrapper> addRequest(final JPPFResourceWrapper resource) {
    if (resource == null) throw new IllegalArgumentException("resource is null");
    resource.preProcess();
    final Future<JPPFResourceWrapper> f;
    synchronized (periodicTask) {
      f = nextRequest.addResource(resource);
    }
    periodicTask.wakeUp();
    return f;
  }

  /**
   * Close this request handler and release its resources.
   * @return the {@link ResourceRequestRunner}.
   */
  public ResourceRequestRunner close() {
    if (debugEnabled) log.debug("closing request handler");
    periodicTask.setStopped(true);
    periodicThread.interrupt();
    final ResourceRequestRunner tmp = requestRunner;
    requestRunner = null;
    periodicThread = null;
    return tmp;
  }

  /**
   * 
   */
  private class PeriodicTask extends ThreadSynchronization implements Runnable {
    @Override
    public void run() {
      try {
        long elapsed = 0L;
        while (!isStopped()) {
          CompositeResourceWrapper request = null;
          long start = System.nanoTime();
          synchronized (this) {
            while (nextRequest.getFutureMap().isEmpty() && !isStopped()) goToSleep();
            while (((elapsed = System.nanoTime() - start) < MAX_WAIT) && !isStopped()) {
              goToSleep((MAX_WAIT - elapsed) / NANO_RANGE, (int) ((MAX_WAIT - elapsed) % NANO_RANGE));
            }
            if (isStopped()) return;
            request = nextRequest;
            nextRequest = newRequest();
          }
          final Map<JPPFResourceWrapper, Future<JPPFResourceWrapper>> futureMap = request.getFutureMap();
          final int n = futureMap.size();
          if (n > peakBatchSize) {
            peakBatchSize = n;
            log.info(build("peakBatchSize = ", peakBatchSize));
          }
          if (debugEnabled) log.debug(build("sending batch of ", futureMap.size(), " class loading requests: ", request));
          if (isStopped()) return;
          requestRunner.setRequest(request);
          requestRunner.run();
          final Throwable t = requestRunner.getThrowable();
          final CompositeResourceWrapper response = (CompositeResourceWrapper) requestRunner.getResponse();
          if (debugEnabled) log.debug(build("got response ", response));
          if (response != null) {
            for (final JPPFResourceWrapper rw : response.getResources()) {
              final ResourceFuture<JPPFResourceWrapper> f = (ResourceFuture<JPPFResourceWrapper>) futureMap.remove(rw);
              if (f != null) f.setDone(rw);
            }
          }
          for (final Map.Entry<JPPFResourceWrapper, Future<JPPFResourceWrapper>> entry : futureMap.entrySet()) {
            final ResourceFuture<JPPFResourceWrapper> future = (ResourceFuture<JPPFResourceWrapper>) entry.getValue();
            if (t != null) future.setThrowable(t);
            else future.setDone(null);
          }
          futureMap.clear();
          requestRunner.reset();
          start = System.nanoTime();
          elapsed = 0L;
        }
      } catch (final Exception e) {
        if (debugEnabled) log.debug(e.getMessage(), e);
        else log.warn(ExceptionUtils.getMessage(e));
      }
    }
  }

  /**
   * Get the bject which sends the class loading requets to the driver and receives the response.
   * @return a {@link ResourceRequestRunner} instance.
   */
  public ResourceRequestRunner getRequestRunner() {
    return requestRunner;
  }

  /**
   * @return a new resource request.
   */
  private static CompositeResourceWrapper newRequest() {
    final CompositeResourceWrapper request = new CompositeResourceWrapper();
    request.setState(JPPFResourceWrapper.State.NODE_REQUEST);
    return request;
  }
}
