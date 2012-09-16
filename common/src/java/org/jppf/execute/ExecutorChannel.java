package org.jppf.execute;

import org.jppf.management.JPPFManagementInfo;
import org.jppf.management.JPPFSystemInformation;
import org.jppf.server.scheduler.bundle.Bundler;
import org.jppf.server.scheduler.bundle.JPPFContext;

/**
 * Created with IntelliJ IDEA.
 * User: jandam
 * Date: 9/4/12
 * Time: 9:28 AM
 * To change this template use File | Settings | File Templates.
 */
public interface ExecutorChannel<T> {

  /**
   * Get the unique identifier of the client.
   * @return the uuid as a string.
   */
  public String getUuid();

  /**
   * Get the unique ID for the connection.
   * @return the connection id.
   */
  public String getConnectionUuid();

  /**
   * Get the bundler used to schedule tasks for the corresponding node.
   * @return a {@link org.jppf.server.scheduler.bundle.Bundler} instance.
   */
  public Bundler getBundler();

  /**
   * Check whether the bundler held by this context is up to date by comparison
   * with the specified bundler.<br>
   * If it is not, then it is replaced with a copy of the specified bundler, with a
   * timestamp taken at creation time.
   * @param serverBundler the bundler to compare with.
   * @param jppfContext execution context.
   * @return true if the bundler is up to date, false if it wasn't and has been updated.
   */
  public boolean checkBundler(final Bundler serverBundler, final JPPFContext jppfContext);

  /**
   * Get the system information.
   * @return a {@link org.jppf.management.JPPFSystemInformation} instance.
   */
  public JPPFSystemInformation getSystemInfo();

  /**
   * Get the management information.
   * @return a {@link org.jppf.management.JPPFManagementInfo} instance.
   */
  public JPPFManagementInfo getManagementInfo();

  /**
   * Submit bundle for execution on corresponding node.
   * @param bundle an instance.
   * @return a {@link JPPFFuture}.
   */
  public JPPFFuture<?> submit(final T bundle);

  /**
   * Determine whether this channel is local (for an in-JVM node).
   * @return <code>false</code> if the channel is local, <code>false</code> otherwise.
   */
  public boolean isLocal();

  public ExecutorStatus getExecutionStatus();

  /**
   * Close this channel and release the resources it uses.
   */
  public void close() throws Exception;

  public Object getMonitor();
}
