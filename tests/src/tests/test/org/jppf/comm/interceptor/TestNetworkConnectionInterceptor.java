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
      List<NetworkConnectionInterceptor> interceptors = NetworkConnectionInterceptor.INTERCEPTORS;
      assertNotNull(interceptors);
      assertEquals(1, interceptors.size());
      NetworkConnectionInterceptor interceptor = interceptors.get(0);
      assertTrue(interceptor instanceof TestInterceptor);
      TestInterceptor.active = true;
      server = new SocketServer();
      Thread serverThread = new Thread(server, "socket server thread");
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
        } catch (@SuppressWarnings("unused") Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") Exception ignore) {
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
      Thread serverThread = new Thread(server, "socket channel server thread");
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
        } catch (@SuppressWarnings("unused") Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") Exception ignore) {
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
      Thread serverThread = new Thread(server, "socket server thread");
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
        } catch (@SuppressWarnings("unused") Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") Exception ignore) {
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
      Thread serverThread = new Thread(server, "socket channel server thread");
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
        } catch (@SuppressWarnings("unused") Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (@SuppressWarnings("unused") Exception ignore) {
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
    private static Logger log = LoggerFactory.getLogger(SocketServer.class.getSimpleName());
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
      log.debug("start");
      try {
        server = new ServerSocket(PORT);
        log.debug("server bound to port {}", server.getLocalPort());
        Socket socket = server.accept();
        log.debug("accepted socket {}", socket);
        synchronized(this) {
          result = InterceptorHandler.invokeOnAccept(socket);
        }
        log.debug("result = {}", result);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * CLose the socket server.
     */
    public void close() {
      try {
        if (server != null) server.close();        
      } catch (Exception e) {
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
    private static Logger log = LoggerFactory.getLogger(ChannelServer.class.getSimpleName());
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
        log.debug("start");
        server = ServerSocketChannel.open();
        InetSocketAddress addr = new InetSocketAddress(PORT);
        server.bind(addr);
        log.debug("server bound to port {}", ((InetSocketAddress) server.getLocalAddress()).getPort());
        server.configureBlocking(true);
        SocketChannel channel = server.accept();
        log.debug("accepted channel {}", channel);
        channel.configureBlocking(true);
        synchronized(this) {
          result = InterceptorHandler.invokeOnAccept(channel);
        }
        log.debug("result = {}", result);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    /**
     * CLose the socket server.
     */
    public void close() {
      try {
        if (server != null) server.close();        
      } catch (Exception e) {
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
