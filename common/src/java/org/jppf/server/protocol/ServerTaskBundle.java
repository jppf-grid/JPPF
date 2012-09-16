package org.jppf.server.protocol;

import org.jppf.execute.ExecutorChannel;
import org.jppf.io.DataLocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Created with IntelliJ IDEA.
 * User: jandam
 * Date: 9/3/12
 * Time: 12:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class ServerTaskBundle extends JPPFTaskBundle {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  /**
   * The job to execute.
   */
  private final ServerJob job;
  /**
   * The shared data provider for this task bundle.
   */
  private transient DataLocation dataProvider = null;
  /**
   * The tasks to be executed by the node.
   */
  private transient List<DataLocation> tasks = null;
  /**
   * The broadcast UUID.
   */
  private transient String broadcastUUID = null;
  /**
   * Job requeue indicator.
   */
  private boolean requeued = false;
  /**
   * Job cancel indicator
   */
  private boolean cancelled  = false;

  private JPPFTaskBundle taskBundle;

  /**
   * Initialize this task bundle and set its build number.
   * @param job   the job to execute.
   * @param tasks the tasks to execute.
   */
  public ServerTaskBundle(final ServerJob job, final List<DataLocation> tasks) {
    this(job, null, tasks);
  }

  public ServerTaskBundle(final ServerJob job, final JPPFTaskBundle taskBundle, final List<DataLocation> tasks)
  {
    if (job == null) throw new IllegalArgumentException("job is null");

    this.job = job;
    if(taskBundle == null)
      this.taskBundle = job.getJob();
    else
      this.taskBundle = taskBundle;
//    if(getState() == State.INITIAL_BUNDLE)
//      this.taskBundle = this.job.getJob();
//    else
//      this.taskBundle = this.job.getJob().copy(tasks.size());
    this.setSLA(job.getSLA());
    this.setMetadata(job.getJob().getMetadata());
    this.tasks = Collections.unmodifiableList(new ArrayList<DataLocation>(tasks));
    this.setName(job.getJob().getName());
    this.dataProvider = job.getDataProvider();
    setTaskCount(this.tasks.size());
  }

  /**
   * Get the job this submission is for.
   * @return a {@link JPPFTaskBundle} instance.
   */
  public JPPFTaskBundle getJob()
  {
    return taskBundle;
  }

  /**
   * Get the client job this submission is for
   * @return a {@link ServerJob} instance.
   */
  public ServerJob getClientJob()
  {
    return job;
  }

  /**
   * Get shared data provider for this task.
   * @return a <code>DataProvider</code> instance.
   */
  public DataLocation getDataProviderL()
  {
    return dataProvider;
  }

  /**
   * Set shared data provider for this task.
   * @param dataProvider a <code>DataProvider</code> instance.
   */
  public void setDataProviderL(final DataLocation dataProvider)
  {
    this.dataProvider = dataProvider;
  }

  /**
   * Get the tasks to be executed by the node.
   * @return the tasks as a <code>List</code> of arrays of bytes.
   */
  public List<DataLocation> getTasksL()
  {
    return tasks;
  }

  /**
   * Make a copy of this bundle.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  @Override
  public ServerTaskBundle copy()
  {
    throw new UnsupportedOperationException();
//    ClientTaskBundle bundle = new ClientTaskBundle(getJob(), tasks);
//    bundle.setUuidPath(getUuidPath());
//    bundle.setRequestUuid(getRequestUuid());
//    bundle.setUuid(getUuid());
//    bundle.setName(getName());
//    bundle.setTaskCount(getTaskCount());
//    bundle.setDataProvider(getDataProvider());
//    synchronized (bundle.getParametersMap())
//    {
//      for (Map.Entry<Object, Object> entry : getParametersMap().entrySet())
//        bundle.setParameter(entry.getKey(), entry.getValue());
//    }
//    bundle.setQueueEntryTime(getQueueEntryTime());
//    bundle.setCompletionListener(getCompletionListener());
//    bundle.setSLA(getSLA());
//    bundle.setLocalExecutionPolicy(localExecutionPolicy);
//    bundle.setBroadcastUUID(broadcastUUID);
//    //bundle.setParameter(BundleParameter.JOB_METADATA, getJobMetadata());
//
//    return bundle;
  }

  /**
   * Make a copy of this bundle containing only the first nbTasks tasks it contains.
   * @param nbTasks the number of tasks to include in the copy.
   * @return a new <code>ClientTaskBundle</code> instance.
   */
  @Override
  public ServerTaskBundle copy(final int nbTasks)
  {
    throw new UnsupportedOperationException();
//    ClientTaskBundle bundle = copy();
//    bundle.setTaskCount(nbTasks);
//    taskCount -= nbTasks;
//    return bundle;
  }

  /**
   * Get the broadcast UUID.
   * @return an <code>String</code> instance.
   */
  public String getBroadcastUUID()
  {
    return broadcastUUID;
  }

  /**
   * Set the broadcast UUID.
   * @param broadcastUUID the broadcast UUID.
   */
  public void setBroadcastUUID(final String broadcastUUID)
  {
    this.broadcastUUID = broadcastUUID;
  }

  /**
   * Called when all or part of a job is dispatched to a node.
   * @param channel the node to which the job is dispatched.
   * @param future  future assigned to bundle execution.
   */
  public void jobDispatched(final ExecutorChannel channel, final Future<?> future)
  {
    job.jobDispatched(this, channel, future);
  }

  /**
   * Called to notify that the results of a number of tasks have been received from the server.
   * @param results the list of tasks whose results have been received from the server.
   */
  public void resultsReceived(final List<DataLocation> results)
  {
    job.resultsReceived(this, results);
  }

  /**
   * Called to notify that throwable eventually raised while receiving the results.
   * @param throwable the throwable that was raised while receiving the results.
   */
  public void resultsReceived(final Throwable throwable)
  {
    job.resultsReceived(this, throwable);
  }

  /**
   * Called to notify that the execution of a task has completed.
   * @param exception the {@link Exception} thrown during job execution or <code>null</code>.
   */
  public void taskCompleted(final Exception exception)
  {
    job.taskCompleted(this, exception);
  }

  /**
   * Called when this task bundle should be resubmitted
   */
  public synchronized void resubmit()
  {
    if (getSLA().isBroadcastJob()) return; // broadcast jobs cannot be resumbitted.
    requeued = true;
  }

  /**
   * Get the requeued indicator.
   * @return <code>true</code> if job is requeued, <code>false</code> otherwise.
   */
  public synchronized boolean isRequeued()
  {
    return requeued;
  }

  /**
   * Called when this task bundle is cancelled.
   */
  public synchronized void cancel()
  {
    this.cancelled = true;
  }

  /**
   * Get the cancelled indicator.
   * @return <code>true</code> if job is cancelled, <code>false</code> otherwise.
   */
  public synchronized boolean isCancelled()
  {
    return cancelled;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder("[");
    sb.append("jobId=").append(getName());
    sb.append(", jobUuid=").append(getUuid());
    sb.append(", initialTaskCount=").append(getInitialTaskCount());
    sb.append(", taskCount=").append(getTaskCount());
    sb.append(", requeue=").append(isRequeued());
    sb.append(", cancelled=").append(isCancelled());
    sb.append(']');
    return sb.toString();
  }
}