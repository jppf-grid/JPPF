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
import org.jppf.utils.ExceptionUtils;
import org.jppf.utils.hooks.HookFactory;
import org.slf4j.*;

/**
 * Instances of this class are used to deserialize objects from an
 * incoming message in parallel.
 * @exclude
 */
public class ObjectDeserializationTask implements Callable<ObjectDeserializationTask> {
  /**
   * Logger for this class.
   */
  private static final Logger log = LoggerFactory.getLogger(ObjectDeserializationTask.class);
  /**
   * Determines whether the trace level is enabled in the logging configuration, without the cost of a method call.
   */
  private static final boolean traceEnabled = log.isTraceEnabled();
  /**
   * The data received over the network connection.
   */
  private final DataLocation dl;
  /**
   * Index of the object to deserialize in the incoming IO message; used for debugging purposes.
   */
  private final int index;
  /**
   * The deserialized object.
   */
  private Object object;
  /**
   * The container that provides the class loader used to desrialize.
   */
  private final JPPFContainer cont;
  /**
   * The task bundle the data to deserialize is a part of.
   */
  private final TaskBundle bundle;

  /**
   * Initialize this task with the specified data buffer.
   * @param cont .
   * @param bundle the task bundle the data to deserialize is a part of.
   * @param dl the data read from the network connection, stored in a memory-sensitive location.
   * @param index index of the object to deserialize in the incoming IO message; used for debugging purposes.
   */
  public ObjectDeserializationTask(final JPPFContainer cont, final TaskBundle bundle, final DataLocation dl, final int index) {
    this.cont = cont;
    this.dl = dl;
    this.index = index;
    this.bundle = bundle;
  }

  /**
   * Execute this task.
   * @return a deserialized object.
   */
  @Override
  public ObjectDeserializationTask call() {
    final ClassLoader cl = Thread.currentThread().getContextClassLoader();
    try {
      TaskThreadLocals.setRequestUuid(bundle.getUuid());
      Thread.currentThread().setContextClassLoader(cont.getClassLoader());
      if (traceEnabled) log.trace("deserializing object index = {}, classloader = {}", index, cont.getClassLoader());
      if (cont.isSequentialDeserialization()) cont.getLock().lock();
      try {
        object = IOHelper.unwrappedData(dl, cont.getSerializer());
      } finally {
        if (cont.isSequentialDeserialization()) cont.getLock().unlock();
      }
      if (traceEnabled) log.trace("deserialized object at index {} (a {})", index, (object == null) ? "null object" : object.getClass().getName());
    } catch (final Throwable t) {
      final String desc = (index == 0 ? "data provider" : "task at index " + index) + " could not be deserialized";
      if (traceEnabled) log.debug("{} : {}", desc, ExceptionUtils.getStackTrace(t));
      else log.error("{} : {}", desc, ExceptionUtils.getMessage(t));
      if (index > 0) object = HookFactory.invokeSingleHook(SerializationExceptionHook.class, "buildExceptionResult", desc, t);
    } finally {
      Thread.currentThread().setContextClassLoader(cl);
    }
    return this;
  }

  /**
   * @return the index of the object to deserialize in the incoming IO message; used for debugging purposes.
   */
  public int getIndex() {
    return index;
  }

  /**
   * @return the deserialized object.
   */
  public Object getObject() {
    return object;
  }
}