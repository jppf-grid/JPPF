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

package org.jppf.io;

import java.io.*;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.*;

import org.jppf.comm.socket.SocketWrapper;
import org.jppf.data.transform.*;
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
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace-level logging is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Ratio of free memory / requested allocation size threshold that triggers disk overflow.
   */
  private static final double FREE_MEM_TO_SIZE_RATIO = JPPFConfiguration.getProperties().getDouble("jppf.disk.overflow.threshold", 2.0d);
  /**
   * Whether to trigger a garbage collection whenever disk overflow is triggered.
   */
  private static final boolean GC_ON_DISK_OVERFLOW = JPPFConfiguration.getProperties().getBoolean("jppf.gc.on.disk.overflow", true);
  /**
   * The available heap threshold above which it is unlikely that memory fragmentation will cause object allocations to fail,
   * i.e. when there is enough free memory but not enough <i><b>contiguous</b></i> free memory. Default value is 32 MB.  
   */
  private static final long LOW_MEMORY_THRESHOLD = JPPFConfiguration.getProperties().getLong("jppf.low.memory.threshold", 32L) * 1024L * 1024L;
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
  private static final NumberFormat nf = createNumberFormat();
  /**
   * Default serilaizer to use when none is specified.
   */
  private static final ObjectSerializer defaultSerializer = createDefaultSerializer();

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
    int n = source.readInt();
    if (traceEnabled) log.trace("read data size = " + nf.format(n));
    DataLocation dl = createDataLocationMemorySensitive(n);
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
    File file = File.createTempFile("jppf", ".tmp");
    if (debugEnabled) log.debug("disk overflow: creating temp file '" + file.getCanonicalPath() + "' with size=" + nf.format(size));
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
      if (!b && GC_ON_DISK_OVERFLOW) {
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
    return ((long) (FREE_MEM_TO_SIZE_RATIO * size) < freeMem) && (freeMem > LOW_MEMORY_THRESHOLD);
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
    return unwrappedData(dl, defaultSerializer);
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
    JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
    InputStream is = null;
    if (transform != null) {
      int size = dl.getSize();
      if (fitsInMemory(size)) {
        try {
          is = unwrapData(transform, dl);
        } catch(OutOfMemoryError oome) {
          if (debugEnabled) log.debug("OOM when allocating in-memory data location, attempting disk overflow", oome);
        } finally {
          footprint.addAndGet(-size);
        }
      }
      if (is == null) is = unwrapDataToFile(transform, dl);
    } else is = dl.getInputStream();
    try {
      return ser.deserialize(is);
    } finally {
      StreamUtils.close(is);
    }
  }

  /**
   * Apply a {@link JPPFDataTransform} to the specified source and store the results in memory.
   * @param transform the {@link JPPFDataTransform} to apply.
   * @param source the source data to transform.
   * @return the transformed data as an <code>InputStream</code>.
   * @throws Exception if an error occurs while preparing the data.
   */
  public static InputStream unwrapData(final JPPFDataTransform transform, final DataLocation source) throws Exception {
    if (traceEnabled) log.trace("unwrapping to memory " + source);
    MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
    InputStream is = source.getInputStream();
    try {
      transform.unwrap(is, mbos);
    } finally {
      StreamUtils.close(is);
    }
    return new MultipleBuffersInputStream(mbos.toBufferList());
  }

  /**
   * Apply a {@link JPPFDataTransform} to the specified source and store the results in a temporary file.
   * @param transform the {@link JPPFDataTransform} to apply.
   * @param source the source data to transform.
   * @return the transformed data as a <code>File</code>.
   * @throws Exception if an error occurs while preparing the data.
   */
  public static InputStream unwrapDataToFile(final JPPFDataTransform transform, final DataLocation source) throws Exception {
    if (traceEnabled) log.trace("unwrapping to file " + source);
    File file = IOHelper.createTempFile(-1);
    OutputStream os = new BufferedOutputStream(new FileOutputStream(file));
    InputStream is = source.getInputStream();
    try {
      transform.unwrap(source.getInputStream(), os);
    } finally {
      StreamUtils.close(is);
    }
    return new BufferedInputStream(new FileInputStream(file));
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
   * Serialize an object and send it to the server.
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
    } catch(OutOfMemoryError e) {
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
    JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
    MultipleBuffersOutputStream mbos = new MultipleBuffersOutputStream();
    NotifyingOutputStream nos = new NotifyingOutputStream(mbos, new OverflowDetectorCallback());
    ser.serialize(o, nos);
    if (transform != null) {
      MultipleBuffersInputStream mbis = new MultipleBuffersInputStream(mbos.toBufferList());
      mbos = new MultipleBuffersOutputStream();
      nos = new NotifyingOutputStream(mbos, new OverflowDetectorCallback());
      transform.wrap(mbis, nos);
    }
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
    DataLocation dl = null;
    JPPFDataTransform transform = JPPFDataTransformFactory.getInstance();
    if (transform != null) {
      InputStream is = new BufferedInputStream(new FileInputStream(file));
      File file2 = IOHelper.createTempFile(-1);
      os = new BufferedOutputStream(new FileOutputStream(file2));
      nos = new NotifyingOutputStream(os, new OverflowDetectorCallback());
      transform.wrap(is, nos);
      dl = new FileDataLocation(file2);
    }
    else dl = new FileDataLocation(file);
    return dl;
  }

  /**
   * Create a number formatter for debugging purposes.
   * @return a {@link NumberFormat} instance.
   */
  private static NumberFormat createNumberFormat() {
    NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
    nf.setGroupingUsed(true);
    return nf;
  }

  /**
   * Create a default serilaizer to use when none is specifed.
   * @return an instance of ObjectSerializer, or null if none could be created.
   */
  private static ObjectSerializer createDefaultSerializer() {
    String name = "org.jppf.utils.ObjectSerializerImpl";
    try {
      Class<?> c = Class.forName(name);
      if (debugEnabled) log.debug("Loaded serializer class " + c);
      Object o = c.newInstance();
      return (ObjectSerializer) o;
    } catch(Exception e) {
      if (debugEnabled) log.debug("Could not load serializer class {}", name);
      return null;
    }
  }
}
