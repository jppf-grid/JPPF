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

package org.jppf.server.node;

import java.util.concurrent.Callable;

import org.jppf.io.*;
import org.jppf.node.protocol.*;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * The goal of this class is to serialize an object before sending it back to the server, and catch an eventual exception.
 * @exclude
 */
public class ObjectSerializationTask implements Callable<DataLocation> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ObjectSerializationTask.class);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The data to send over the network connection.
   */
  private final Object object;
  /**
   * Used to serialize the object.
   */
  private final ObjectSerializer ser;
  /**
   * The context class loader to use.
   */
  private final ClassLoader contextCL;
  /**
   * The order in which this task was submitted, if any.
   */
  private final int submitOrder;

  /**
   * Initialize this task with the specified data buffer.
   * @param object the object to serialize.
   * @param cont container used to serialize the object.
   * @param submitOrder this task's submission order.
   */
  public ObjectSerializationTask(final Object object, final JPPFContainer cont, final int submitOrder) {
    this.object = object;
    this.ser = cont.getSerializer();
    this.contextCL = cont.getClassLoader();
    this.submitOrder = submitOrder;
  }

  @Override
  public DataLocation call() {
    DataLocation dl = null;
    final boolean isTask = object instanceof Task;
    final int p = isTask ? ((Task<?>) object).getPosition() : -1;
    try {
      Thread.currentThread().setContextClassLoader(contextCL);
      if (traceEnabled) log.trace("serializing {} at position={}, submitOrder={}, job={}", toString(object), p, submitOrder, toString(isTask ? ((Task<?>) object).getJob() : object));
      dl = IOHelper.serializeData(object, ser);
      final int size = dl.getSize();
      if (traceEnabled) log.trace("serialized  {} at position={}, submitOrder={}, job={}, size={}", toString(object), p, submitOrder, toString(isTask ? ((Task<?>) object).getJob() : object), size);
    } catch(final Throwable t) {
      log.error(t.getMessage(), t);
      try {
        final JPPFExceptionResult result = (JPPFExceptionResult) HookFactory.invokeSingleHook(SerializationExceptionHook.class, "buildExceptionResult", object, t);
        result.setPosition(p);
        dl = IOHelper.serializeData(result, ser);
      } catch(final Exception e2) {
        log.error(e2.getMessage(), e2);
      }
    }
    return dl;
  }

  /**
   * @param o the object from which to get a string description.
   * @return a string description of the object.
   */
  private static String toString(final Object o) {
    if (o instanceof JPPFDistributedJob) {
      final JPPFDistributedJob job = (JPPFDistributedJob) o;
      return o.getClass().getSimpleName() + "[name=" + job.getName() + ", uuid=" + job.getUuid() + ']';
    }
    return (o == null) ? "null" : o.getClass().getSimpleName();
  }
}