/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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

package org.jppf.management;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import javax.management.*;
import javax.management.remote.*;

import org.jppf.JPPFException;
import org.jppf.ssl.SSLHelper;
import org.jppf.utils.*;
import org.slf4j.*;

/**
 * Wrapper around a JMX connection, providing a thread-safe way of handling disconnections and recovery.
 * @author Laurent Cohen
 */
public class JMXConnectionWrapper extends ThreadSynchronization implements JPPFAdminMBean
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JMXConnectionWrapper.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether trace log statements are enabled.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * URL of the MBean server, in a JMX-compliant format.
   */
  protected JMXServiceURL url = null;
  /**
   * The JMX client.
   */
  protected JMXConnector jmxc = null;
  /**
   * A connection to the MBean server.
   */
  protected AtomicReference<MBeanServerConnection> mbeanConnection = new AtomicReference<MBeanServerConnection>(null);
  /**
   * The host the server is running on.
   */
  protected String host = null;
  /**
   * The RMI port used by the server.
   */
  protected int port = 0;
  /**
   * The connection thread that performs the connection to the management server.
   */
  protected AtomicReference<JMXConnectionThread> connectionThread = new AtomicReference<JMXConnectionThread>(null);
  /**
   * A string representing this connection, used for logging purposes.
   */
  protected String idString = null;
  /**
   * A string representing this connection, used for displaying in the admin conosle.
   */
  protected String displayName = null;
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  protected AtomicBoolean connected = new AtomicBoolean(false);
  /**
   * Determines whether the connection to the JMX server has been established.
   */
  protected boolean local = false;
  /**
   * JMX property used for establishing the connection.
   */
  protected Map<String, Object> env = new HashMap<String, Object>();
  /**
   * Determines whether the JMX connection should be secure or not.
   */
  protected boolean secure = false;
  /**
   * 
   */
  private final Object connectionLock = new Object();

  /**
   * Initialize a local connection (same JVM) to the MBean server.
   */
  public JMXConnectionWrapper()
  {
    local = true;
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the RMI port used by the server.
   * @param rmiSuffix	RMI registry namespace suffix.
   * @deprecated JPPF no longer uses the RMI remote connector for JMX.
   */
  private JMXConnectionWrapper(final String host, final int port, final String rmiSuffix)
  {
    this.host = host;
    this.port = port;

    try
    {
      idString = host + ':' + port;
      displayName = idString;
      String s = null;
      if (!JMXServerFactory.isUsingRMIConnector() && JMXServerFactory.isJMXMPPresent()) s = "service:jmx:jmxmp://" + idString;
      else s = "service:jmx:rmi:///jndi/rmi://" + idString + rmiSuffix;
      url = new JMXServiceURL(s);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    local = false;
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @param host the host the server is running on.
   * @param port the port used by the server.
   * @param ssl specifies whether the jmx connection should be secure or not.
   */
  public JMXConnectionWrapper(final String host, final int port, final boolean ssl)
  {
    try
    {
      /*
      InetAddress addr = InetAddress.getByName(host);
      this.host = (addr instanceof Inet6Address) ? "[" + host + "]" : host;
      */
      this.host = (NetworkUtils.isIPv6Address(host)) ? "[" + host + "]" : host;
      this.port = port;
      this.secure = ssl;
      idString = this.host + ':' + this.port;
      this.displayName = this.idString;
      url = new JMXServiceURL("service:jmx:jmxmp://" + idString);
      if (ssl) SSLHelper.configureJMXProperties(env);
    }
    catch(Exception e)
    {
      log.error(e.getMessage(), e);
    }
    local = false;
  }

  /**
   * Initialize the connection to the remote MBean server.
   */
  public void connect()
  {
    if (isConnected()) return;
    if (local)
    {
      mbeanConnection.set(ManagementFactory.getPlatformMBeanServer());
      setConnectedStatus(true);
    }
    else
    {
      JMXConnectionThread jct = null;
      boolean jctNull = true;
      synchronized(this)
      {
        jctNull = (jct = connectionThread.get()) == null;
        if (jctNull)
        {
          jct = new JMXConnectionThread(this);
          connectionThread.set(jct);
          Thread t = new Thread(jct, "JMX connection " + getId());
          t.setDaemon(true);
          t.start();
        }
      }
      if (!jctNull && !jct.isConnecting()) jct.resume();
    }
  }

  /**
   * Initiate the connection and wait until the connection is established or the timeout has expired, whichever comes first.
   * @param timeout the maximum time to wait for, a value of zero means no timeout and
   * this method just waits until the connection is established.
   */
  public void connectAndWait(final long timeout)
  {
    if (isConnected()) return;
    long start = System.currentTimeMillis();
    long max = timeout > 0 ? timeout : Long.MAX_VALUE;
    connect();
    long elapsed;
    while (!isConnected() && ((elapsed = System.currentTimeMillis() - start) < max)) goToSleep(max - elapsed);
  }

  /**
   * Initialize the connection to the remote MBean server.
   * @throws Exception if the connection could not be established.
   */
  void performConnection() throws Exception
  {
    setConnectedStatus(false);
    synchronized(connectionLock)
    {
      jmxc = JMXConnectorFactory.connect(url, env);
    }
    synchronized(this)
    {
      mbeanConnection.set(jmxc.getMBeanServerConnection());
      try
      {
        setHost(InetAddress.getByName(host).getHostName());
      }
      catch (UnknownHostException e)
      {
      }
    }
    setConnectedStatus(true);
    if (debugEnabled) log.debug(getId() + " JMX connection successfully established");
  }

  /**
   * Close the connection to the remote MBean server.
   * @throws Exception if the connection could not be closed.
   */
  public void close() throws Exception
  {
    if (connectionThread.get() != null) connectionThread.get().close();
    if (jmxc != null) jmxc.close();
  }

  /**
   * Invoke a method on the specified MBean.
   * @param name the name of the MBean.
   * @param methodName the name of the method to invoke.
   * @param params the method parameter values.
   * @param signature the types of the method parameters.
   * @return an object or null.
   * @throws Exception if the invocation failed.
   */
  public Object invoke(final String name, final String methodName, final Object[] params, final String[] signature) throws Exception
  {
    if (!isConnected() || ((connectionThread.get() != null) && connectionThread.get().isConnecting())) return null;
    synchronized(this)
    {
      Object result = null;
      try
      {
        ObjectName mbeanName = new ObjectName(name);
        result = getMbeanConnection().invoke(mbeanName, methodName, params, signature);
      }
      catch(IOException e)
      {
        if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
        setConnectedStatus(false);
        try
        {
          if (jmxc != null) jmxc.close();
        }
        catch(Exception e2)
        {
          if (debugEnabled) log.debug(e2.getMessage(), e2);
        }
        if (!connectionThread.get().isConnecting()) connectionThread.get().resume();
      }
      return result;
    }
  }

  /**
   * Get the value of an attribute of the specified MBean.
   * @param name the name of the MBean.
   * @param attribute the name of the attribute to read.
   * @return an object or null.
   * @throws Exception if the invocation failed.
   */
  public Object getAttribute(final String name, final String attribute) throws Exception
  {
    if (!isConnected() || ((connectionThread.get() != null) && connectionThread.get().isConnecting())) return null;
    synchronized(this)
    {
      Object result = null;
      try
      {
        ObjectName mbeanName = new ObjectName(name);
        result = getMbeanConnection().getAttribute(mbeanName, attribute);
      }
      catch(IOException e)
      {
        setConnectedStatus(false);
        try
        {
          if (jmxc != null) jmxc.close();
        }
        catch(Exception e2)
        {
          if (debugEnabled) log.debug(e2.getMessage(), e2);
        }
        if (!connectionThread.get().isConnecting()) connectionThread.get().resume();
        if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
        throw e;
      }
      return result;
    }
  }


  /**
   * Get the value of an attribute of the specified MBean.
   * @param name the name of the MBean.
   * @param attribute the name of the attribute to read.
   * @param value the value to set on the attribute.
   * @throws Exception if the invocation failed.
   */
  public void setAttribute(final String name, final String attribute, final Object value) throws Exception
  {
    if (!isConnected() || ((connectionThread.get() != null) && connectionThread.get().isConnecting())) return;
    synchronized(this)
    {
      try
      {
        ObjectName mbeanName = new ObjectName(name);
        getMbeanConnection().setAttribute(mbeanName, new Attribute(attribute, value));
      }
      catch(IOException e)
      {
        setConnectedStatus(false);
        try
        {
          if (jmxc != null) jmxc.close();
        }
        catch(Exception e2)
        {
          if (debugEnabled) log.debug(e2.getMessage(), e2);
        }
        if (!connectionThread.get().isConnecting()) connectionThread.get().resume();
        if (debugEnabled) log.debug(getId() + " : error while invoking the JMX connection", e);
        throw e;
      }
    }
  }

  /**
   * Get the host the server is running on.
   * @return the host as a string.
   */
  public String getHost()
  {
    return host;
  }

  /**
   * Get the host the server is running on.
   * @param host the host as a string.
   */
  public void setHost(final String host)
  {
    this.host = host;
    //this.idString = this.host + ':' + this.port;
    this.displayName = this.host + ':' + this.port;
  }

  /**
   * Get the RMI port used by the server.
   * @return the port as an int.
   */
  public int getPort()
  {
    return port;
  }

  /**
   * Get a string describing this connection.
   * @return a string in the format host:port.
   */
  public String getId()
  {
    return idString;
  }

  /**
   * Get the service URL of the MBean server.
   * @return a {@link JMXServiceURL} instance.
   */
  public JMXServiceURL getURL()
  {
    return url;
  }

  /**
   * Get the connection to the MBean server.
   * @return a <code>MBeanServerConnection</code> instance.
   */
  public MBeanServerConnection getMbeanConnection()
  {
    return mbeanConnection.get();
  }

  /**
   * Set the connected state of this connection wrapper.
   * @param status true if the jmx connection is established, false otherwise.
   */
  protected void setConnectedStatus(final boolean status)
  {
    connected.set(status);
    wakeUp();
  }

  /**
   * Determines whether the connection to the JMX server has been established.
   * @return true if the connection is established, false otherwise.
   */
  public boolean isConnected()
  {
    return connected.get();
  }

  /**
   * Obtain a proxy to the specified remote MBean.
   * @param <T> the type of the MBean (must be an interface).
   * @param name the name of the mbean to retrieve.
   * @param inf the class of the MBean interface.
   * @return an instance of the specified proxy interface.
   * @throws Exception if any error occurs.
   */
  public <T> T getProxy(final String name, final Class<T> inf) throws Exception
  {
    return getProxy(new ObjectName(name), inf);
  }

  /**
   * Obtain a proxy to the specified remote MBean.
   * @param <T> the type of the MBean (must be an interface).
   * @param objectName the name of the mbean to retrieve.
   * @param inf the class of the MBean interface.
   * @return an instance of the specified proxy interface.
   * @throws Exception if any error occurs.
   */
  public <T> T getProxy(final ObjectName objectName, final Class<T> inf) throws Exception
  {
    if (!isConnected()) connect();
    if (isConnected())
    {
      MBeanServerConnection mbsc = getMbeanConnection();
      return MBeanServerInvocationHandler.newProxyInstance(mbsc, objectName, inf, true);
    }
    return null;
  }

  @Override
  public JPPFSystemInformation systemInformation() throws Exception
  {
    throw new JPPFException("this method is not implemented");
  }

  /**
   * Get the string representing this connection, used for displaying in the admin conosle.
   * @return the display name as a string.
   */
  public String getDisplayName()
  {
    return displayName;
  }

  /**
   * Determine whether the JMX connection is secure or not.
   * @return <code>true</code> if this connection is secure, <code>false</code> otherwise.
   */
  public boolean isSecure()
  {
    return secure;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder().append(getClass().getSimpleName()).append('[');
    //sb.append(", idString=").append( idString);
    sb.append("url=").append( url);
    sb.append(", connected=").append( connected);
    sb.append(", local=").append( local);
    sb.append(", secure=").append( secure);
    sb.append(']');
    return sb.toString();
  }
}
