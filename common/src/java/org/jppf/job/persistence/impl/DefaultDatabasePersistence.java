/*
 * JPPF.
 * Copyright (C) 2005-2017 JPPF Team.
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

package org.jppf.job.persistence.impl;

import java.io.*;
import java.sql.*;
import java.util.*;

import javax.sql.DataSource;

import org.jppf.job.persistence.*;
import org.jppf.persistence.JPPFDatasourceFactory;
import org.jppf.utils.*;
import org.jppf.utils.configuration.JPPFProperties;
import org.slf4j.*;

/**
 * A job persistence implementation which stores jobs in a single database table. The table has the following structure:<br>
 *
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; margin: 0px; background-color: #E0E0F0">
 * CREATE TABLE &lt;table_name&gt; (
 *   UUID varchar(250) NOT NULL,
 *   TYPE varchar(20) NOT NULL,
 *   POSITION int NOT NULL,
 *   CONTENT blob NOT NULL,
 *   PRIMARY KEY (UUID, TYPE, POSITION)
 * );</pre>
 *
 * <p>Where:
 * <ul style="margin-top: 0px">
 *   <li>the UUID column represents the job uuid</li>
 *   <li>the TYPE column represents the type of object, taken from the {@link PersistenceObjectType} enum</li>
 *   <li>the POSITION column represents the object's position in the job if {@code TYPE} is 'task' or 'task_result', otherwise -1</li>
 *   <li>the CONTENT column represents the serialized object</li>
 * </ul>
 *
 * <p>The table name is specified in the JPPF configuration like this:<br>
 * {@code jppf.job.persistence = org.jppf.job.persistence.impl.DefaultDatabasePersistence <table_name>}<br>
 * If unspecified, it defaults to the {@linkplain #DEFAULT_TABLE default table name} 'JOB_PERSISTENCE'.
 * If the table does not exist, JPPF will attempt to create it. If this fails for any reason, for instance if the user does not have sufficient privileges,
 * then persistene will be disabled.
 *
 * <p>This database persistence implementation uses a <a href="https://github.com/brettwooldridge/HikariCP">HikariCP</a> connection pool and datasource.
 * The datasource is specified by name in the configuration:<br>
 * {@code jppf.job.persistence = org.jppf.job.persistence.impl.DefaultDatabasePersistence <table_name> <datasource_name>}<br>
 * If unspecified, it defaults to 'job_persistence'. The datasource properties <b>must</b> be defined in the JPPF configuration like so:
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
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; margin: 0px; background-color: #E0E0F0">
 * <span style="color: green"># persistence definition</span>
 * jppf.job.persistence = org.jppf.job.persistence.impl.DefaultDatabasePersistence MY_TABLE <b>jobDS</b>
 *
 * <span style="color: green"># datsource definition</span>
 * jppf.datasource.jobs.name = <b>jobDS</b>
 * jppf.datasource.jobs.driverClassName = com.mysql.jdbc.Driver
 * jppf.datasource.jobs.jdbcUrl = jdbc:mysql://localhost:3306/testjppf
 * jppf.datasource.jobs.username = testjppf
 * jppf.datasource.jobs.password = testjppf
 * jppf.datasource.jobs.minimumIdle = 5
 * jppf.datasource.jobs.maximumPoolSize = 10
 * jppf.datasource.jobs.connectionTimeout = 30000
 * jppf.datasource.jobs.idleTimeout = 600000</pre>
 * @author Laurent Cohen
 */
public class DefaultDatabasePersistence implements JobPersistence {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(DefaultDatabasePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The default persistence table name.
   */
  protected static final String DEFAULT_TABLE = "JOB_PERSISTENCE";
  /**
   * The default persistence table name.
   */
  protected static final String DEFAULT_DATASOURCE = "job_persistence";
  /**
   * The porperty for the persistence table name.
   */
  private static final String TABLE_PROP = "table";
  /**
   * The JDBC datasource name as defined in the configuration.
   */
  String dataSourceName;
  /**
   * The JDBC datasource.
   */
  protected DataSource dataSource;
  /**
   * The SQL statements used by this persistence implementation, loaded from a properties file in the classpath.
   */
  private TypedProperties sqlStatements;
  /**
   * Whether to wrap input streams into buffered input streams.
   */
  private final boolean bufferStreams = JPPFConfiguration.getProperties().getBoolean("jppf.job.persistence.bufferStreams", true);

  /**
   * Intialize this persistence with the {@linkplain #DEFAULT_TABLE default table name}.
   * @throws JobPersistenceException if any error occurs.
   */
  public DefaultDatabasePersistence() throws JobPersistenceException {
    this(DEFAULT_TABLE, DEFAULT_DATASOURCE);
  }

  /**
   * Intialize this persistence with a table name specified in the first string parmater.
   * @param params if parameters are provided, they have this meaning:
   * <ul style="margin-top: 0px">
   * <li>params[0] is the table name, which defaults to 'JOB_PERSISTENCE'</li>
   * <li>params[1] is the name of a datasource defined in the configuration, and defaults to 'job_persistence'</li>
   * </ul>
   * @throws JobPersistenceException if any error occurs.
   */
  public DefaultDatabasePersistence(final String...params) throws JobPersistenceException {
    try {
      String tableName = null;
      if ((params == null) || (params.length < 1) || (params[0] == null)) tableName = DEFAULT_TABLE;
      else tableName = params[0];
      if ((params == null) || (params.length < 2) || (params[1] == null)) dataSourceName = DEFAULT_DATASOURCE;
      else dataSourceName = params[1];
      checkTable(tableName);
      TypedProperties props = new TypedProperties();
      String path = getClass().getPackage().getName().replace('.', '/') + "/sql_statements.properties";
      ClassLoader cl = getClass().getClassLoader();
      if (debugEnabled) log.debug("loading SQL statements from path={}, with classloader={}", path, cl);
      try (Reader reader = new InputStreamReader(cl.getResourceAsStream(path), "utf-8")) {
        props.load(reader);
      }
      props.setString(TABLE_PROP, tableName);
      sqlStatements = new TypedProperties();
      try (Reader reader = new StringReader(props.asString())) {
        sqlStatements.loadAndResolve(reader);
      }
    } catch (Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public void store(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if (debugEnabled) log.debug("storing {}", infos);
    try (Connection connection = getDataSource().getConnection()) {
      boolean autocommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        for (PersistenceInfo info: infos) {
          try (PreparedStatement ps = prepareStoreStatement(connection, info)) {
            ps.executeUpdate();
          }
        }
        connection.commit();
      } catch(Exception e) {
        connection.rollback();
        throw new JobPersistenceException(e);
      } finally {
        connection.setAutoCommit(autocommit);
      }
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<InputStream> load(final Collection<PersistenceInfo> infos) throws JobPersistenceException {
    if (debugEnabled) log.debug("loading {}", infos);
    try (Connection connection = getDataSource().getConnection()) {
      boolean autocommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        List<InputStream> result = new ArrayList<>(infos.size());
        for (PersistenceInfo info: infos) {
          try (PreparedStatement ps = prepareLoadStatement(connection, info)) {
            try (ResultSet rs= ps.executeQuery()) {
              if (rs.next()) result.add(getInputStream(rs.getBinaryStream(1)));
            }
          }
        }
        connection.commit();
        return result;
      } catch(Exception e) {
        connection.rollback();
        throw e;
      } finally {
        connection.setAutoCommit(autocommit);
      }
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public List<String> getPersistedJobUuids() throws JobPersistenceException {
    try (Connection connection = getDataSource().getConnection()) {
      try (PreparedStatement ps = prepareGetAllUUidsStatement(connection)) {
        try (ResultSet rs= ps.executeQuery()) {
          List<String> uuids = new ArrayList<>();
          while (rs.next()) uuids.add(rs.getString(1));
          if (debugEnabled) log.debug("uuids of persisted jobs: {}", uuids);
          return uuids;
        }
      }
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public int[] getTaskPositions(final String jobUuid) throws JobPersistenceException {
    return getPositions(jobUuid, PersistenceObjectType.TASK);
  }

  @Override
  public int[] getTaskResultPositions(final String jobUuid) throws JobPersistenceException {
    return getPositions(jobUuid, PersistenceObjectType.TASK_RESULT);
  }

  /**
   * Get the  positions for all the objects of the specified type in the specified job.
   * @param jobUuid the uuid of the job for which to get the positions.
   * @param type the type of object for which to get the positions, one of {@link PersistenceObjectType#TASK TASK} or {@link PersistenceObjectType#TASK_RESULT TASK_RESULT}.
   * @return an array of int holding the positions.
   * @throws JobPersistenceException if any error occurs.
   */
  private int[] getPositions(final String jobUuid, final PersistenceObjectType type) throws JobPersistenceException {
    try (Connection connection = getDataSource().getConnection()) {
      try (PreparedStatement ps = prepareGetPositionsStatement(connection, jobUuid, type)) {
        try (ResultSet rs= ps.executeQuery()) {
          List<Integer> positions = new ArrayList<>();
          while (rs.next()) positions.add(rs.getInt(1));
          int[] result = new int[positions.size()];
          int i = 0;
          for (Integer n: positions) result[i++] = n;
          if (debugEnabled) log.debug(String.format("positions of %s for job uuid=%s : %s", type, jobUuid, StringUtils.buildString(", ", "{", "}", result)));
          return result;
        }
      }
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public void deleteJob(final String jobUuid) throws JobPersistenceException {
    if (debugEnabled) log.debug("deleting job with uuid = {}", jobUuid);
    try (Connection connection = getDataSource().getConnection();
      PreparedStatement ps = prepareDeleteJobStatement(connection, jobUuid)) {
      ps.executeUpdate();
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
  }

  @Override
  public boolean isJobPersisted(final String jobUuid) throws JobPersistenceException {
    try (Connection connection = getDataSource().getConnection()) {
      try (PreparedStatement ps = prepareJobHeaderCountStatement(connection, jobUuid)) {
        try (ResultSet rs= ps.executeQuery()) {
          if (rs.next()) {
            int n = rs.getInt(1);
            return n > 0;
          }
        }
      }
    } catch(JobPersistenceException e) {
      throw e;
    } catch(Exception e) {
      throw new JobPersistenceException(e);
    }
    return false;
  }

  /**
   * Get the JDBC data source.
   * @return a {@link DataSource} instance.
   * @throws SQLException if any error occurs.
   */
  protected synchronized DataSource getDataSource() throws SQLException {
    //if (dataSource == null) dataSource = new JDBCDataSource();
    if (dataSource == null) dataSource = JPPFDatasourceFactory.getInstance().getDataSource(dataSourceName);
    return dataSource;
  }

  /**
   * Determine whether an object is already s
   * @param connection the JDBC connection with which to create an dexecute the query.
   * @param info the information on the object to check.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private boolean alreadyExists(final Connection connection, final PersistenceInfo info) throws Exception {
    try (PreparedStatement ps = connection.prepareStatement(getSQL("already.exists.sql"))) {
      ps.setString(1, info.getJobUuid());
      ps.setString(2, info.getType().name());
      ps.setInt(3, info.getTaskPosition());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) return rs.getInt(1) == 1;
      }
    }
    return false;
  }

  /**
   * Create a prepared statement which will insert or update an object n the database.
   * @param connection the JDBC connection with which to create an dexecute the statement.
   * @param info the information on the object to persist.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private PreparedStatement prepareStoreStatement(final Connection connection, final PersistenceInfo info) throws Exception {
    boolean exists = alreadyExists(connection, info);
    PreparedStatement ps = connection.prepareStatement(getSQL(exists ? "store.update.sql" : "store.insert.sql"));
    InputStream is = getInputStream(info.getInputStream());
    if (exists) {
      ps.setBlob(1, is);
      ps.setString(2, info.getJobUuid());
      ps.setString(3, info.getType().name());
      ps.setInt(4, info.getTaskPosition());
    } else {
      ps.setString(1, info.getJobUuid());
      ps.setString(2, info.getType().name());
      ps.setInt(3, info.getTaskPosition());
      ps.setBlob(4, is);
    }
    return ps;
  }

  /**
   * Create a prepared statement which will insert or update an object n the database.
   * @param connection the JDBC connection with which to create an dexecute the statement.
   * @param info the information on the object to persist.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private PreparedStatement prepareLoadStatement(final Connection connection, final PersistenceInfo info) throws Exception {
    PreparedStatement ps = connection.prepareStatement(getSQL("load.sql"));
    ps.setString(1, info.getJobUuid());
    ps.setString(2, info.getType().name());
    ps.setInt(3, info.getTaskPosition());
    return ps;
  }

  /**
   * Create a prepared statement which obtain the positions of all tasks or task results for the specified job.
   * @param connection the JDBC connection with which to create and execute the query.
   * @param uuid the uuid of the job for which to get the positions.
   * @param type the type of object for which to get the positions.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private PreparedStatement prepareGetPositionsStatement(final Connection connection, final String uuid, final PersistenceObjectType type) throws Exception {
    PreparedStatement ps = connection.prepareStatement(getSQL("get.positions.sql"));
    ps.setString(1, uuid);
    ps.setString(2, type.name());
    return ps;
  }

  /**
   * Create a prepared statement which obtain the positions of all tasks or task results for the specified job.
   * @param connection the JDBC connection with which to create and execute the query.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private PreparedStatement prepareGetAllUUidsStatement(final Connection connection) throws Exception {
    return connection.prepareStatement(getSQL("get.all.uuids.sql"));
  }

  /**
   * Create a prepared statement which obtain the positions of all tasks or task results for the specified job.
   * @param connection the JDBC connection with which to create and execute the statement.
   * @param uuid the uuid of the job to delete.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private PreparedStatement prepareDeleteJobStatement(final Connection connection, final String uuid) throws Exception {
    PreparedStatement ps = connection.prepareStatement(getSQL("delete.job.sql"));
    ps.setString(1, uuid);
    return ps;
  }

  /**
   * Create a prepared statement which counts the headers of the job with the specified uuid.
   * @param connection the JDBC connection with which to create and execute the statement.
   * @param uuid the uuid of the job to delete.
   * @return a {@link PreparedStatement}.
   * @throws Exception if any error occurs.
   */
  private PreparedStatement prepareJobHeaderCountStatement(final Connection connection, final String uuid) throws Exception {
    PreparedStatement ps = connection.prepareStatement(getSQL("exists.job.sql"));
    ps.setString(1, uuid);
    ps.setString(2, PersistenceObjectType.JOB_HEADER.name());
    return ps;
  }

  /**
   * Get the SQL statement or query for the specified key.
   * @param key the key for the sqkl to retrieve.
   * @return a string containing an SQL statement or query, opr {@code null} if the key could not be found.
   */
  private String getSQL(final String key) {
    return sqlStatements.getString(key, null);
  }

  /**
   * Check whether the persistence table exists and create it if it doesn't.
   * @param tableName the name of the persistence table.
   * @throws Exception if any error occurs.
   */
  private void checkTable(final String tableName) throws Exception {
    try (Connection conn = getDataSource().getConnection()) {
      try (ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
        while (rs.next()) {
          String name = rs.getString("TABLE_NAME");
          if ((name != null) && name.equalsIgnoreCase(tableName)) {
            if (debugEnabled) log.debug("table '{}' exists in the database", tableName);
            return;
          }
        }
      }
      if (debugEnabled) log.debug("table '{}' does not exist in the database, creating it", tableName);
      String path = JPPFConfiguration.get(JPPFProperties.JOB_PERSISTENCE_DDL_LOCATION);
      if (debugEnabled) log.debug("Read DDL file from {}", path);
      String sql = FileUtils.readTextFile(path).replace("${" + TABLE_PROP + "}", tableName);
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.executeUpdate();
      }
    }
  }

  /**
   * Optionally wrap the specified stream into a buffered input stream, if {@link #bufferStreams} is {@link true}.
   * @param is the stream to write.
   * @return either {@code is} if {@link #bufferStreams} is {@link false}, or a {@link BufferedInputStream} wrapping it otherwise.
   * @throws Exception if any error occurs.
   */
  private InputStream getInputStream(final InputStream is) throws Exception {
    return !bufferStreams || (is instanceof BufferedInputStream) ? is : new BufferedInputStream(is);
  }
}
