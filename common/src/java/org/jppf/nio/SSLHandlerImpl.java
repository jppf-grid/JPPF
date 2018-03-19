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
import java.net.SocketException;
import java.nio.*;
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
      final int pos = appReceiveBuffer.position();
      //flush();
      if (sslEngine.isInboundDone()) return -1;
      int count = doRead();
      netReceiveBuffer.flip();
      final SSLEngineResult sslEngineResult = sslEngine.unwrap(netReceiveBuffer, appReceiveBuffer);
      netReceiveBuffer.compact();
      if (traceEnabled) log.trace("{}", sslEngineResult.getStatus());
      switch (sslEngineResult.getStatus()) {
        case BUFFER_UNDERFLOW:
          if (traceEnabled) log.trace("BUFFER_UNDERFLOW, " + printReceiveBuffers());
          return 0;

        case BUFFER_OVERFLOW:
          throw new BufferOverflowException();

        case CLOSED:
          channel.shutdownInput();
          break;

        case OK:
          break;
      }
      while (processHandshake());
      if (count == -1) sslEngine.closeInbound();
      if (sslEngine.isInboundDone()) return -1;
      count = appReceiveBuffer.position() - pos;
      return count;
    }
  }

  @Override
  public int write() throws Exception {
    synchronized(channel) {
      final int pos = appSendBuffer.position();
      netSendBuffer.clear();
      appSendBuffer.flip();
      if (traceEnabled) log.trace("before wrap, " + printSendBuffers() + ", pos=" + pos);
      final SSLEngineResult sslEngineResult = sslEngine.wrap(appSendBuffer,netSendBuffer);
      final SSLEngineResult.Status status = sslEngineResult.getStatus();
      if (traceEnabled) log.trace(String.format("after wrap, status = %s, %s, pos=%d", status, printSendBuffers(), pos));
      appSendBuffer.compact();
      switch (sslEngineResult.getStatus()) {
        case BUFFER_UNDERFLOW:
          throw new BufferUnderflowException();

        case BUFFER_OVERFLOW:
          throw new BufferOverflowException();

        case CLOSED:
          throw new SSLException("SSLEngine is CLOSED");

        case OK:
          if (traceEnabled) log.trace("OK");
          break;
      }
      while (processHandshake());
      flush();
      return pos - appSendBuffer.position();
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
        appSendBuffer.flip();
        sslEngineResult = sslEngine.wrap(appSendBuffer, netSendBuffer);
        if (traceEnabled) log.trace("{} engine status after wrap() = {}", handshakeStatus, sslEngineResult.getStatus());
        appSendBuffer.compact();
        try {
          flush();
        } catch (final SocketException e) {
          if (sslEngineResult.getStatus() == SSLEngineResult.Status.CLOSED) log.debug(e.getMessage(), e);
          else throw e;
        }
        break;

      case NEED_UNWRAP:
        final int n = sslEngine.isInboundDone() ? -1 : channel.read(netReceiveBuffer);
        netReceiveBuffer.flip();
        if (traceEnabled) log.trace(String.format("%s before unwrap, %s, n=%d", handshakeStatus, printReceiveBuffers(), n));
        sslEngineResult = sslEngine.unwrap(netReceiveBuffer, appReceiveBuffer);
        if (traceEnabled) log.trace(String.format("%s engine status after unwrap = %s, %s", handshakeStatus, sslEngineResult.getStatus(), printReceiveBuffers()));
        netReceiveBuffer.compact();
        if (traceEnabled) log.trace(String.format("%s engine status after compact = %s, %s", handshakeStatus, sslEngineResult.getStatus(), printReceiveBuffers()));
        break;

      default:
        return false;
    }
    switch (sslEngineResult.getStatus()) {
      case BUFFER_UNDERFLOW:
      case BUFFER_OVERFLOW:
        return false;

      case CLOSED:
        if (sslEngine.isOutboundDone()) channel.shutdownOutput();
        return false;
  
       case OK:
         break;
    }
    return true;
  }

  @Override
  public int flush() throws IOException {
    synchronized(channel) {
      netSendBuffer.flip();
      if (traceEnabled) log.trace("channelSendBuffer = {}", netSendBuffer);
      final int n = channel.write(netSendBuffer);
      if (traceEnabled) log.trace("write result = {}", n);
      if (n > 0) channelWriteCount += n;
      netSendBuffer.compact();
      return n;
    }
  }

  /**
   * Read bytes from the underlying channel.
   * @return the number of bytes read.
   * @throws IOException if any error occurs.
   */
  private int doRead() throws IOException {
    final int n = channel.read(netReceiveBuffer);
    if (traceEnabled) log.trace("read result = {}", n);
    if (n > 0) channelReadCount += n;
    //else if (n < 0) throw new EOFException("EOF reading inbound channel");
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
      while (netSendBuffer.position() > 0) {
        final int n = flush();
        if (n == 0) {
          log.error("unable to flush remaining " + netSendBuffer.remaining() + " bytes");
          break;
        }
      }
      sslEngine.closeOutbound();
      if (traceEnabled) log.trace("close outbound handshake");
      while (processHandshake());
      if (netSendBuffer.position() > 0 && flush() == 0) log.error("unable to flush remaining " + netSendBuffer.position() + " bytes");
      if (traceEnabled) log.trace("close outbound done");
      channel.close();
      if (traceEnabled) log.trace("SSLEngine closed");
    }
  }
}
