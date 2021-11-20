/*
 * JPPF.
 * Copyright (C) 2005-2019 JPPF Team.
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

package test.org.jppf.comm.interceptor;

import static org.junit.Assert.*;

import java.net.*;
import java.nio.channels.*;
import java.util.List;

import org.jppf.comm.interceptor.*;
import org.jppf.utils.*;
import org.jppf.utils.concurrent.*;
import org.jppf.utils.concurrent.ConcurrentUtils.ConditionFalseOnException;
import org.junit.Test;
import org.slf4j.*;

import test.org.jppf.test.setup.BaseTest;
import test.org.jppf.test.setup.common.TestInterceptor;

/**
 * Test the network interceptor feature.
 * @author Laurent Cohen
 */
public class TestNetworkConnectionInterceptor extends BaseTest {
  /**
   * 
   */
  private static final int PORT = 4444;

  /**
   * Test that the interceptor works with a Socket / ServerSocket.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testSocketClientAndServer() throws Exception {
    print(false, false, "TestInterceptor state: %s", TestInterceptor.state());
    SocketServer server = null;
    Socket client = null;
    try {
      //print(false, false, "class path:\n%s", prettySystemClassPath());
      print(false, false, "getting interceptor list");
      final List<NetworkConnectionInterceptor> interceptors = NetworkConnectionInterceptor.INTERCEPTORS;
      print(false, false, "interceptor list: %s", interceptors);
      assertNotNull(interceptors);
      assertEquals(1, interceptors.size());
      final NetworkConnectionInterceptor interceptor = interceptors.get(0);
      assertTrue(interceptor instanceof TestInterceptor);
      TestInterceptor.active = true;
      print(false, false, "interceptor %s now active", interceptor);
      server = new SocketServer();
      print(false, false, "starting server %s", server);
      ThreadUtils.startThread(server, "socket server thread");
      Thread.sleep(250L);
      print(false, false, "starting client");
      client = new Socket("localhost", PORT);
      print(false, false, "started client %s", client);
      assertTrue(client.isConnected());
      assertTrue(InterceptorHandler.invokeOnConnect(client, JPPFChannelDescriptor.UNKNOWN));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      print(false, false, "closing client and server");
      close(client, server);
      TestInterceptor.active = false;
      print(false, false, "interceptor now inactive");
      TestInterceptor.resetMessages();
    }
  }

  /**
   * Test that the interceptor works with a SocketChannel / ServerSocketChannel.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testSocketChannelClientAndServer() throws Exception {
    ChannelServer server = null;
    SocketChannel client = null;
    try {
      final List<NetworkConnectionInterceptor> interceptors = NetworkConnectionInterceptor.INTERCEPTORS;
      print(false, false, "interceptor list: %s", interceptors);
      TestInterceptor.active = true;
      server = new ChannelServer();
      ThreadUtils.startThread(server, "socket channel server thread");
      Thread.sleep(250L);
      client = SocketChannel.open(new InetSocketAddress("localhost", PORT));
      client.finishConnect();
      assertTrue(client.isConnected());
      client.configureBlocking(true);
      assertTrue(InterceptorHandler.invokeOnConnect(client, JPPFChannelDescriptor.UNKNOWN));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      close(client, server);
      TestInterceptor.active = false;
      TestInterceptor.resetMessages();
    }
  }

  /**
   * Test that the interceptor works with a SocketChannel / ServerSocket.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testSocketChannelClientAndSocketServer() throws Exception {
    SocketServer server = null;
    SocketChannel client = null;
    try {
      final List<NetworkConnectionInterceptor> interceptors = NetworkConnectionInterceptor.INTERCEPTORS;
      print(false, false, "interceptor list: %s", interceptors);
      TestInterceptor.active = true;
      server = new SocketServer();
      ThreadUtils.startThread(server, "socket server thread");
      Thread.sleep(250L);
      client = SocketChannel.open(new InetSocketAddress("localhost", PORT));
      client.finishConnect();
      assertTrue(client.isConnected());
      client.configureBlocking(true);
      assertTrue(InterceptorHandler.invokeOnConnect(client, JPPFChannelDescriptor.UNKNOWN));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      close(client, server);
      TestInterceptor.active = false;
      TestInterceptor.resetMessages();
    }
  }

  /**
   * Test that the interceptor works with a Socket / ServerSocketChannel.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testSocketClientAndSocketChannelServer() throws Exception {
    ChannelServer server = null;
    Socket client = null;
    try {
      final List<NetworkConnectionInterceptor> interceptors = NetworkConnectionInterceptor.INTERCEPTORS;
      print(false, false, "interceptor list: %s", interceptors);
      TestInterceptor.active = true;
      server = new ChannelServer();
      ThreadUtils.startThread(server, "socket channel server thread");
      Thread.sleep(250L);
      client = new Socket("localhost", PORT);
      assertTrue(client.isConnected());
      assertTrue(InterceptorHandler.invokeOnConnect(client, JPPFChannelDescriptor.UNKNOWN));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      close(client, server);
      TestInterceptor.active = false;
      TestInterceptor.resetMessages();
    }
  }

  private static void close(final AutoCloseable...closeables) {
    if (closeables == null) return;
    for (final AutoCloseable c: closeables) {
      if (c != null) {
        try {
          c.close();
        } catch (final Exception e) {
          print(false, false, "error closing %s: %s", c, ExceptionUtils.getStackTrace(e));
        }
      }
    }
  }

  /** */
  private static class SocketServer implements Runnable, AutoCloseable {
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(SocketServer.class.getSimpleName());
    /**
     * The result of calling the interceptor on the accepted socket.
     */
    private boolean result;

    /** */
    public ServerSocket server;
    /** */
    private boolean firstException = true;

    @Override
    public void run() {
      logger.debug("start");
      try {
        ConcurrentUtils.awaitCondition(5000L, 100L, true, (ConditionFalseOnException) () -> {
          try {
            server = new ServerSocket(PORT);
            return true;
          } catch(final Exception e) {
            if (firstException) {
              firstException = false;
              print(false, false, "first exception starting socket server: %s", ExceptionUtils.getStackTrace(e));
            }
          }
          return false;
        });
        logger.debug("server bound to port {}", server.getLocalPort());
        final Socket socket = server.accept();
        logger.debug("accepted socket {}", socket);
        synchronized(this) {
          result = InterceptorHandler.invokeOnAccept(socket, JPPFChannelDescriptor.UNKNOWN);
        }
        logger.debug("result = {}", result);
      } catch (final Exception e) {
        print(false, false, "error starting socket server: %s", ExceptionUtils.getStackTrace(e));
      }
    }

    @Override
    public void close() {
      try {
        if (server != null) server.close();        
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * @return the result.
     */
    public synchronized boolean isResult() {
      return result;
    }
  }

  /** */
  private static class ChannelServer implements Runnable, AutoCloseable {
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(ChannelServer.class.getSimpleName());
    /**
     * The result of calling the interceptor on the accepted socket channel.
     */
    private boolean result;
    /** */
    public ServerSocketChannel server;
    /** */
    private boolean firstException = true;

    @Override
    public void run() {
      try {
        logger.debug("start");
        server = ServerSocketChannel.open();
        final InetSocketAddress addr = new InetSocketAddress(PORT);
        ConcurrentUtils.awaitCondition(5000L, 100L, true, (ConditionFalseOnException) () -> {
          try {
            server.bind(addr);
            return true;
          } catch(final Exception e) {
            if (firstException) {
              firstException = false;
              print(false, false, "first exception starting socket channel server: %s", ExceptionUtils.getStackTrace(e));
            }
          }
          return false;
        });
        logger.debug("server bound to port {}", ((InetSocketAddress) server.getLocalAddress()).getPort());
        server.configureBlocking(true);
        final SocketChannel channel = server.accept();
        logger.debug("accepted channel {}", channel);
        channel.configureBlocking(true);
        synchronized(this) {
          result = InterceptorHandler.invokeOnAccept(channel, JPPFChannelDescriptor.UNKNOWN);
        }
        logger.debug("result = {}", result);
      } catch (final Exception e) {
        print(false, false, "error starting socket channel server: %s", ExceptionUtils.getStackTrace(e));
      }
    }

    @Override
    public void close() {
      try {
        if (server != null) server.close();        
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * @return the result.
     */
    public synchronized boolean isResult() {
      return result;
    }
  }
}
