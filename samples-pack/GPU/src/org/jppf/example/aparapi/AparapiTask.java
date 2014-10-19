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

package org.jppf.example.aparapi;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.*;

import org.jppf.server.protocol.JPPFTask;

import com.amd.aparapi.*;
import com.amd.aparapi.Kernel.EXECUTION_MODE;

/**
 * This task performs the multiplication of 2 square dense matrices.
 * It uses APARAPI to execute on a GPU if one is available.
 * If no GPU is available, APARAPI will default to executing in a Java thread pool.
 * Additionally, this task ensures that only one GPU execution is performed at any given time,
 * to avoid race conditions with unpredicatble consequences.
 * @author Laurent Cohen
 */
public class AparapiTask extends JPPFTask {
  /**
   * Used to display information only on the first Aparapi invocation.
   */
  private static AtomicBoolean first = new AtomicBoolean(false);
  /**
   * Used to ensure that only one thread at a time is using the GPU.
   * This avoids crashes at execution time.
   */
  private static Lock lockGPU = new ReentrantLock();
  /**
   * The first matrix of the multiplication (1st operand).
   */
  SquareMatrix matrixA;
  /**
   * The second matrix of the multiplication (2nd operand).
   */
  SquareMatrix matrixB;
  /**
   * The execution mode for this task.
   */
  String execMode = "GPU";

  /**
   * Initialize this task with the specified operands.
   * @param matrixA the first matrix of the multiplication (1st operand).
   * @param matrixB the second matrix of the multiplication (2nd operand).
   * @param execMode determines the execution mode, either "GPU" or "JTP" (Java Thread Pool).
   */
  public AparapiTask(final SquareMatrix matrixA, final SquareMatrix matrixB, final String execMode) {
    this.matrixA = matrixA;
    this.matrixB = matrixB;
    this.execMode = execMode;
  }

  @Override
  public void run() {
    MatrixKernel kernel = null;
    try {
      final int size = matrixB.getSize();
      kernel = new MatrixKernel(matrixA.getValues(), matrixB.getValues(), size);
      kernel.setExecutionMode("GPU".equalsIgnoreCase(execMode) ? EXECUTION_MODE.GPU : EXECUTION_MODE.JTP);

      // make sure that only one thread at a time is using the GPU
      lockGPU.lock();
      try {
        kernel.execute(size);
        if (first.compareAndSet(false, true)) {
          if (!kernel.getExecutionMode().equals(Kernel.EXECUTION_MODE.GPU)) System.out.println("Kernel did not execute on the GPU!");
          else System.out.println("*** Kernel executed on the GPU!");
          printDevicesInformation();
        }
      } finally {
        lockGPU.unlock();
      }

      // set the multiplication result as the result of this task
      setResult(new SquareMatrix(kernel.getKernelResults()));

    } catch (Exception e) {
      setThrowable(e);
    } finally {
      if (kernel != null) kernel.dispose();
      // we don't need to send the operands back to the client.
      // nullifying them will save a lot of time on the overall job execution.
      matrixA = null;
      matrixB = null;
    }
  }

  /**
   * Print information on the available OpenCL devices.
   */
  private void printDevicesInformation() {
    MyDeviceSelector selector = new MyDeviceSelector();
    OpenCLDevice.select(selector);
    List<OpenCLDevice> list = selector.getDevices();
    int count = 0;
    for (OpenCLDevice device: list) System.out.println("#" + count++ + ": " + device);
  }

  /**
   * A device selector which collects the list of available devices.
   * APARAPI doesn't provide any means to simply list the devices, so
   * we had to implement this as a workaround.
   */
  private class MyDeviceSelector implements OpenCLDevice.DeviceSelector {
    /**
     * The collected devices.
     */
    private final List<OpenCLDevice> devices = new ArrayList<>();

    @Override
    public OpenCLDevice select(final OpenCLDevice device) {
      devices.add(device);
      return null;
    }

    /**
     * Get the collected devices.
     * @return a list of <code>OpenCLDevice</code> instances.
     */
    public List<OpenCLDevice> getDevices() {
      return devices;
    }
  }
}
