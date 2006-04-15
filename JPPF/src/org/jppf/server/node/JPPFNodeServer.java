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
package org.jppf.server.node;

import static org.jppf.server.JPPFStatsUpdater.isStatsEnabled;
import static org.jppf.server.JPPFStatsUpdater.taskExecuted;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.jppf.node.JPPFBootstrapException;
import org.jppf.server.JPPFDriver;
import org.jppf.server.JPPFNIOServer;
import org.jppf.server.JPPFQueue;
import org.jppf.server.JPPFQueue.QueueListener;
import org.jppf.server.event.TaskCompletionListener;
import org.jppf.server.protocol.JPPFTaskBundle;
import org.jppf.utils.SerializationHelper;
import org.jppf.utils.SerializationHelperImpl;

/**
 * This is a node server. It deals with job dispatching and results retrieve.
 * It distribute equaly bundles beteween available jobs (considering that
 * node have the same processing time of jobs).
 * It probably will receive and algorithm to weight each node, or a scheduling
 * algorithm using the mean processing time. 
 * 
 * @author Laurent Cohen
 * @author Domingos Creado
 */
public class JPPFNodeServer extends JPPFNIOServer implements QueueListener {

	/**
	 * A reference to the driver's tasks queue.
	 */
	private JPPFQueue queue = null;

	private List<SocketChannel> availableNodes = new LinkedList<SocketChannel>();

	/**
	 * Initialize this socket server with a specified execution service and port
	 * number.
	 * 
	 * @param port
	 *            the port this socket server is listening to.
	 * @throws JPPFException
	 *             if the underlying server socket can't be opened.
	 */
	public JPPFNodeServer(int port) throws JPPFBootstrapException {
		super(port, "NodeServer Thread");

		getQueue().addListener(this);
	}

	/**
	 * Get a reference to the driver's tasks queue.
	 * 
	 * @return a <code>JPPFQueue</code> instance.
	 */
	private JPPFQueue getQueue() {
		if (queue == null)
			queue = JPPFDriver.getInstance().getTaskQueue();
		return queue;
	}

	@Override
	protected int getInitialInterest() {
		return 0;
	}

	@Override
	protected State getInitialState() {
		return SendingJob;
	}

	@Override
	protected Object getInitialContent() {
		return null;
	}

	@Override
	protected void postAccept(SocketChannel client) {
		JPPFTaskBundle bundle = getQueue().nextBundle();
		if (bundle != null) {
			SelectionKey key = client.keyFor(selector);
			Context context = (Context) key.attachment();
			try {
				sendTask(client, key, context, bundle);
			} catch (Exception e) {
				// to not enter in a loop
				queue.removeListener(this);
				bundle.setPriority(10);
				queue.addBundle(bundle);
				queue.addListener(this);
				try {
					client.close();
				} catch (IOException ignored) {
				}
			}
		} else {
			availableNodes.add(client);
		}
	}

	public void newTask(JPPFQueue queue) {
		if (availableNodes.isEmpty()) {
			return;
		}
		JPPFTaskBundle bundle = queue.nextBundle();
		if (bundle != null) {
			SocketChannel aNode = availableNodes.remove(0);
			SelectionKey key = aNode.keyFor(selector);
			Context context = (Context) key.attachment();
			try {
				sendTask(aNode, key, context, bundle);
				selector.wakeup();
			} catch (Exception e) {
				// to not enter in a loop
				queue.removeListener(this);
				queue.addBundle(bundle);
				queue.addListener(this);
				try {
					aNode.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	private State SendingJob = new CSendingJob();

	private State WaitingResult = new CWaitingResult();

	private class CSendingJob implements State {

		public void exec(SelectionKey key, Context context) throws IOException {
			SocketChannel channel = (SocketChannel) key.channel();
			if (context.content == null)
				return;
			ByteBuffer task = ((TaskRequest) context.content).getSending();

			channel.write(task);
			if (!task.hasRemaining()) {
				context.state = WaitingResult;
				key.interestOps(SelectionKey.OP_READ);
			}
		}
	}

	private class CWaitingResult implements State {

		public void exec(SelectionKey key, Context context) {

			SocketChannel channel = (SocketChannel) key.channel();
			TaskRequest out = (TaskRequest) context.content;
			JPPFTaskBundle bundle = out.getBundle();
			Request request = out.getRequest();
			try {
				if (fillRequest(channel, out.getRequest())) {

					SerializationHelper helper = new SerializationHelperImpl();
					TaskCompletionListener listener = bundle
							.getCompletionListener();

					long elapsed = System.currentTimeMillis()
							- request.getStart();

					DataInputStream dis = new DataInputStream(
							new ByteArrayInputStream(request.getOutput()
									.toByteArray()));

					bundle = (JPPFTaskBundle) helper.readNextObject(dis, false);
					List<byte[]> taskList = new ArrayList<byte[]>();
					for (int i = 0; i < bundle.getTaskCount(); i++)
						taskList.add(helper.readNextBytes(dis));
					bundle.setTasks(taskList);
					dis.close();
					if (isStatsEnabled()) {
						taskExecuted(bundle.getTaskCount(), elapsed, bundle
								.getNodeExecutionTime(), out.getBundleBytes());
					}
					listener.taskCompleted(bundle);

					// wrapper.setBytes(resultBytes);
					listener.taskCompleted(bundle);

					// verificar se tem tarefa nova
					bundle = getQueue().nextBundle();
					try {
						if (bundle != null) {
							sendTask(channel, key, context, bundle);
							return;
						}
					} catch (Exception e) {
						getQueue().addBundle(bundle);
						bundle = null;
						throw e;
					}

					availableNodes.add(channel);
					key.interestOps(0);
				}
			} catch (Exception e) {

				if (bundle != null) {
					getQueue().addBundle(bundle);
				}

			}
		}
	}

	public void sendTask(SocketChannel channel, SelectionKey key,
			Context context, JPPFTaskBundle bundle) throws Exception {

		ByteArrayOutputStream baos = new ByteArrayOutputStream() {
			// overriden for performance reasons - no need to create a copy of
			// the buffer here.
			public synchronized byte[] toByteArray() {
				return buf;
			}
		};
		DataOutputStream dos = new DataOutputStream(baos);
		SerializationHelper helper = new SerializationHelperImpl();

		long size = bundle.getDataProvider().length;
		helper.writeNextObject(bundle, dos, false);
		helper.writeNextBytes(dos, bundle.getDataProvider(), 0, bundle
				.getDataProvider().length);
		for (byte[] task : bundle.getTasks()) {
			size += task.length;
			helper.writeNextBytes(dos, task, 0, task.length);
		}

		dos.flush();
		dos.close();

		byte[] data = baos.toByteArray();
		ByteBuffer sending = ByteBuffer.allocateDirect(data.length + 4);
		sending.putInt(data.length);
		sending.put(data);
		sending.flip();
		channel.write(sending);
		if (sending.hasRemaining()) {
			context.content = new TaskRequest(new Request(), sending, bundle,
					size);
			context.state = SendingJob;
			key.interestOps(SelectionKey.OP_WRITE);
		} else {
			context.state = WaitingResult;
			context.content = new TaskRequest(new Request(), sending, bundle,
					size);
			key.interestOps(SelectionKey.OP_READ);
		}
	}

	private class TaskRequest {
		private Request request;

		private ByteBuffer sending;

		private JPPFTaskBundle bundle;

		private long bundleBytes;

		public TaskRequest(Request request, ByteBuffer sending,
				JPPFTaskBundle bundle, long bundleBytes) {
			super();
			this.bundle = bundle;
			this.request = request;
			this.sending = sending;
			this.bundleBytes = bundleBytes;
		}

		public Request getRequest() {
			return request;
		}

		public ByteBuffer getSending() {
			return sending;
		}

		public JPPFTaskBundle getBundle() {
			return bundle;
		}

		public long getBundleBytes() {
			return bundleBytes;
		}

	}
}
