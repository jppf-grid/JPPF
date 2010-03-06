/*
 * JPPF.
 * Copyright (C) 2005-2010 JPPF Team.
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

package org.jppf.comm.socket.mina;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;

import org.apache.commons.logging.*;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.*;
import org.jppf.JPPFException;

/**
 * 
 * @author Laurent Cohen
 */
public class SocketConnectorIoHandler extends IoHandlerAdapter
{
	/**
	 * Logger for this class.
	 */
	private static Log log = LogFactory.getLog(SocketConnectorIoHandler.class);
	/**
	 * Determines whether DEBUG logging level is enabled.
	 */
	private static boolean debugEnabled = log.isDebugEnabled();
	/**
	 * The current session with the server.
	 */
	private IoSession session = null;
	/**
	 * Stores the data buffers read fro, the connection.
	 */
	private Queue<IoBuffer> readBufferQueue = new ConcurrentLinkedQueue<IoBuffer>();
	/**
	 * Unprocessed read data, for which the buffer was removed from the queue.
	 */
	private IoBuffer remainder = null;
	/**
	 * Read lock for synchronization on read operations.
	 */
	private Object readLock = new Object();
	/**
	 * Write lock for synchronization on write operations.
	 */
	private Object writeLock = new Object();
	/**
	 * An exception that may eventually occur while performing asynchronous I/O.
	 */
	private AtomicReference<Exception> exception = new AtomicReference<Exception>(null);
	/**
	 * 
	 */
	private AtomicBoolean writeComplete = new AtomicBoolean(false);
	/**
	 * Cached read message, used for performance improvements.
	 */
	private ByteMessage readMessage = new ByteMessage();

	/**
	 * Initialize this io handler.
	 */
	public SocketConnectorIoHandler()
	{
	}

	/**
	 * Read the next message from the server.
	 * @param data an array of bytes into which the data is stored.
	 * @param offset the position where to start storing data read from the socket.
	 * @param len the length of data to read.
	 * @throws Exception if any error occurs while processing the message.
	 */
	public void readMessage(byte[] data, int offset, int len) throws Exception
	{
		if (debugEnabled) log.debug("len = " + len);
		//if (session.isReadSuspended()) session.resumeRead();
		/*
		ByteMessage readMessage = new ByteMessage();
		readMessage.buffer = ByteBuffer.wrap(data, offset, len);
		readMessage.length = len;
		*/
		readMessage.reset(data, offset, len);
		while (!readMessage.complete && (exception.get() == null))
		{
			IoBuffer buffer = null;
			synchronized(readLock)
			{
				buffer = (remainder != null) ? remainder : readBufferQueue.poll();
				if (buffer == null)
				{
					readLock.wait();
					continue;
				}
			}
			if (readMessage.read(buffer))
			{
				remainder = buffer.hasRemaining() ? buffer : null;
			}
			if ((remainder != null) && !remainder.hasRemaining()) remainder = null;
			if (debugEnabled) log.debug("message buffer = " + readMessage.buffer);
		}
		if (debugEnabled) log.debug("message buffer = " + readMessage.buffer);
		if (exception.get() != null) throw exception.get();
		//session.suspendRead();
	}

	/**
	 * Write the specified message.
	 * @param data an array of bytes containing the data to write.
	 * @param offset the position where to start reading data to write to the socket.
	 * @param len the length of data to write.
	 * @throws Exception if any error occurs.
	 */
	public void writeMessage(byte[] data, int offset, int len) throws Exception
	{
		//if (session.isWriteSuspended()) session.resumeWrite();
		writeComplete.set(false);
		IoBuffer buffer = IoBuffer.wrap(data, offset, len);
		if (debugEnabled) log.debug("about to write buffer: " + buffer);
		synchronized(writeLock)
		{
			WriteFuture future = session.write(buffer);
			if (!writeComplete.get()) writeLock.wait();
		}
		if (debugEnabled) log.debug("len = " + len + ", count = " + buffer.position() + ", buffer = " + buffer);
		if (exception.get() != null) throw exception.get();
		//session.suspendWrite();
	}

	/**
	 * Invoked when a message has been received.
	 * @param session the session to which the message applies.
	 * @param message the message that was received.
	 * @throws Exception if any error occurs while processing the message.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#messageReceived(org.apache.mina.core.session.IoSession, java.lang.Object)
	 */
	public void messageReceived(IoSession session, Object message) throws Exception
	{
		if (debugEnabled) log.debug("adding buffer to the queue: " + message);
		synchronized(readLock)
		{
			readBufferQueue.offer((IoBuffer) message);
			readLock.notifyAll();
		}
	}

	/**
	 * Invoked when a message has been sent.
	 * @param session the session to which the message applies.
	 * @param message the message that was sent.
	 * @throws Exception if any error occurs while processing the message.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#messageSent(org.apache.mina.core.session.IoSession, java.lang.Object)
	 */
	public void messageSent(IoSession session, Object message) throws Exception
	{
		if (debugEnabled) log.debug("message sent: " + message);
		writeComplete.set(true);
		synchronized(writeLock)
		{
			writeLock.notifyAll();
		}
	}

	/**
	 * Invoked when a session has been created and connected to a remote peer.
	 * @param session the created session.
	 * @throws Exception if any error occurs while processing the new session.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionOpened(org.apache.mina.core.session.IoSession)
	 */
	public void sessionOpened(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("session: " + session); 
		//if (!session.isWriteSuspended()) session.suspendWrite();
		//if (!session.isReadSuspended()) session.suspendRead();
		if (session.isWriteSuspended()) session.resumeWrite();
		if (session.isReadSuspended()) session.resumeRead();
	}

	/**
	 * Invoked when an exception is caught during IO processing.
	 * @param session the session for which the exception occurred.
	 * @param cause the cause exception.
	 * @throws Exception if any error occurs while processing the exception.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#exceptionCaught(org.apache.mina.core.session.IoSession, java.lang.Throwable)
	 */
	public void exceptionCaught(IoSession session, Throwable cause) throws Exception
	{
		exception.set((cause instanceof Exception) ? (Exception) cause : new JPPFException(cause));
		synchronized(readLock)
		{
			readLock.notifyAll();
		}
		synchronized(writeLock)
		{
			writeLock.notifyAll();
		}
		log.error("session " + session.getId() + " : " + cause.getMessage(), cause);
	}

	/**
	 * Called when a session is closed.
	 * @param session the session that was closed.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionClosed(org.apache.mina.core.session.IoSession)
	 */
	public void sessionClosed(IoSession session) throws Exception
	{
		if (debugEnabled) log.debug("session " + session.getId() + " closed");
	}

	/**
	 * Called when a session is created.
	 * @param session the session that was created.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionCreated(org.apache.mina.core.session.IoSession)
	 */
	public void sessionCreated(IoSession session) throws Exception
	{
		this.session = session;
		if (debugEnabled) log.debug("session " + session.getId() + " created");
	}

	/**
	 * Called when a session becomes idle.
	 * @param session the session that is idle.
	 * @param status the session's idle status.
	 * @throws Exception if any error occurs.
	 * @see org.apache.mina.core.service.IoHandlerAdapter#sessionIdle(org.apache.mina.core.session.IoSession, org.apache.mina.core.session.IdleStatus)
	 */
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception
	{
		if (debugEnabled) log.debug("session " + session.getId() + " idle, status = " + status);
	}

	/**
	 * Get the current session with the server.
	 * @return an <code>IoSession</code> instance.
	 */
	public IoSession getSession()
	{
		return session;
	}

	/**
	 * Set the current session with the server.
	 * @param session an <code>IoSession</code> instance.
	 */
	public void setSession(IoSession session)
	{
		this.session = session;
	}
}
