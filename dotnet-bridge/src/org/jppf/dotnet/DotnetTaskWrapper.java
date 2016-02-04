/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.dotnet;

import java.lang.reflect.Method;

import org.jppf.node.protocol.*;

/**
 * This task wraps a .Net task to which operations are delegated.
 * @author Laurent Cohen
 * @since 5.0
 * @exclude
 */
public class DotnetTaskWrapper extends AbstractTask<Object> implements CancellationHandler, TimeoutHandler {
  /**
   * The serialized dotnet task.
   */
  private byte[] bytes = null;
  /**
   * Whether looging messages are printed to the console.
   */
  private boolean loggingEnabled = false;
  /**
   * The class of the proxy for DotnetSerializer
   */
  private transient Class<?> serializerClass;
  /**
   * An instance of the proxy for DotnetSerializer
   */
  private transient Object serializer;

  /**
   * Initialize this wrapper with the specified serialized dotnet task.
   * @param bytes the serialized dotnet task.
   */
  public DotnetTaskWrapper(final byte[] bytes) {
    this.bytes = bytes;
  }

  @Override
  public void run() {
    try {
      // create a serializer instance
      if (loggingEnabled) System.out.println("[Java] creating serializer instance");
      doInit();
      // deserialize the .Net task, execute it and serialize its new state
      if (loggingEnabled) System.out.println("[Java] executing the dotnet task");
      doExecute();
      String s = "execution successful for " + this;
      if (loggingEnabled) System.out.println("[Java]  " + s);
      setResult(s);
    } catch (Throwable t) {
      t.printStackTrace();
      Throwable cause = t.getCause();
      setThrowable(cause == null ? t : cause);
      //setResult("execution failure: " + ExceptionUtils.getStackTrace(t));
    }
  }

  /**
   * Lookup the {@link Class} for the serializer proxy and create a serializer instance.
   * @throws Throwable if any error occurs.
   */
  private void doInit() throws Throwable {
    serializerClass = Class.forName("org.jppf.dotnet.DotnetSerializer");
    serializer = serializerClass.newInstance();
  }

  /**
   * Invokes the {@code Execute()} method on the {@code DotnetSerializer} Java proxy.
   * @throws Throwable if any error occurs.
   */
  private void doExecute() throws Throwable {
    if (serializer != null) {
      Method m = serializerClass.getDeclaredMethod("Execute", byte[].class);
      bytes = (byte[]) m.invoke(serializer, bytes);
    }
  }

  /**
   * Get the serialized dotnet task.
   * @return the serialized dotnet task as an array of bytes.
   */
  public byte[] getBytes() {
    return bytes;
  }

  @Override
  public void onCancel() {
    if (loggingEnabled) System.out.println("[Java] task " + this + " has been cancelled");
  }

  /**
   * Invokes the {@code Cancel()} method on the {@code DotnetSerializer} Java proxy.
   * @throws Exception if any error occurs while this callback is executing.
   */
  @Override
  public void doCancelAction() throws Exception {
    if (loggingEnabled) System.out.println("cancelling dot net task");
    if (serializer != null) {
      Method m = serializerClass.getDeclaredMethod("Cancel");
      m.invoke(serializer);
    }
  }

  /**
   * Invokes the {@code Timeout()} method on the {@code DotnetSerializer} Java proxy.
   * @throws Exception if any error occurs while this callback is executing.
   */
  @Override
  public void doTimeoutAction() throws Exception {
    if (loggingEnabled) System.out.println("dotnet task has timed out");
    if (serializer != null) {
      Method m = serializerClass.getDeclaredMethod("Timeout");
      m.invoke(serializer);
    }
  }

  /**
   * Determine whether looging messages are printed to the console.
   * @return {@code true} if logging is enabled, {@code false} otherwise.
   */
  public boolean isLoggingEnabled() {
    return loggingEnabled;
  }

  /**
   * Specify whether looging messages are printed to the console.
   * @param loggingEnabled {@code true} if logging is enabled, {@code false} otherwise.
   */
  public void setLoggingEnabled(final boolean loggingEnabled) {
    this.loggingEnabled = loggingEnabled;
  }
}
