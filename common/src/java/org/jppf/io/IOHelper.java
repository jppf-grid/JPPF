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

package org.jppf.io;

import java.io.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.serialization.ObjectSerializer;
import org.jppf.utils.*;
import org.jppf.utils.streams.*;
import org.slf4j.*;


/**
 * Collection of utility methods to create and manipulate IO objects.
 * @author Laurent Cohen
 */
public final class IOHelper {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(IOHelper.class);
  /**
   * Determines whether debug-level logging is enabled.
   */
  private static boolean debugEnabled = LoggingUtils.isDebugEnabled(log);
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Lock used to check if there is sufficient free memory to read an object, AND reserve the memory, in a single atomic operation. 
   */
  private static final Lock lock = new ReentrantLock();
  /**
   * This is used to reserve the memory for an object about to be read, so that we don't have to lock the JVM while reading the object. 
   */
  private static final AtomicLong footprint = new AtomicLong(0L);
  /**
   * A number formatter for debugging and tracing purposes.
   */
  private static final NumberFormat nf;
  static {
    nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setGroupingUsed(true);
  }
  /**
   * Default serilaizer to use when none is specified.
   */
  private static final ObjectSerializer DEFAULT_SERIALIZER = new ObjectSerializerImpl();

  /**
   * Instantiation of this class is not permitted.
   */
  private IOHelper() {
  }

  /**
   * Create a data location object based on a comparison of the available heap memory
   * and the data location object size.
   * @param size the requested size of the data location to create.
   * @return a <code>DataLocation</code> object whose content may be stored in memory
   * or on another medium, depending on the available memory.
   * @throws Exception if an IO error occurs.
   */
  public static DataLocation createDataLocationMemorySensitive(final int size) throws Exception {
    if (fitsInMemory(size)) {
      try {
        DataLocation dl = new MultipleBuffersLocation(size);
        return dl;
      } catch (OutOfMemoryError oome) {
        if (debugEnabled) log.debug("OOM when allocating in-memory data location, attempting disk overflow", oome);
      } finally {
        footprint.addAndGet(-size);
      }
    }
    File file = createTempFile(size);
    return new FileDataLocation(file, size);
  }

  /**
   * Read a provider or task data from an input source.
   * The data may be stored in memory or on another medium depending on its size and the available memory.
   * @param source the input source from which to read the data.
   * @return A data location containing the data provider or task data.
   * @throws Exception if an error occurs while reading the data.
   */
  public static DataLocation readData(final InputSource source) throws Exception {
    return readData(source, source.readInt());
  }

  /**
   * Read a provider or task data from an input source with a known data size.
   * The data may be stored in memory or on another medium depending on its size and the available memory.
   * @param source the input source from which to read the data.
   * @param size the size of the data to read.
   * @return A data location containing the data provider or task data.
   * @throws Exception if an error occurs while reading the data.
   */
  public static DataLocation readData(final InputSource source, final int size) throws Exception {
    if (size <= 0) return null;
    if (traceEnabled) log.trace("read data size = {}", nf.format(size));
    DataLocation dl = createDataLocationMemorySensitive(size);
    dl.transferFrom(source, true);
    return dl;
  }

  /**
   * Write the specified data to the specified destination.
   * @param data the data to write.
   * @param destination tyhe destination to write to.
   * @throws Exception if any error occurs.
   */
  public static void writeData(final DataLocation data, final OutputDestination destination) throws Exception {
    destination.writeInt(data.getSize());
    data.transferTo(destination, true);
  }

  /**
   * Create a temporary file.
   * @param size the file size (for logging purposes only).
   * @return the created <code>File</code>.
   * @throws Exception if an IO error occurs.
   */
  public static File createTempFile(final int size) throws Exception {
    File file = File.createTempFile("jppf", ".tmp", FileUtils.getJPPFTempDir());
    if (debugEnabled) log.debug("disk overflow: creating temp file '{}' with size={}", file.getCanonicalPath(), nf.format(size));
    file.deleteOnExit();
    return file;
  }

  /**
   * Determines whether the data of the specified size would fit in memory.
   * @param size the data size to check.
   * @return true if the data would fit in memory, false otherwise.
   */
  public static boolean fitsInMemory(final int size) {
    lock.lock();
    try {
      if (traceEnabled) {
        long freeMem = SystemUtils.maxFreeHeap() - footprint.get();
        log.trace("free mem / requested size / footprint : {} / {} / {}", new Object[] { nf.format(freeMem), nf.format(size), nf.format(footprint.get())});
      }
      boolean b = fitsInMemory0(size);
      if (!b && IO.GC_ON_DISK_OVERFLOW) {
        if (debugEnabled) log.debug("triggering GC to avoid disk overflow, requested size={}", size);
        System.gc();
        b = fitsInMemory0(size);
      }
      if (b) footprint.addAndGet(size);
      return b;
    } finally {
      lock.unlock();
    }
  }

  /**
   * Determines whether the data of the specified size would fit in memory.
   * @param size the data size to check.
   * @return true if the data would fit in memory, false otherwise.
   */
  private static boolean fitsInMemory0(final int size) {
    long freeMem = SystemUtils.maxFreeHeap() - footprint.get();
    //if (traceEnabled) log.trace("free mem / requested size / footprint : {} / {} / {}", new Object[] { nf.format(freeMem), nf.format(size), nf.format(footprint.get())});
    return ((long) (IO.FREE_MEM_TO_SIZE_RATIO * size) < freeMem) && (freeMem > IO.LOW_MEMORY_THRESHOLD);
  }

  /**
   * Deserialize the next object available via a network connection.
   * @param socketWrapper the network connection used to read data.
   * @param ser the object serializer to use.
   * @return the transformed result as an object.
   * @throws Exception if an error occurs while preparing the data.
   */
  public static Object unwrappedData(final SocketWrapper socketWrapper, final ObjectSerializer ser) throws Exception {
    //if (traceEnabled) log.trace("unwrapping from network connection");
    InputSource sis = new SocketWrapperInputSource(socketWrapper);
    DataLocation dl = IOHelper.readData(sis);
    if (dl == null) return null;
    Object o = unwrappedData(dl, ser);
    if (traceEnabled) log.trace("unwrapping from network connection, serialized size=" + dl.getSize() + " : object=" + o);
    return o;
  }

  /**
   * Deserialize the specified data into an object, using a default serializer.
   * @param dl the data, stored in a memory-aware location.
   * @return the transformed result as an object.
   * @throws Exception if an error occurs while preparing the data.
   */
  public static Object unwrappedData(final DataLocation dl) throws Exception {
    return unwrappedData(dl, DEFAULT_SERIALIZER);
  }

  /**
   * Deserialize the specified data into an object.
   * @param dl the data, stored in a memory-aware location.
   * @param ser the object serializer to use.
   * @return the transformed result as an object.
   * @throws Exception if an error occurs while preparing the data.
   */
  public static Object unwrappedData(final DataLocation dl, final ObjectSerializer ser) throws Exception {
    if (traceEnabled) log.trace("unwrapping " + dl);
    try (InputStream is = dl.getInputStream()) {
      return ser.deserialize(is);
    }
  }

  /**
   * Serialize an object and send it to the server.
   * @param socketWrapper the socket client used to send data to the server.
   * @param o the object to serialize.
   * @param ser the object serializer.
   * @throws Exception if any error occurs.
   */
  public static void sendData(final SocketWrapper socketWrapper, final Object o, final ObjectSerializer ser) throws Exception {
    DataLocation dl = serializeData(o, ser);
    if (traceEnabled) log.trace("sending object with serialized size=" + dl.getSize() + " : " + o);
    socketWrapper.writeInt(dl.getSize());
    OutputDestination od = new SocketWrapperOutputDestination(socketWrapper);
    dl.transferTo(od, true);
  }

  /**
   * Send a null object to the server.
   * @param socketWrapper the socket client used to send data to the server.
   * @throws Exception if any error occurs.
   */
  public static void sendNullData(final SocketWrapper socketWrapper) throws Exception {
    if (traceEnabled) log.trace("sending object with serialized size=0");
    socketWrapper.writeInt(0);
  }

  /**
   * Serialize an object into a {@link DataLocation}.
   * @param o the object to serialize.
   * @return a {@link DataLocation} instance.
   * @throws Exception if any error occurs.
   */
  public static DataLocation serializeData(final Object o) throws Exception {
    return serializeData(o, IOHelper.DEFAULT_SERIALIZER);
  }

  /**
   * Serialize an object into a {@link DataLocation}.
   * @param o the object to serialize.
   * @param ser the object serializer.
   * @return a {@link DataLocation} instance.
   * @throws Exception if any error occurs.
   */
  public static DataLocation serializeData(final Object o, final ObjectSerializer ser) throws Exception {
    if (traceEnabled) log.trace("serializing object " + o);
    DataLocation dl = null;
    try {
      dl = serializeDataToMemory(o, ser);
    } catch(@SuppressWarnings("unused") OutOfMemoryError e) {
      dl = serializeDataToFile(o, ser);
    }
    return dl;
  }

  /**
   * Serialize an object to a buffer in memory.
   * @param o the object to serialize.
   * @param ser the object serializer.
   * @return an instance of {@link MultipleBuffersOutputStream}.
   * @throws Exception if any error occurs.
   */
  public static DataLocation serializeDataToMemory(final Object o, final ObjectSerializer ser) throws Exception {
    if (traceEnabled) log.trace("serializing object to memory " + o);
    MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
    //NotifyingOutputStream nos = new NotifyingOutputStream(mbos, new OverflowDetectorCallback());
    //ser.serialize(o, nos);
    ser.serialize(o, mbos);
    return new MultipleBuffersLocation(mbos.toBufferList(), mbos.size());
  }

  /**
   * Serialize an object and send it to the server.
   * @param o the object to serialize.
   * @param ser the object serializer.
   * @return an instance of {@link FileDataLocation}.
   * @throws Exception if any error occurs.
   */
  public static DataLocation serializeDataToFile(final Object o, final ObjectSerializer ser) throws Exception {
    if (traceEnabled) log.trace("serializing object to file " + o);
    File file = IOHelper.createTempFile(-1);
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    NotifyingOutputStream nos = new NotifyingOutputStream(os, new OverflowDetectorCallback());
    ser.serialize(o, nos);
    DataLocation dl = new FileDataLocation(file);
    return dl;
  }

  /**
   * @return the default serilaizer to use when none is specified.
   */
  public static ObjectSerializer getDefaultserializer() {
    return DEFAULT_SERIALIZER;
  }
}
