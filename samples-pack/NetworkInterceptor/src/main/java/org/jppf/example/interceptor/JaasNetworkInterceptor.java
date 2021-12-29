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

package org.jppf.example.interceptor;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;

import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;

import org.jppf.comm.interceptor.AbstractNetworkConnectionInterceptor;
import org.jppf.example.interceptor.auth.InterceptorCallbackHandler;
import org.jppf.example.interceptor.auth.JPPFJaasConfiguration;
import org.jppf.utils.JPPFChannelDescriptor;

/**
 * This interceptor implementation enforces a simple authentication mechanism.
 * @author Laurent Cohen
 */
public class JaasNetworkInterceptor extends AbstractNetworkConnectionInterceptor {
  /**
   * The name of the system property that holds the user name to validate against.
   */
  private static final String USER_NAME_PROPERTY = "jppf.user.name";
  /**
   * Timeout for socket read() opearations.
   */
  private static final int SOCKET_TIMEOUT = 6000;
  /*
   * Set the default Jaas configuration to ours, so that we don"'t have to use an external jaas.conf file.
  */
  static {
    print("Initializing Jaas configuration");
    Configuration.setConfiguration(new JPPFJaasConfiguration());
  }

  /**
   * Initialize this interceptor.
   */
  public JaasNetworkInterceptor() {
    print("Initializing the Jaas network interceptor");
  }

  /**
   * Perform the interceptor's job on the specified accepted {@code Socket} or {@code SocketChannel} streams.
   * <p>Here we are on the server side of a connection so we check that the credentials sent by the client are
   * valid and send a response accordingly.
   * @param acceptedSocket the channel to read from and write to.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  @Override
  public boolean onAccept(final Socket acceptedSocket, final JPPFChannelDescriptor descriptor) {
    int prevTimeout = -1;
    try {
      // set a timeout on read operations and store the previous setting, if any
      prevTimeout = acceptedSocket.getSoTimeout();
      acceptedSocket.setSoTimeout(SOCKET_TIMEOUT);

      final InputStream is = acceptedSocket.getInputStream();
      final OutputStream os = acceptedSocket.getOutputStream();
      final String userName = CryptoHelper.readAndDecrypt(is);
      final String localUser = System.getProperty(USER_NAME_PROPERTY);
      if (!userName.equals(localUser)) {
        print("invalid user name '%s' from client side, source is %s", userName, acceptedSocket);
        // send invalid user response
        CryptoHelper.encryptAndWrite("invalid user name", os);
      } else {
        // send ok response
        CryptoHelper.encryptAndWrite("OK", os);
        print("successful server authentication");
        return true;
      }
    } catch (@SuppressWarnings("unused") final SocketTimeoutException e) {
      print("unable to get a response from the client after %,d ms", SOCKET_TIMEOUT);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (prevTimeout >= 0) {
        try {
          // restore the initial SO_TIMEOUT setting
          acceptedSocket.setSoTimeout(prevTimeout);
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
    return false;
  }

  /**
   * Perform the interceptor's job on the specified connected {@code Socket} or {@code SocketChannel} streams.
   * <p>Here we are on the client side of a connection, so we send credentials and obtain the confirmation
   * from the server side that they are valid.
   * @param connectedSocket the channel to read from and write to.
   * @param descriptor provides information on the connected socket.
   * @return {@code true} to accept the connection {@code false} to deny it.
   */
  @Override
  public boolean onConnect(final Socket connectedSocket, final JPPFChannelDescriptor descriptor) {
    try {
      // perform the authentication through JAAS 
      final LoginContext ctx = new LoginContext("NetworkInterceptorDemo", new InterceptorCallbackHandler(connectedSocket));
      ctx.login();
      return true;
    } catch (final Exception e) {
      e.printStackTrace();
    }
    // the client side process terminates if authentication fails
    print("authentication failed, terminating");
    System.exit(1);
    return false;
  }

  /**
   * Print the specified formatted message.
   * @param format the format string.
   * @param params the parameters referenced in the format.
   */
  public static void print(final String format, final Object...params) {
    final String message = String.format("[demo] " + format, params);
    System.out.println(message);
  }
}
