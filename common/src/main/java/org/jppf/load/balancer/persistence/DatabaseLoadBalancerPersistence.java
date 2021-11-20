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

package org.jppf.load.balancer.persistence;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.jppf.persistence.AbstractDatabasePersistence;
import org.jppf.serialization.JPPFSerializationHelper;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * A job persistence implementation which stores jobs in a single database table. The table has the following structure:<br>
 *
 * <pre class="jppf_pre">
 * CREATE TABLE &lt;table_name&gt; (
 *   NODEID varchar(250) NOT NULL,
 *   ALGORITHMID varchar(250) NOT NULL,
 *   STATE blob NOT NULL,
 *   PRIMARY KEY (NODEID, ALGORITHMID)
 * );</pre>
 *
 * <p>Where:
 * <ul style="margin-top: 0px">
 *   <li>the NODEID column represents a hash of a string concatenated from various properties of the node. This id is unique for each node
 *   and resilient over node restarts, contrary to the node uuid, which is recreated each time a node starts</li>
 *   <li>the ALGORITHMID column is a hash of the load-balancer's algorithm name.</li>
 *   <li>the STATE column represents the serialized state of the load-balancer, such as provided by {@link PersistentState#getState()}</li>
 * </ul>
 *
 * <p>The table name is specified in the JPPF configuration like this:<br>
 * <pre class="jppf_pre">
 * lb.pkg = org.jppf.load.balancer.persistence
 * jppf.load.balancing.persistence = ${lb.pkg}.DatabaseLoadBalancerPersistence &lt;table_name&gt;</pre>
 * <br>If unspecified, it defaults to the {@linkplain #DEFAULT_TABLE default table name} 'load_balancer'.
 * If the table does not exist, JPPF will attempt to create it. If this fails for any reason, for instance if the user does not have sufficient privileges,
 * then persistence will be disabled.
 *
 * <p>This database persistence implementation uses a <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> connection pool and datasource.
 * The datasource is specified by name in the configuration:<br>
 * {@code jppf.load.balancing.persistence = org.jppf.load.balancer.persistence.DatabaseLoadBalancerPersistence <table_name> <datasource_name>}<br>
 * If unspecified, it defaults to 'loadBalancerDS'. The datasource properties <b>must</b> be defined in the JPPF configuration like so:
 *
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; margin: 0px; background-color: #E0E0F0">
 * jppf.datasource.&lt;configId&gt;.name = &lt;datasource_name&gt;
 * jppf.datasource.&lt;configId&gt;.&lt;hikaricp_property&gt; = &lt;value&gt;</pre>
 *
 * <p>Where:
 *   <ul style="margin-top: 0px">
 *   <li>{@code configId} is used to distinguish the datasource properties when multiple datasources are defined</li>
 *   <li>the datasource {@code name} is mandatory and is used to store and retrieve the datasource in a custom registry.
 *       It is also the datasource name used in the configuration of this job persistence implementation</li>
 *   <li>{@code hikaricp_property} desginates any valid <a href="https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby">HikariCP configuration property</a>.
 *       Properties not supported by HikariCP are simply ignored</li>
 * </ul>
 *
 * <p>Here is a full example configuration:
 * <pre class="jppf_pre">
 * <span style="color: green"># persistence definition</span>
 * lb.pkg = org.jppf.load.balancer.persistence
 * jppf.load.balancing.persistence = ${lb.pkg}.DatabaseLoadBalancerPersistence MY_TABLE <b>loadBalancerDS</b>
 *
 * <span style="color: green"># datasource definition</span>
 * jppf.datasource.lb.name = <b>loadBalancerDS</b>
 * jppf.datasource.lb.driverClassName = com.mysql.jdbc.Driver
 * jppf.datasource.lb.jdbcUrl = jdbc:mysql://localhost:3306/testjppf
 * jppf.datasource.lb.username = testjppf
 * jppf.datasource.lb.password = testjppf
 * jppf.datasource.lb.minimumIdle = 5
 * jppf.datasource.lb.maximumPoolSize = 10
 * jppf.datasource.lb.connectionTimeout = 30000
 * jppf.datasource.lb.idleTimeout = 600000</pre>
 *
 * @author Laurent Cohen
 * @since 6.0
 */
public class DatabaseLoadBalancerPersistence extends AbstractDatabasePersistence<LoadBalancerPersistenceInfo> implements LoadBalancerPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DatabaseLoadBalancerPersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The default persistence table name.
   */
  public static final String DEFAULT_TABLE = "load_balancer";
  /**
   * The default persistence datasource name.
   */
  public static final String DEFAULT_DATASOURCE = "loadBalancerDS";
  /**
   * The number of persistence operations, including load, store, delete and list, that have started but not yet completed.
   */
  private final AtomicInteger uncompletedOperations = new AtomicInteger(0);

  /**
   * Intialize this persistence with the {@linkplain #DEFAULT_TABLE default table name}.
   * @throws Exception if any error occurs.
   */
  public DatabaseLoadBalancerPersistence() throws Exception {
    this(DEFAULT_TABLE, DEFAULT_DATASOURCE);
  }

  /**
   * Intialize this persistence with a table name specified in the first string parameter.
   * @param params if parameters are provided, they have this meaning:
   * <ul style="margin-top: 0px">
   * <li>params[0] is the table name, which defaults to 'JOB_PERSISTENCE'</li>
   * <li>params[1] is the name of a datasource defined in the configuration, and defaults to 'job_persistence'</li>
   * </ul>
   * @throws Exception if any error occurs.
   */
  public DatabaseLoadBalancerPersistence(final String...params) throws Exception {
    super(DEFAULT_TABLE, DEFAULT_DATASOURCE, JPPFProperties.LOAD_BALANCING_PERSISTENCE_DDL_LOCATION, params);
  }

  @Override
  public Object load(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    if (debugEnabled) log.debug("loading {}", info);
    final String sql = getSQL("load.sql");
    final String[] args = { info.getChannelID(), info.getAlgorithmID() };
    try (final ConnectionWrapper wrapper = getConnection(false, Connection.TRANSACTION_READ_COMMITTED)) {
      if (debugEnabled) log.debug("before executing sql=\"{}\" with params={}", sql, Arrays.toString(args));
      try (PreparedStatement ps = wrapper.getConnection().prepareStatement(sql)) {
        ps.setString(1, args[0]);
        ps.setString(2, args[1]);
        try (ResultSet rs = ps.executeQuery()) {
          if (rs.next()) {
            final Object o = JPPFSerializationHelper.deserialize(rs.getBinaryStream(1));
            wrapper.getConnection().commit();
            return o;
          }
        }
      } catch(final Exception e) {
        wrapper.getConnection().rollback();
        throw e;
      }
    } catch(final Exception e) {
      final String message = "error performing SQL query = \"" + sql + "\" with params = " + Arrays.toString(args);
      throw new LoadBalancerPersistenceException(message, e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
    return null;
  }

  @Override
  public void store(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    if (debugEnabled) log.debug("storing {}", info);
    try (ConnectionWrapper wrapper = getConnection(false, Connection.TRANSACTION_READ_COMMITTED)) {
      final Connection connection = wrapper.getConnection();
      try {
        storeElement(connection, info, info.getStateAsBytes());
        connection.commit();
      } catch(final Exception e) {
        connection.rollback();
        throw e;
      }
    } catch(final LoadBalancerPersistenceException e) {
      throw e;
    } catch(final Exception e) {
      throw new LoadBalancerPersistenceException(e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
  }

  @Override
  public void delete(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    if (debugEnabled) log.debug("deleting {}", info);
    String sql = null;
    String[] args = EMPTY_STRINGS;
    if ((info == null) || ((info.getChannelID() == null) && (info.getAlgorithmID() == null))) {
      sql = getSQL("delete.sql");
    } else if (info.getAlgorithmID() == null) {
      sql = getSQL("delete.node.sql");
      args = new String[] { info.getChannelID() };
    } else if (info.getChannelID() == null) {
      sql = getSQL("delete.algo.all.nodes.sql");
      args = new String[] { info.getAlgorithmID() };
    } else {
      sql = getSQL("delete.algo.sql");
      args = new String[] { info.getChannelID(), info.getAlgorithmID() };
    }
    if (debugEnabled) log.debug("before executing sql=\"{}\" with params={}", sql, Arrays.toString(args));
    try (ConnectionWrapper wrapper = getConnection(false, Connection.TRANSACTION_READ_COMMITTED)) {
      final Connection connection = wrapper.getConnection();
      try (final PreparedStatement ps = connection.prepareStatement(sql)) {
        for (int i=0; i<args.length; i++) ps.setString(i + 1, args[i]);
        ps.executeUpdate();
        connection.commit();
      } catch(final Exception e) {
        connection.rollback();
        throw e;
      }
    } catch(final Exception e) {
      final String message = "error performing SQL query = \"" + sql + "\" with params = " + Arrays.toString(args);
      throw new LoadBalancerPersistenceException(message, e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
  }

  @Override
  public List<String> list(final LoadBalancerPersistenceInfo info) throws LoadBalancerPersistenceException {
    uncompletedOperations.incrementAndGet();
    String sql = null;
    String[] args = EMPTY_STRINGS;
    if ((info == null) || ((info.getChannelID() == null) && (info.getAlgorithmID() == null))) {
      sql = getSQL("get.all.nodes.sql");
    } else if (info.getAlgorithmID() == null) {
      sql = getSQL("get.all.algos.for.node.sql");
      args = new String[] { info.getChannelID() };
    } else if (info.getChannelID() == null) {
      sql = getSQL("get.all.nodes.with.algo.sql");
      args = new String[] { info.getAlgorithmID() };
    } else {
      sql = getSQL("get.node.with.algo.sql");
      args = new String[] { info.getChannelID(), info.getAlgorithmID() };
    }
    if (debugEnabled) log.debug("before executing sql=\"{}\" with params={}", sql, Arrays.toString(args));
    final List<String> result = new ArrayList<>();
    try (ConnectionWrapper wrapper = getConnection(false, Connection.TRANSACTION_READ_COMMITTED)) {
      final Connection connection = wrapper.getConnection();
      try (final PreparedStatement ps = connection.prepareStatement(sql)) {
        for (int i=0; i<args.length; i++) ps.setString(i + 1, args[i]);
        try (final ResultSet rs = ps.executeQuery()) {
          while (rs.next()) result.add(rs.getString(1));
          connection.commit();
        }
      } catch(final Exception e) {
        connection.rollback();
        throw e;
      }
    } catch(final Exception e) {
      final String message = "error performing SQL query = \"" + sql + "\" with params = " + Arrays.toString(args);
      throw new LoadBalancerPersistenceException(message, e);
    } finally {
      uncompletedOperations.decrementAndGet();
    }
    if (debugEnabled) log.debug("result for {} is {}", info, result);
    return result;
  }

  /** @exclude */
  @Override
  protected boolean lockForUpdate(final Connection connection, final LoadBalancerPersistenceInfo info) throws Exception {
    final String[] args = { info.getChannelID(), info.getAlgorithmID() };
    final String sql = getSQL("select.for.update.sql");
    if (debugEnabled) log.debug("before performing SQL query = \"{}\" with params = {}", sql, Arrays.toString(args));
    try (final PreparedStatement ps = connection.prepareStatement(sql)) {
      ps.setString(1, args[0]);
      ps.setString(2, args[1]);
      try (final ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch(final SQLException e) {
      final String message = "error performing SQL query = \"" + sql + "\" with params = " + Arrays.toString(args);
      throw new LoadBalancerPersistenceException(message, e);
    }
  }

  /** @exclude */
  @Override
  protected void insertElement(final Connection connection, final LoadBalancerPersistenceInfo info, final byte[] bytes) throws Exception {
    final String sql = getSQL("insert.sql");
    if (debugEnabled) log.debug("before performing SQL update = \"{}\" with params = [{}, {}, blob(length={})]", sql, info.getChannelID(), info.getAlgorithmID(), bytes.length);
    try (final PreparedStatement ps = connection.prepareStatement(sql)) {
      try (final InputStream is = new ByteArrayInputStream(bytes)) {
        ps.setString(1, info.getChannelID());
        ps.setString(2, info.getAlgorithmID());
        ps.setBlob(3, is);
        ps.executeUpdate();
      }
    } catch(final SQLException e) {
      final String message = "error performing SQL update = \"" + sql + "\" with params = [" + info.getChannelID() + ", " + info.getAlgorithmID() + ", blob(length=" + bytes.length + ")]";
      throw new LoadBalancerPersistenceException(message, e);
    }
  }

  /** @exclude */
  @Override
  protected void updateElement(final Connection connection, final LoadBalancerPersistenceInfo info, final byte[] bytes) throws Exception {
    final String sql = getSQL("update.sql");
    if (debugEnabled) log.debug("before performing SQL update = \"{}\" with params = [blob(length={}), {}, {}]", sql, bytes.length, info.getChannelID(), info.getAlgorithmID());
    try (PreparedStatement ps2 = connection.prepareStatement(sql)) {
      try (InputStream is = new ByteArrayInputStream(bytes)) {
        ps2.setBlob(1, is);
        ps2.setString(2, info.getChannelID());
        ps2.setString(3, info.getAlgorithmID());
        ps2.executeUpdate();
      }
    } catch(final SQLException e) {
      final String message = "error performing SQL update = \"" + sql + "\" with params = [blob(length=" + bytes.length + "), " + info.getChannelID() + ", " + info.getAlgorithmID() + "]";
      throw new LoadBalancerPersistenceException(message, e);
    }
  }

  @Override
  public int getUncompletedOperations() {
    return uncompletedOperations.get();
  }
}
