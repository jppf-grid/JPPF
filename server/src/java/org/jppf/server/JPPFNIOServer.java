/*
 * Java Parallel Processing Framework.
 * Copyright (C) 2005-2006 Laurent Cohen.
 * lcohen@osp-chicago.com
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation; either version 2.1 of the License, or (at your
 * option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jppf.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jppf.classloader.ResourceProvider;
import org.jppf.node.JPPFBootstrapException;

/**
 * This is a abstract class that provide a base architecture
 * for non blocking socket server.
 * 
 * @author Domingos Creado
 */
public abstract class JPPFNIOServer extends Thread{
	
	/**
	 * Log4j logger for this class.
	 */
	protected static Logger log = Logger.getLogger(JPPFNIOServer.class);
	
	/**
	 * the selector of all socket channels open with providers or nodes.
	 */
	protected Selector selector;
	
	/**
	 * Reads resource files from the classpath.
	 */
	protected ResourceProvider resourceProvider = new ResourceProvider();
	
	/**
	 * Flag indicating that this socket server is closed.
	 */
	protected boolean stop = false;
	
	/**
	 * The port this socket server is listening to.
	 */
	protected int port = -1;
	
	/**
	 * Initialize this socket server with a specified port number.
	 * @param port the port this socket server is listening to.
	 * @param name the name given to this thread.
	 * @throws JPPFBootstrapException if the underlying server socket can't be opened.
	 */
	public JPPFNIOServer(int port,String name) throws JPPFBootstrapException
	{
		super(name);
		this.port = port;
		init(port);
	}
	
	/**
	 * Start the underlying server socket by making it accept incoming connections.
	 * @see java.lang.Runnable#run()
	 */
	public void run()
	{
		try
		{
			while (!stop && !JPPFDriver.getInstance().isShuttingDown())
			{
				int n = selector.select();

				if (n == 0) {
					continue;
				}
				
				go(selector.selectedKeys());
			}
			end();
		}
		catch (Throwable t)
		{
			log.error(t.getMessage(), t);
			end();
		}
	}

	/**
	 * Process the keys selected by the selector for IO operations.
	 * @param selectedKeys the set of keys thast were selected by the latest <code>select()</code> invocation.
	 * @throws Exception if an error is raised while processing the keys.
	 */
	public void go(Set<SelectionKey> selectedKeys) throws Exception
	{
		Iterator<SelectionKey> it = selectedKeys.iterator();
		while (it.hasNext())
		{
			SelectionKey key = it.next();
			try
			{
				//if ((key.readyOps() & SelectionKey.OP_ACCEPT) == SelectionKey.OP_ACCEPT)
				if (key.isAcceptable())
				{
					doAccept(key);
				}
				else
				{
					Context context = (Context) key.attachment();
					context.state.exec(key, context);
				}
			}
			catch (Exception e)
			{
				log.error(e.getMessage(), e);
				if (!(key.channel() instanceof ServerSocketChannel))
				{
					try
					{
						key.channel().close();
					}
					catch (Exception e2)
					{
						log.error(e2.getMessage(), e2);
					}
				}
			}
			it.remove();
		}
	}
	
	/**
	 * accept the incoming connection.
	 * It accept and put it in a state to define what type of peer is.
	 * @param key the selection key that represents the channel's registration witht the selector.
	 */
	private void doAccept(SelectionKey key)
	{
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();
		SocketChannel client;
		try
		{
			client = serverSocketChannel.accept();
		}
		catch (IOException ignored)
		{
			log.error(ignored.getMessage(), ignored);
			return;
		}
		if (client == null) return;
		try
		{
			client.configureBlocking(false);
		}
		catch (IOException e)
		{
			log.error(e.getMessage(), e);
			try
			{
				client.close();
			}
			catch (IOException ignored)
			{
				log.error(ignored.getMessage(), ignored);
			}
			return;
		}
		Context context = new Context();
		context.state = getInitialState();
		context.content = getInitialContent();
		try
		{
			client.register(selector,
					SelectionKey.OP_READ | SelectionKey.OP_WRITE, context)
					.interestOps(getInitialInterest());
		}
		catch (ClosedChannelException ignored)
		{
			log.error(ignored.getMessage(), ignored);
		}
		postAccept(client);
	}

	/**
	 * Get the IO operations a connection is initially intertested in.
	 * @return a bit-wise combination of the interests, taken from {@link java.nio.channels.SelectionKey SelectionKey}
	 * constants definitions.
	 */
	protected abstract int getInitialInterest() ;
	
	/**
	 * 
	 * @return
	 */
	protected abstract Object getInitialContent() ;

	protected abstract State getInitialState();

	/**
	 * Process a channel that was accepted by the server socket channel.
	 * @param client the socket channel to process.
	 */
	protected abstract void postAccept(SocketChannel client);

	/**
	 * just to not instatiate every invocation of method fillRequest
	 */
	private byte[] buf = new byte[1024];

	/**
	 * This method read everything it can from the channel until the request is
	 * fulfill. The request must be a transfer of object like JPPFBuffer, with
	 * the first 4 bytes as the size of the payload and the payload.
	 * 
	 * @param channel
	 *            from where data will be read
	 * @param request
	 *            the request been received
	 * @return if the request was completed received.
	 * @throws IOException
	 */
	protected boolean fillRequest(SocketChannel channel, Request request)
			throws IOException {

		ByteBuffer buffer;

		if (request.getSize() == 0) {

			buffer = request.getBuffer();

			if (buffer == null) {
				// determining the size of the packet
				buffer = ByteBuffer.allocate(4);
				request.setBuffer(buffer);
			}

			if(channel.read(buffer) < 0){
				throw new ClosedChannelException();
			}
			if (buffer.remaining() != 0) {
				// the first 4 bytes was not received yet
				return false;
			}
			// turning buffer to be read
			buffer.flip();
			request.setSize(buffer.getInt());

			// allocating a buffer with the size of the payload
			request.setBuffer(ByteBuffer.allocateDirect(request.getSize()));
		}

		buffer = request.getBuffer();

		// read every it can
		int readed = channel.read(buffer);
		while (readed > 0){
			readed = channel.read(buffer);
		}
		if(readed < 0 ){
			throw new ClosedChannelException();
		}
			

		if (buffer.remaining() != 0) {
			return false;
		}
		// the payload has been fully received
		buffer.flip();
		while (buffer.hasRemaining()) {
			int size = (buf.length < buffer.remaining() ? buf.length : buffer
					.remaining());
			buffer.get(buf, 0, size);
			request.getOutput().write(buf, 0, size);
		}

		buffer.clear();
		request.setBuffer(null);
		return true;
	}
	
	/**
	 * Close and remove all connections accepted by this server.
	 */
	public synchronized void removeAllConnections()
	{
		if (!stop) return;
		
		for (SelectionKey connection: selector.keys())
		{
			try {
				connection.channel().close();
			} catch (IOException ignored) {
				log.error(ignored.getMessage(), ignored);
			}
		}
	}
	
	/**
	 * Initialize the underlying server socket with a specified port.
	 * @param port the port the underlying server listens to.
	 * @throws JPPFBootstrapException if the server socket can't be opened on the specified port.
	 */
	protected void init(int port) throws JPPFBootstrapException
	{
		Exception e = null;
		try
		{
			ServerSocketChannel server = ServerSocketChannel.open();
			int size = 32*1024;
			server.socket().setReceiveBufferSize(size);
			server.socket().bind(new InetSocketAddress(port));
			server.configureBlocking(false);
			selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch(IllegalArgumentException iae)
		{
			e = iae;
		}
		catch(IOException ioe)
		{
			e = ioe;
		}
		if (e != null)
		{
			throw new JPPFBootstrapException(e.getMessage(), e);
		}
	}
	
	/**
	 * Close the underlying server socket and stop this socket server.
	 */
	public synchronized void end()
	{
		if (!stop)
		{
			stop = true;
			removeAllConnections();
		}
	}

  //==========================================================
	// classes that creates a basic "framework" to deal with nio
	//==========================================================
	
	/**
	 * Class that represents a context to a channel.
	 * 
	 */
	public class Context {
		/**
		 * what will be executed when the channel is selected
		 */
		public State state;

		/**
		 * the "memory" of the DFA
		 */
		public Object content;

		/**
		 * the uuid of the application, it does not make sense for channels to
		 * nodes
		 */
		public String uuid;
	}

	public interface State {
		void exec(SelectionKey key, Context context) throws IOException;
	}

	/**
	 * Represent a request to be or been received.
	 * It follow the same strategy of JPPFBuffer, but this is designed to run 
	 * with nonblocking io.
	 */
	public class Request {
		
		private long start = System.currentTimeMillis();
		private int size;
		private ByteBuffer buffer;
		private ByteArrayOutputStream output = new ByteArrayOutputStream();

		public long getStart(){
			return start;
		}
		
		public ByteArrayOutputStream getOutput() {
			return output;
		}

		public ByteBuffer getBuffer() {
			return buffer;
		}

		public void setBuffer(ByteBuffer buffer) {
			this.buffer = buffer;
		}

		public int getSize() {
			return size;
		}

		public void setSize(int size) {
			this.size = size;
		}
	}
	
	
}
