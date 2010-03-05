/*
 * JPPF.
 * Copyright (C) 2005-2009 JPPF Team.
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

package org.jppf.server.mina;

import java.util.Map;
import java.util.concurrent.*;

import org.apache.commons.logging.*;
import org.apache.mina.core.service.*;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.jppf.server.nio.NioTransition;
import org.jppf.utils.JPPFThreadFactory;

/**
 * 
 * @param <S> the type of the states to use.
 * @param <T> the type of the transitions to use.
 * @author Laurent Cohen
 */
public abstract class MinaServer<S extends Enum<S>, T extends Enum<T>>
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(MinaServer.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The ports to listen to.
	 */
	protected int[] ports = null;
	/**
	 * Acceptor for this server.
	 */
	protected NioSocketAcceptor acceptor = null;
	/**
	 * The IoProcessor for the acceptor.
	 */
	protected IoProcessor processor = null;
	/**
	 * The factory for this server.
	 */
	protected MinaServerFactory<S, T> factory = createFactory();

	/**
	 * Initialiaze this server with the specified port number.
	 * @param port the port to listen to.
	 */
	public MinaServer(int port)
	{
		this(new int[] { port });
	}

	/**
	 * Initialiaze this server with the specified port numbers.
	 * @param ports the ports to listen to.
	 */
	public MinaServer(int[] ports)
	{
		this.ports = ports;
	}

	/**
	 * Initialize the server.
	 * @throws Exception if any error occurs.
	 */
	public abstract void start() throws Exception;

	/**
	 * Close this node server.
	 */
	public void close()
	{
		Map<Long, IoSession> sessions = acceptor.getManagedSessions();
		for (Map.Entry<Long, IoSession> entry: sessions.entrySet()) entry.getValue().close(true);
		acceptor.dispose();
	}

	/**
	 * Transition the specified session to the specified transition.
	 * @param session the session to transition.
	 * @param transition contains the new state and interest ops.
	 */
	public void transitionSession(IoSession session, T transition)
	{
		if (!session.isConnected()) return;
		NioTransition<S> tr = factory.getTransition(transition);
		//log.debug("transitioning session " + session.getId() + " : transition name = " + transition + ", transition object = " + tr);
		S s = tr.getState();
		MinaContext context = (MinaContext) session.getAttribute(MinaContext.CONTEXT);
		if (debugEnabled)
		{
			String[] ops = new String[] { "NONE", "R", "W", "RW" };
			log.debug("transitioning session " + session.getId() + " from " + context.getState() + " to " + s + ", new ops = " + ops[tr.getInterestOps()]);
		}
		context.setState(s);
		switch(tr.getInterestOps())
		{
			case MinaServerFactory.NONE:
				session.suspendRead();
				session.suspendWrite();
				return;
			case MinaServerFactory.R:
				session.resumeRead();
				session.suspendWrite();
				break;
			case MinaServerFactory.W:
				session.suspendRead();
				session.resumeWrite();
				break;
			case MinaServerFactory.RW:
				session.resumeRead();
				session.resumeWrite();
				break;
		}
	}

	/**
	 * Get the IoProcessor for the acceptor.
	 * @return a <code>IoProcessor</code> instance.
	 */
	public IoProcessor getProcessor()
	{
		return processor;
	}

	/**
	 * Create the factory holding all the states and transition mappings.
	 * @return an <code>MinaServerFactory</code> instance.
	 */
	protected abstract MinaServerFactory<S, T> createFactory();

	/**
	 * Get the factory for this server.
	 * @return a <code>MinaServerFactory</code> instance.
	 */
	public MinaServerFactory<S, T> getFactory()
	{
		return factory;
	}

	/**
	 * Create an executor supplied to the io processor and io acceptor.
	 * @param baseName the suffix used for the threads naming.
	 * @param size the max size of the thread pool.
	 * @return an <code>ExecutorService</code> instance.
	 */
	protected ExecutorService createExecutor(String baseName, int size)
	{
		ThreadFactory fact = new JPPFThreadFactory(baseName);
		return Executors.newFixedThreadPool(size, fact);
	}
}
