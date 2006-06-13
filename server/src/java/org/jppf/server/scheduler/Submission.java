package org.jppf.server.scheduler;

import java.util.List;

import org.jppf.server.event.TaskCompletionListener;
import org.jppf.server.scheduler.bundle.Bundler;

/**
 * This class represents a submission to JPPF Driver. Each submission has a
 * bunch of independent tasks, with certain priority.
 * 
 * @author Domingos Creado
 * 
 */
public final class Submission {

	/**
	 * The tasks to be executed by the node.
	 */
	private List<byte[]> tasks = null;

	/**
	 * The priority of this task bundle.
	 */
	private int priority = 0;

	/**
	 * The task completion listener to notify, once the execution of this task
	 * has completed.
	 */
	private TaskCompletionListener completionListener = null;

	/**
	 * The unique identifier for this task bundle.
	 */
	private String uuid = null;

	/**
	 * The unique identifier for the request this task is a part of.
	 */
	private String requestUuid = null;

	/**
	 * The unique identifier for the submitting application.
	 */
	private String appUuid = null;

	/**
	 * The shared data provider for this task bundle.
	 */
	private byte[] dataProvider = null;
	
	/**
	 * the algorithm that dynamically computes the task bundle size
	 */
	private Bundler bundler;

	public Submission(String uuid, String requestUuid, String appUuid,
			byte[] dataProvider, int priority,
			TaskCompletionListener completionListener, List<byte[]> tasks, Bundler bundler) {
		super();
		this.tasks = tasks;
		this.priority = priority;
		this.completionListener = completionListener;
		this.uuid = uuid;
		this.requestUuid = requestUuid;
		this.appUuid = appUuid;
		this.dataProvider = dataProvider;
		this.bundler = bundler;
	}

	/**
	 * Get the priority of this task bundle.
	 * 
	 * @return the priority as an int.
	 */
	public int getPriority() {
		return priority;
	}

	/**
	 * Set the priority of this task bundle.
	 * 
	 * @param priority
	 *            the priority as an int.
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}


}
