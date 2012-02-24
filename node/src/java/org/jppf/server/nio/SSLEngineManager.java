/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

/*
 * SSLEngineManager.java
 *
 * Copyright Esmond Pitt, 2005. All rights reserved.
 *
 * Created on 30 November 2004, 18:00
 */

package org.jppf.server.nio;

import java.io.IOException;
import java.nio.*;
import java.nio.channels.SocketChannel;

import javax.net.ssl.*;

import org.slf4j.*;

/**
 * The SSLEngineManager is a higher-level wrapper for javax.net.ssl.SSLEngine.
 * It takes care of all handshaking except for blocking tasks, so the only statuses the caller needs to be aware of are the EngineStatus codes:
 *<dl>
 *<dt>OK
 *<dd>The operation completed successfully.
 *<dt>BUFFER_UNDERFLOW
 *<dd>This can be returned by SSLEngineManager.unwrap().
 * It means that there is insufficient data in the appRecvBuffer to proceed.
 * The application should not retry the SSLEngineManager.unwrap() until at least one byte of data has been read into the appRecvBuffer.
 * If the ReadableByteChannel supplied is non-null and non-blocking, the SSLEngineManager will already have
 * tried a read on the channel, and only returns this status if that read returned zero (which can only happen if the channel is non-blocking).
 * In this way the SSLEngineManager makes its best attempt to proceed without ever blocking the application.
 *
 *<dt>BUFFER_OVERFLOW
 *<dd>This can be returned by SSLEngineManager.wrap() if there is insufficient room in the target buffer.
 * The application should not retry the SSLEngineManager.wrap() until at least one byte of net data
 * has been written from the netSendBuffer. If the ReadableByteChannel supplied is non-null and non-blocking,
 * the SSLEngineManager will already have tried a write on the channel, and only returns this status
 * if that write returned zero. In this way the SSLEngineManager makes its best attempt to proceed
 * without ever blocking the application.
 *<p>
 * BUFFER_OVERFLOW can also be returned by SSLEngineManager.unwrap() if there is unsufficient room in the appRecvBuffer for the unwrapped data.
 * The application should remove application data from the AppRecvBuffer and retry the wrap.
 * The conditions under which this happens are dependent on the implementation of SSLEngine:
 * in Sun's JDK 1.5 implementation it is returned on unwrap() if the application receive buffer
 * isn't completely empty. This is pretty drastic but can be expected to improve in later releases.
 * In any case all the application has to do is respond to the condition by emptying the buffer (i.e. draining it to an application-side buffer).
 *
 *<dt>CLOSED
 *<dd>This means that the SSLEngine is closed correctly. All the application can sensibly do
 * is close the connection directly (i.e. not via SSLEngineManager.close()).
 *</dl>
 * and the EngineResult.HandshakeStatus codes:
 *<dl>
 *<dt>NEED_TASK
 *<dd>As in SSLEngine, this means that a potentially blocking task must be executed.
 *</dl>
 * The application and net buffers are maintained on these principles:
 *<ul>
 *<li>
 * they are always ready for a read() or put() operation, or to be the target of a wrap or unwrap;
 *<li>
 * they need to be flipped before a write() or get() operation and compacted afterwards.
 * This also applies to the source buffer of a wrap() or unwrap().
 *</ul>
 *<p>
 * State machine:
 *<pre>
 * HandshakeStatus  Status            Action
 * NEED_UNWRAP      BUFFER_OVERFLOW   application must read appRecvBuffer and retry.
 * NEED_UNWRAP      BUFFER_UNDERFLOW  read from network into netRecvBuffer and retry.
 * NEED_WRAP        BUFFER_OVERFLOW   write from netSendBuffer to network and retry.
 * NEED_WRAP        BUFFER_UNDERFLOW  Doesn't seem possible.
 *</pre>
 * In Sun's implementation there are no other ways to get the BUFFER_UNDERFLOW/BUFFER_OVERFLOW handshake statuses.
 * In JDK 1.5.0 the UNWRAP/BUFFER_OVERFLOW condition is triggered far too often: presently it happens
 * if the target buffer isn't completely empty, regardless of how much room is really needed.
 * I expect Sun to improve this over time.
 * There is a discussion on the java-security mailing list about this.
 *<p>
 * In JDK 1.5 it should be noted that SSLSocket/SSLServerSocket don't actually use
 * the SSLEngine so its usage and reliability are open to question.
 *<p>
 * Copyright (c) Esmond Pitt, 2005. All rights reserved.
 *
 * @author Esmond Pitt
 * @version $Revision: 8 $
 */
public class SSLEngineManager
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLEngineManager.class);
  /**
   * Determines whether DEBUG logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  /**
   * 
   */
  private SocketChannel channel;

  /**
   * 
   */
  private SSLEngine engine;

  /**
   * 
   */
  private ByteBuffer appSendBuffer;

  /**
   * 
   */
  private ByteBuffer netSendBuffer;

  /**
   * 
   */
  private ByteBuffer appRecvBuffer;

  /**
   * 
   */
  private ByteBuffer netRecvBuffer;

  /**
   * 
   */
  private SSLEngineResult engineResult = null;
  /**
   * 
   */
  int threadNumber = 1;
  /**
   * 
   */
  private int lastReadCount = 0;

  /**
   * Creates new SSLEngineManager.
   * 
   * @param channel Socket channel (the client or server end of TCP/IP connection)
   * @param engine SSLEngine: must be in the appropriate client or server mode and must be otherwise fully configured (e.g.
   * want/need/client/server/auth, cipher suites, &c) before this manager is used.
   * @throws IOException if any error occurs.
   */
  public SSLEngineManager(final SocketChannel channel, final SSLEngine engine) throws IOException
  {
    this.channel = channel;
    this.engine = engine;
    SSLSession session = engine.getSession();
    this.appSendBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());
    this.netSendBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
    this.appRecvBuffer = ByteBuffer.allocate(session.getApplicationBufferSize());
    this.netRecvBuffer = ByteBuffer.allocate(session.getPacketBufferSize());
  }

  /**
   * @return the application receive buffer
   */
  public ByteBuffer getAppRecvBuffer()
  {
    return appRecvBuffer;
  }

  /**
   * @return the application send buffer
   */
  public ByteBuffer getAppSendBuffer()
  {
    return appSendBuffer;
  }

  /**
   * @return the SSLEngine
   */
  public SSLEngine getEngine()
  {
    return engine;
  }

  /**
   * @return the number of bytes that can be read immediately.
   */
  public int available()
  {
    return engine.isInboundDone() ? -1 : appRecvBuffer.position();
  }

  /**
   * @return a {@link SSLEngineResult} instance.
   */
  public SSLEngineResult getEngineResult()
  {
    return engineResult;
  }

  /**
   * Read from the channel via the SSLEngine into the application receive buffer.
   * Called in blocking mode when input is expected, or in non-blocking mode when the channel is readable.
   * @return the number of bytes read.
   * @throws IOException if any error occurs.
   */
  public int read() throws IOException
  {
    int cipherTextCount = 0;
    int plainTextCount = appRecvBuffer.position();
    do
    {
      flush();
      if (engine.isInboundDone()) return plainTextCount > 0 ? plainTextCount : -1;
      // NB none, some, or all of the data required may already have been
      // unwrapped/decrypted, or EOF may intervene at any of these points.
      int count = channel.read(netRecvBuffer);
      netRecvBuffer.flip();
      engineResult = engine.unwrap(netRecvBuffer, appRecvBuffer);
      netRecvBuffer.compact();
      switch (engineResult.getStatus())
      {
        case BUFFER_UNDERFLOW:
          // Oops no data: time to read more ciphertext.
          if (traceEnabled) log.trace("reading into netRecv=" + netRecvBuffer);
          assert (channel.isOpen());
          cipherTextCount = channel.read(netRecvBuffer);
          if (traceEnabled) log.trace("read count=" + cipherTextCount + ", netRecv=" + netRecvBuffer);
          if (cipherTextCount == 0) return plainTextCount;
          if (cipherTextCount == -1)
          {
            if (traceEnabled) log.trace("read EOF, closing inbound");
            engine.closeInbound(); // may throw if incoming close_notify was not received, this is good.
          }
          break;
        case BUFFER_OVERFLOW:
          // throw new BufferOverflowException();
          // There is no room in appRecvBuffer to decrypt the data in netRecvBuffer.
          // The application must empty appRecvBuffer every time it gets > 0 from this method.
          // In this case all we can do is return zero to the application.
          // We are certainly not handshaking or at EOS so we can exit straight out of this loop and method.
          return 0;
        case CLOSED:
          // logger.fine("SSLEngineManager.read: isInboundDone="+engine.isInboundDone()+" engineResult "+engineResult);
          // RFC 2246 #7.2.1 requires us to stop accepting input.
          channel.socket().shutdownInput();
          // Yes but if we do that we are going to provoke outgoing RSTs on Windows platforms. EJP 18/09/2005.
          // RFC 2246 #7.2.1 requires us to respond with an outgoing close_notify.
          // This is deferred to processHandshake();
          break;
        case OK:
          plainTextCount = appRecvBuffer.position();
          // logger.fine("SSLEngineManager.read(): "+engineResult);
          break;
      }
      while (processHandshakeStatus());
      plainTextCount = appRecvBuffer.position();
    }
    while (plainTextCount == 0);
    if (engine.isInboundDone()) plainTextCount = -1;
    return plainTextCount;
  }

  /**
   * Write from the application send buffer to the channel via the SSLEngine. Called in either blocking or non-blocking
   * mode when application output is ready to send.
   * @return the number of bytes consumed.
   * @throws IOException if any error occurs.
   */
  public int write() throws IOException
  {
    //if (debugEnabled) log.trace("position=" + appSendBuffer.position());
    int count = appSendBuffer.position();
    int bytesConsumed = 0;
    int totalBytesConsumed = 0;
    // If there is stuff left over to write and we still can't write it all, proceed no further.
    //if (flush() > 0 && count > 0) return 0;
    if ((count > 0) && (flush() > 0)) return 0;
    while (count > 0)
    {
      if (traceEnabled) log.trace("before flip/wrap/compact appSend=" + appSendBuffer + " netSend=" + netSendBuffer + " count=" + count);
      appSendBuffer.flip();
      engineResult = engine.wrap(appSendBuffer, netSendBuffer);
      appSendBuffer.compact();
      if (traceEnabled) log.trace("after flip/wrap/compact  appSend=" + appSendBuffer + " netSend=" + netSendBuffer);
      switch (engineResult.getStatus())
      {
        case BUFFER_UNDERFLOW:
          if (traceEnabled) log.trace("write", new BufferUnderflowException());
          throw new BufferUnderflowException(/* "source buffer: "+engineResult.getStatus() */);
        case BUFFER_OVERFLOW:
          // netSendBuffer is full: flush it and try again
          if (traceEnabled) log.trace("BUFFER_OVERFLOW before flush() netSend=" + netSendBuffer);
          int writeCount = flush();
          if (traceEnabled) log.trace("BUFFER_OVERFLOW after flush()  netSend=" + netSendBuffer + ", writeCount=" + writeCount);
          if (writeCount == 0) return 0;
          continue;
        case CLOSED:
          // I would have thought the SSLEngine would do this itself, so this is probably unreachable.
          throw new SSLException("SSLEngine: invalid state for write - " + engineResult.getStatus());
        case OK:
          bytesConsumed = engineResult.bytesConsumed();
          totalBytesConsumed += bytesConsumed;
          count -= bytesConsumed;
          //if (bytesConsumed > 0) flush();
          break;
      }
    }
    // return count of bytes written.
    //return count;
    //if (totalBytesConsumed > 0) flush();
    return totalBytesConsumed;
  }

  /**
   * @return the number of bytes flushed.
   * @throws IOException if any error occurs.
   */
  public int flush() throws IOException
  {
    netSendBuffer.flip();
    int count = channel.write(netSendBuffer);
    netSendBuffer.compact();
    return count;
  }

  /**
   * @throws IOException if any error occurs.
   */
  public void close() throws IOException
  {
    // try a read
    // TODO what to do in blocking mode ...
    if (!engine.isInboundDone() && !channel.isBlocking()) read();
    while (netSendBuffer.position() > 0)
    {
      int count = flush();
      if (count == 0)
      {
        // Houston we have a problem, can't flush the remaining outbound data on close, what to do in non-blocking mode?
        log.error("Can't flush remaining " + netSendBuffer.remaining() + " bytes");
        break;
      }
    }
    if (traceEnabled) log.trace("close: closing outbound");
    engine.closeOutbound();
    if (traceEnabled) log.trace("close: closeOutbound handshake");
    while (processHandshakeStatus());
    if (netSendBuffer.position() > 0 && flush() == 0)
      // Houston we have a problem, can't flush the remaining outbound data on close, what to do in non-blocking mode?
      log.error("Can't flush remaining " + netSendBuffer.position() + " bytes");
    if (traceEnabled) log.trace("close: closeOutbound complete");
    // RFC 2246 #7.2.1 requires us to respond to an incoming close_notify with an outgoing close_notify,
    // but it doesn't require us to wait for it if we sent it first.
    if (!engine.isInboundDone())
    {
    }
    channel.close();
    if (traceEnabled) log.trace("close: SSLEngine & channel closed");
  }

  /**
   * 
   * @throws IOException if any error occurs.
   */
  void processEngineResult() throws IOException
  {
    while (processStatus() && processHandshakeStatus()) continue;
  }

  /**
   * 
   * @return true
   * @throws IOException if any error occurs.
   */
  boolean processHandshakeStatus() throws IOException
  {
    int count;
    // process handshake status
    switch (engine.getHandshakeStatus())
    {
      case NOT_HANDSHAKING: // not presently handshaking => session is available
      case FINISHED: // just finished handshaking, SSLSession is available
        return false;
      case NEED_TASK:
        runDelegatedTasks();
        // TODO need to do something to engineResult to stop it looping here forever
        return true; // keep going
      case NEED_WRAP:
        // output needed
        appSendBuffer.flip();
        engineResult = engine.wrap(appSendBuffer, netSendBuffer);
        appSendBuffer.compact();
        if (engineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) return (count = flush()) > 0;
        return true;
      case NEED_UNWRAP:
        // Sometimes we get > 1 handshake messages at a time ...
        netRecvBuffer.flip();
        engineResult = engine.unwrap(netRecvBuffer, appRecvBuffer);
        netRecvBuffer.compact();
        if (engineResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW)
        {
          if (traceEnabled) log.trace("unwrap underflow: reading ...");
          if (engine.isInboundDone()) count = -1;
          else
          {
            assert (channel.isOpen());
            count = channel.read(netRecvBuffer);
          }
          if (traceEnabled) log.trace("unwrap underflow readCount=" + count);
          return count > 0;
        }
        if (engineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) return false; // read data is ready but no room in appRecvBuffer
        return true;
      default: // unreachable, just for compiler
        return false;
    }
  }

  /**
   * 
   * @return true if a full SSL packet was read or written, false otherwise.
   * @throws IOException if any error occurs.
   */
  boolean processStatus() throws IOException
  {
    int count;
    // processs I/O
    if (traceEnabled) log.trace("engineResult=" + engineResult);
    switch (engineResult.getStatus())
    {
      case OK: // OK: packet was sent or received
        return true; // true;
      case CLOSED: // Orderly SSL termination from either end
        return engineResult.getHandshakeStatus() != SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
      case BUFFER_OVERFLOW:
        // output needed
        switch (engineResult.getHandshakeStatus())
        {
          case NEED_WRAP:
            // If we are wrapping we are doing output to the channel, and we can continue if we managed to write it all.
            flush();
            return netSendBuffer.position() == 0;
          case NEED_UNWRAP:
            // If we are unwrapping we are doing input from the channel but the overflow means there is no room in the appRecvBuffer, so the application has to empty it.
            // fall through
            if (traceEnabled) log.trace("netSendBuffer=" + netSendBuffer + ", netRecvBuffer=" + netRecvBuffer + ", appSendBuffer=" + appSendBuffer + ", appRecvBuffer=" + appRecvBuffer);
            return false;
          default:
            return false;
        }
      case BUFFER_UNDERFLOW:
        // input needed, existing data too short to unwrap
        if (traceEnabled) log.trace("netSendBuffer=" + netSendBuffer + ", netRecvBuffer=" + netRecvBuffer + ", appSendBuffer=" + appSendBuffer + ", appRecvBuffer=" + appRecvBuffer);
        // Underflow can only mean there is no data in the netRecvBuffer, so try a read. We can continue if we managed to read something,
        // otherwise the application has to wait (select on OP_READ).
        // First flush any pending output.
        flush();
        // now read
        count = channel.read(netRecvBuffer);
        if (traceEnabled) log.trace("underflow: read " + count + " netRecv=" + netRecvBuffer);
        // If we didn't read anything we want to exit processEngineStatus()
        return count > 0;
      default: // unreachable, just for compiler
        return false;
    }
  }

  /**
   * Run delegated tasks. This implementation runs all the presently existing delegated tasks in a single new thread.
   * Derived classes should override this method to use a strategy appropriate to their environment, e.g.
   * <ul>
   * <li>dispatching into a single existing thread,
   * <li>a new thread per task,
   * <li>a thread-pool, &c.
   * </ul>
   */
  protected void runDelegatedTasks()
  {
    Thread delegatedTaskThread = new Thread("DelegatedTaskThread-" + (threadNumber++))
    {
      @Override
      public void run()
      {
        // run delegated tasks
        Runnable task;
        while ((task = engine.getDelegatedTask()) != null)
        {
          //if (debugEnabled) log.trace(this.getName() + ".runDelegatedTask: " + task);
          task.run();
        }
      }
    };
    delegatedTaskThread.run();
  }

  /**
   * @return the last count of bytes put in the app receive buffer.
   */
  public int getLastReadCount()
  {
    return lastReadCount;
  }

  /**
   * @param lastReadCount the last count of bytes put in the app receive buffer.
   */
  public void setLastReadCount(final int lastReadCount)
  {
    this.lastReadCount = lastReadCount;
  }
}
