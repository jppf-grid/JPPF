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

package test.org.jppf.comm.interceptor;

import static org.junit.Assert.*;

import java.net.*;
import java.nio.channels.*;
import java.util.List;

import org.jppf.comm.interceptor.*;
import org.jppf.utils.ReflectionUtils;
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
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(TestNetworkConnectionInterceptor.class);
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
    log.debug("********** {}() **********", ReflectionUtils.getCurrentMethodName());
    SocketServer server = null;
    Socket client = null;
    try {
      final List<NetworkConnectionInterceptor> interceptors = NetworkConnectionInterceptor.INTERCEPTORS;
      assertNotNull(interceptors);
      assertEquals(1, interceptors.size());
      final NetworkConnectionInterceptor interceptor = interceptors.get(0);
      assertTrue(interceptor instanceof TestInterceptor);
      TestInterceptor.active = true;
      server = new SocketServer();
      final Thread serverThread = new Thread(server, "socket server thread");
      serverThread.start();
      Thread.sleep(250L);
      client = new Socket("localhost", PORT);
      assertTrue(client.isConnected());
      assertTrue(InterceptorHandler.invokeOnConnect(client));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
      TestInterceptor.active = false;
      TestInterceptor.resetMessages();
    }
  }

  /**
   * Test that the interceptor works with a SocketChannel / ServerSocketChannel.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 5000)
  public void testSocketChannelClientAndServer() throws Exception {
    log.debug("********** {}() **********", ReflectionUtils.getCurrentMethodName());
    ChannelServer server = null;
    SocketChannel client = null;
    try {
      TestInterceptor.active = true;
      server = new ChannelServer();
      final Thread serverThread = new Thread(server, "socket channel server thread");
      serverThread.start();
      Thread.sleep(250L);
      client = SocketChannel.open(new InetSocketAddress("localhost", PORT));
      client.finishConnect();
      assertTrue(client.isConnected());
      client.configureBlocking(true);
      assertTrue(InterceptorHandler.invokeOnConnect(client));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
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
      TestInterceptor.active = true;
      server = new SocketServer();
      final Thread serverThread = new Thread(server, "socket server thread");
      serverThread.start();
      Thread.sleep(250L);
      client = SocketChannel.open(new InetSocketAddress("localhost", PORT));
      client.finishConnect();
      assertTrue(client.isConnected());
      client.configureBlocking(true);
      assertTrue(InterceptorHandler.invokeOnConnect(client));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
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
      TestInterceptor.active = true;
      server = new ChannelServer();
      final Thread serverThread = new Thread(server, "socket channel server thread");
      serverThread.start();
      Thread.sleep(250L);
      client = new Socket("localhost", PORT);
      assertTrue(client.isConnected());
      assertTrue(InterceptorHandler.invokeOnConnect(client));
      assertTrue(server.isResult());
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") final Exception ignore) {
        }
      }
      TestInterceptor.active = false;
      TestInterceptor.resetMessages();
    }
  }

  /**
   * 
   */
  private static class SocketServer implements Runnable {
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(SocketServer.class.getSimpleName());
    /**
     * The result of calling the interceptor on the accepted socket.
     */
    private boolean result;

    /**
     * 
     */
    public ServerSocket server;

    @Override
    public void run() {
      logger.debug("start");
      try {
        server = new ServerSocket(PORT);
        logger.debug("server bound to port {}", server.getLocalPort());
        final Socket socket = server.accept();
        logger.debug("accepted socket {}", socket);
        synchronized(this) {
          result = InterceptorHandler.invokeOnAccept(socket);
        }
        logger.debug("result = {}", result);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * CLose the socket server.
     */
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

  /**
   * 
   */
  private static class ChannelServer implements Runnable {
    /**
     * Logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(ChannelServer.class.getSimpleName());
    /**
     * The result of calling the interceptor on the accepted socket channel.
     */
    private boolean result;
    /**
     * 
     */
    public ServerSocketChannel server;

    @Override
    public void run() {
      try {
        logger.debug("start");
        server = ServerSocketChannel.open();
        final InetSocketAddress addr = new InetSocketAddress(PORT);
        server.bind(addr);
        logger.debug("server bound to port {}", ((InetSocketAddress) server.getLocalAddress()).getPort());
        server.configureBlocking(true);
        final SocketChannel channel = server.accept();
        logger.debug("accepted channel {}", channel);
        channel.configureBlocking(true);
        synchronized(this) {
          result = InterceptorHandler.invokeOnAccept(channel);
        }
        logger.debug("result = {}", result);
      } catch (final Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * CLose the socket server.
     */
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
