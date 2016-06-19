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

import org.jppf.comm.interceptor.InterceptorHandler;
import org.junit.Test;

import test.org.jppf.test.setup.common.TestInterceptor;

/**
 * Test the network interceptor feature.
 * @author Laurent Cohen
 */
public class TestNetworkCommunicationInterceptor {
  /**
   * 
   */
  private static final int PORT = 4444;

  /**
   * Test that the interceptor works with a Socket / ServerSocket.
   * @throws Exception if any error occurs
   */
  @Test(timeout = 10000)
  public void testSocketClientAndServer() throws Exception {
    SocketServer server = null;
    Socket client = null;
    try {
      TestInterceptor.active = true;
      server = new SocketServer();
      Thread serverThread = new Thread(server, "socket server thread");
      serverThread.start();
      Thread.sleep(250L);
      client = new Socket("localhost", PORT);
      assertTrue(client.isConnected());
      assertTrue(InterceptorHandler.invokeOnConnect(client));
      assertTrue(server.result);
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (Exception ignore) {
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
  @Test(timeout = 10000)
  public void testSocketChannelClientAndServer() throws Exception {
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
      assertTrue(server.result);
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (Exception ignore) {
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
  @Test(timeout = 10000)
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
      assertTrue(server.result);
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (Exception ignore) {
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
  @Test(timeout = 10000)
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
      assertTrue(server.result);
      assertEquals(TestInterceptor.CLIENT_MESSAGE, TestInterceptor.clientMessage);
      assertEquals(TestInterceptor.SERVER_MESSAGE, TestInterceptor.serverMessage);
    } finally {
      if (client != null) {
        try {
          client.close();
        } catch (Exception ignore) {
        }
      }
      if (server != null) {
        try {
          server.close();
        } catch (Exception ignore) {
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
     * The result of calling the interceptor on the accepted socket.
     */
    public boolean result;
    /**
     * 
     */
    public ServerSocket server;

    @Override
    public void run() {
      try {
        server = new ServerSocket(PORT);
        Socket socket = server.accept();
        result = InterceptorHandler.invokeOnAccept(socket);
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
      } catch (Exception ignore) {
      }
    }
  }

  /**
   * 
   */
  private static class ChannelServer implements Runnable {
    /**
     * The result of calling the interceptor on the accepted socket channel.
     */
    public boolean result;
    /**
     * 
     */
    public ServerSocketChannel server;

    @Override
    public void run() {
      try {
        server = ServerSocketChannel.open();
        InetSocketAddress addr = new InetSocketAddress(PORT);
        server.bind(addr);
        server.configureBlocking(true);
        SocketChannel channel = server.accept();
        channel.configureBlocking(true);
        result = InterceptorHandler.invokeOnAccept(channel);
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
      } catch (Exception ignore) {
      }
    }
  }
}
