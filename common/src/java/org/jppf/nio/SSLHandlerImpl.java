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

package org.jppf.nio;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.channels.SocketChannel;

import javax.net.ssl.*;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;

import org.slf4j.*;

/**
 * Wrapper for an {@link SSLEngine} and an associated channel.
 * @exclude
 */
public class SSLHandlerImpl extends AbstractSSLHandler {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(SSLHandlerImpl.class);
  /**
   * Determines whether TRACE logging level is enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();

  /**
   * Instantiate this SSLHandler with the specified channel and SSL engine.
   * @param channel the channel from which data is read or to which data is written.
   * @param sslEngine performs the SSL-related operations before sending data/after receiving data.
   * @throws Exception if any error occurs.
   */
  public SSLHandlerImpl(final ChannelWrapper<?> channel, final SSLEngine sslEngine) throws Exception {
    this(channel.getSocketChannel(), sslEngine);
  }

  /**
   * Instantiate this SSLHandler with the specified channel and SSL engine.
   * @param channel the channel from which data is read or to which data is written.
   * @param sslEngine performs the SSL-related operations before sending data/after receiving data.
   * @throws Exception if any error occurs.
   */
  public SSLHandlerImpl(final SocketChannel channel, final SSLEngine sslEngine) throws Exception {
    super(channel, sslEngine);
  }

  @Override
  public int read() throws Exception {
    synchronized(channel) {
      channelReadCount = 0L;
      int sslCount = 0;
      int count = applicationReceiveBuffer.position();
      do {
        flush();
        if (sslEngine.isInboundDone()) return count > 0 ? count : -1;
        doRead();
        channelReceiveBuffer.flip();
        final SSLEngineResult sslEngineResult = sslEngine.unwrap(channelReceiveBuffer, applicationReceiveBuffer);
        channelReceiveBuffer.compact();
        if (traceEnabled) log.trace("{}", sslEngineResult.getStatus());
        switch (sslEngineResult.getStatus()) {
          case BUFFER_UNDERFLOW:
            if (traceEnabled) log.trace("BUFFER_UNDERFLOW, " + printReceiveBuffers());
            sslCount = doRead();
            if (traceEnabled) log.trace("BUFFER_UNDERFLOW, sslCount=" + sslCount + ", " + printReceiveBuffers());
            if (sslCount == 0) return count;
            if (sslCount == -1) {
              if (traceEnabled) log.trace("reached EOF, closing inbound");
              sslEngine.closeInbound();
            }
            break;
  
          case BUFFER_OVERFLOW:
            return 0;
  
          case CLOSED:
            channel.shutdownInput();
            break;
  
          case OK:
            count = applicationReceiveBuffer.position();
            break;
        }
        while (processHandshake());
        count = applicationReceiveBuffer.position();
      } while (count == 0);
      if (sslEngine.isInboundDone()) count = -1;
      return count;
    }
  }

  @Override
  public int write() throws Exception {
    synchronized(channel) {
      int remaining = applicationSendBuffer.position();
      if (traceEnabled) log.trace("position=" + applicationSendBuffer.position());
      channelWriteCount = 0L;
      int writeCount = 0;
      if ((remaining > 0) && (flush() > 0)) return 0;
      while (remaining > 0) {
        applicationSendBuffer.flip();
        if (traceEnabled) log.trace("before wrap, " + printSendBuffers() + ", remaining=" + remaining);
        final SSLEngineResult sslEngineResult = sslEngine.wrap(applicationSendBuffer, channelSendBuffer);
        final SSLEngineResult.Status status = sslEngineResult.getStatus();
        if (traceEnabled) log.trace(String.format("after wrap, status = %s, %s, remaining=%d", status, printSendBuffers(), remaining));
        applicationSendBuffer.compact();
        switch (status) {
          case BUFFER_UNDERFLOW:
            final Exception e = new BufferUnderflowException();
            if (traceEnabled) log.trace("buffer underflow", e);
            throw e;
  
          case BUFFER_OVERFLOW:
            if (traceEnabled) log.trace("BUFFER_OVERFLOW, before flush(), " + printSendBuffers());
            final int flushCount = flush();
            if (traceEnabled) log.trace("BUFFER_OVERFLOW, after flush(), =" + printSendBuffers() + ", flushCount=" + flushCount);
            if (flushCount == 0) return 0;
            continue;
  
          case CLOSED:
            if (traceEnabled) log.trace("closed");
            throw new SSLException("outbound closed");
  
          case OK:
            final int n = sslEngineResult.bytesConsumed();
            if (traceEnabled) log.trace("ok, n = {}", n);
            //if (n == 0) return writeCount;
            writeCount += n;
            remaining -= n;
            break;
        }
        while (processHandshake());
      }
      return writeCount;
    }
  }

  /**
   * Process the current handshaking status.
   * @return <code>true</code> if handshaking is still ongoing, <code>false</code> otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean processHandshake() throws Exception {
    if (!checkChannel()) throw new IOException("invalid state for channel " + channel);
    if (traceEnabled) log.trace("buffers = {}", printBuffers());
    int count;
    final SSLEngineResult sslEngineResult;
    final HandshakeStatus handshakeStatus = sslEngine.getHandshakeStatus();
    if (traceEnabled) log.trace("handshakeStatus = {}", handshakeStatus);
    switch (handshakeStatus) {
      case NOT_HANDSHAKING:
      case FINISHED:
        return false;

      case NEED_TASK:
        performDelegatedTasks();
        return true;

      case NEED_WRAP:
        applicationSendBuffer.flip();
        sslEngineResult = sslEngine.wrap(applicationSendBuffer, channelSendBuffer);
        if (traceEnabled) log.trace("{} engine status after wrap() = {}", handshakeStatus, sslEngineResult.getStatus());
        applicationSendBuffer.compact();
        if (sslEngineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) {
          count = flush();
          return count > 0;
        }
        return true;

      case NEED_UNWRAP:
        channelReceiveBuffer.flip();
        if (traceEnabled) log.trace(String.format("%s before unwrap, %s", handshakeStatus, printReceiveBuffers()));
        sslEngineResult = sslEngine.unwrap(channelReceiveBuffer, applicationReceiveBuffer);
        if (traceEnabled) log.trace(String.format("%s engine status after unwrap = %s, %s", handshakeStatus, sslEngineResult.getStatus(), printReceiveBuffers()));
        channelReceiveBuffer.compact();
        if (traceEnabled) log.trace(String.format("%s engine status after compact = %s, %s", handshakeStatus, sslEngineResult.getStatus(), printReceiveBuffers()));
        if (sslEngineResult.getStatus() == SSLEngineResult.Status.BUFFER_UNDERFLOW) {
          if (sslEngine.isInboundDone()) count = -1;
          else count = doRead();
          if (traceEnabled) log.trace("NEED_UNWRAP ==> BUFFER_UNDERFLOW, readCount=" + count);
          return count > 0;
        }
        if (sslEngineResult.getStatus() == SSLEngineResult.Status.BUFFER_OVERFLOW) return false;
        return true;

      default:
        return false;
    }
  }

  @Override
  public int flush() throws IOException {
    synchronized(channel) {
      channelSendBuffer.flip();
      if (traceEnabled) log.trace("channelSendBuffer = {}", channelSendBuffer);
      final int n = channel.write(channelSendBuffer);
      if (traceEnabled) log.trace("write result = {}", n);
      if (n > 0) channelWriteCount += n;
      channelSendBuffer.compact();
      return n;
    }
  }

  /**
   * Read bytes from the underlying channel.
   * @return the number of bytes read.
   * @throws IOException if any error occurs.
   */
  private int doRead() throws IOException {
    final int n = channel.read(channelReceiveBuffer);
    if (traceEnabled) log.trace("read result = {}", n);
    if (n > 0) channelReadCount += n;
    else if (n < 0) throw new EOFException("EOF reading inbound channel");
    return n;
  }

  /**
   * Check whether the channel is valid.
   * @return {@code true} if this channel is valid, {@code false} otherwise.
   * @throws Exception if any error occurs.
   */
  private boolean checkChannel() throws Exception {
    return channel.isOpen() && channel.isConnected() && channel.isRegistered();
  }

  @Override
  public void close() throws Exception {
    synchronized(channel) {
      if (!sslEngine.isInboundDone() && !channel.isBlocking()) read();
      while (channelSendBuffer.position() > 0) {
        final int n = flush();
        if (n == 0) {
          log.error("unable to flush remaining " + channelSendBuffer.remaining() + " bytes");
          break;
        }
      }
      sslEngine.closeOutbound();
      if (traceEnabled) log.trace("close outbound handshake");
      while (processHandshake());
      if (channelSendBuffer.position() > 0 && flush() == 0) log.error("unable to flush remaining " + channelSendBuffer.position() + " bytes");
      if (traceEnabled) log.trace("close outbound done");
      channel.close();
      if (traceEnabled) log.trace("SSLEngine closed");
    }
  }
}
