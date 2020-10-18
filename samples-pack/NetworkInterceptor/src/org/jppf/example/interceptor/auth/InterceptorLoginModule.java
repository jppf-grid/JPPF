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

package org.jppf.example.interceptor.auth;

import static org.jppf.example.interceptor.JaasNetworkInterceptor.print;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;

import org.jppf.example.interceptor.CryptoHelper;

/**
 * A Jaas login module invoked on the client side of a JPPPF connection through a specialized network connection interceptor.
 * @author Laurent Cohen
 */
public class InterceptorLoginModule implements LoginModule {
  /**
   * The name of the system property that holds the user name to validate against.
   */
  private static final String USER_NAME_PROPERTY = "jppf.user.name";
  /**
   * Timeout for socket read() opearations.
   */
  private static final int SOCKET_TIMEOUT = 6000;
  /**
   * The callback handler used for login.
   */
  private CallbackHandler callbackHandler;

  @Override
  public void initialize(final Subject subject, final CallbackHandler callbackHandler, final Map<String, ?> sharedState, final Map<String, ?> options) {
    this.callbackHandler = callbackHandler;
  }

  @Override
  public boolean login() throws LoginException {
    int prevTimeout = -1;
    Socket socket = null;
    try {
      final SocketCallback callback = new SocketCallback();
      callbackHandler.handle(new Callback[] { callback });
      socket = callback.getSocket();

      // set a timeout on read operations and store the previous setting, if any
      prevTimeout = socket.getSoTimeout();
      socket.setSoTimeout(SOCKET_TIMEOUT);

      final InputStream is = socket.getInputStream();
      final OutputStream os = socket.getOutputStream();
      // send the user name to the server
      CryptoHelper.encryptAndWrite(System.getProperty(USER_NAME_PROPERTY), os);
      // read the server reponse
      final String response = CryptoHelper.readAndDecrypt(is);
      if (!"OK".equals(response)) {
        print("bad response from server: %s", response);
        throw new LoginException("Authentication failed with remote message: " + response);
      } else {
        print("successful client authentication");
        return true;
      }
    } catch (@SuppressWarnings("unused") final SocketTimeoutException e) {
      print("unable to get a response from the server after %,d ms", SOCKET_TIMEOUT);
    } catch (final Exception e) {
      e.printStackTrace();
    } finally {
      if (prevTimeout >= 0) {
        try {
          // restore the initial SO_TIMEOUT setting
          socket.setSoTimeout(prevTimeout);
        } catch(final Exception e) {
          e.printStackTrace();
        }
      }
    }
    throw new LoginException("Authentication failed");
  }

  @Override
  public boolean commit() throws LoginException {
    return true;
  }

  @Override
  public boolean abort() throws LoginException {
    return true;
  }

  @Override
  public boolean logout() throws LoginException {
    return true;
  }
}
