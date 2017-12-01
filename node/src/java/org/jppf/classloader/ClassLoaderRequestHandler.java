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

package org.jppf.classloader;

import static org.jppf.utils.StringUtils.build;

import java.util.Map;
import java.util.concurrent.Future;

import org.jppf.utils.*;
import org.jppf.utils.concurrent.ThreadSynchronization;
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
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
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
  private CompositeResourceWrapper nextRequest = null;
  /**
   * Object which sends the class loading requets to the driver and receives the response.
   */
  private ResourceRequestRunner requestRunner;
  /**
   * A task run at constant intervals, which runs the class loading send/receive tto the server and notifies
   * waiting threads when the reponse is received.
   */
  private PeriodicTask periodicTask = new PeriodicTask();
  /**
   * A thread wrapping the periodic task.
   */
  private Thread periodicThread = null;
  /**
   * 
   */
  private int maxBatchSize = 0;

  /**
   * Initialize this request handler.
   * @param requestRunner the periodic task submitted to the scheduled executor.
   */
  public ClassLoaderRequestHandler(final ResourceRequestRunner requestRunner) {
    this.nextRequest = new CompositeResourceWrapper();
    this.requestRunner = requestRunner;
    periodicThread = new Thread(periodicTask, "PeriodicTask");
    periodicThread.start();
  }

  /**
   * Add a resource request.
   * @param resource the resource request to add.
   * @return a future for getting the respone at a later time.
   */
  public Future<JPPFResourceWrapper> addRequest(final JPPFResourceWrapper resource) {
    if (resource == null) throw new IllegalArgumentException("resource is null");

    resource.preProcess();
    Future<JPPFResourceWrapper> f;
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
    ResourceRequestRunner tmp = requestRunner;
    requestRunner = null;
    //nextRequest = null;
    periodicThread = null;
    periodicTask = null;
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
            nextRequest = new CompositeResourceWrapper();
          }
          Map<JPPFResourceWrapper, Future<JPPFResourceWrapper>> futureMap = request.getFutureMap();
          int n = futureMap.size();
          if (n > maxBatchSize) {
            maxBatchSize = n;
            log.info(build("maxBatchSize = ", maxBatchSize));
          }
          if (debugEnabled) log.debug(build("sending batch of ", futureMap.size(), " class loading requests: ", request));
          if (isStopped()) return;
          requestRunner.setRequest(request);
          requestRunner.run();
          Throwable t = requestRunner.getThrowable();
          CompositeResourceWrapper response = (CompositeResourceWrapper) requestRunner.getResponse();
          if (debugEnabled) log.debug(build("got response ", response));
          if (response != null) {
            for (JPPFResourceWrapper rw : response.getResources()) {
              ResourceFuture<JPPFResourceWrapper> f = (ResourceFuture<JPPFResourceWrapper>) futureMap.remove(rw);
              if (f != null) f.setDone(rw);
            }
          }
          for (Map.Entry<JPPFResourceWrapper, Future<JPPFResourceWrapper>> entry : futureMap.entrySet()) {
            ResourceFuture<JPPFResourceWrapper> future = (ResourceFuture<JPPFResourceWrapper>) entry.getValue();
            if (t != null) future.setThrowable(t);
            else future.setDone(null);
          }
          futureMap.clear();
          requestRunner.reset();
          start = System.nanoTime();
          elapsed = 0L;
        }
      } catch (Exception e) {
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
}
