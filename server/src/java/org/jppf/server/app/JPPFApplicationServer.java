/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	 http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jppf.server.app;

import java.net.Socket;
import org.jppf.JPPFException;
import org.jppf.server.*;

/**
 * Instances of this class listens for incoming connections from client applications.<br>
 * For each incoming connection, a new connection thread is created. This thread listens to incoming
 * connection requests and puts them on the execution queue.
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFApplicationServer extends JPPFServer
{
	/**
	 * Initialize this socket server with a specified execution service and port number.
	 * @param port the port this socket server is listening to.
	 * @throws JPPFException if the underlying server socket can't be opened.
	 */
	public JPPFApplicationServer(int port) throws JPPFException
	{
		super(port,"Application Server Thread");
	}
	
	/**
	 * Instanciate a wrapper for the socket connection opened by this socket server.
	 * Subclasses must implement this method.
	 * @param socket the socket connection obtained through a call to
	 * {@link java.net.ServerSocket#accept() ServerSocket.accept()}.
	 * @return a <code>JPPFServerConnection</code> instance.
	 * @throws JPPFException if an exception is raised while creating the socket handler.
	 */
	protected JPPFConnection createConnection(Socket socket) throws JPPFException
	{
		return new ApplicationConnection(this, socket);
	}
}
