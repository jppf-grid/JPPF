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

package org.jppf.persistence;

import java.io.*;
import java.sql.*;
import java.util.Locale;

import javax.sql.DataSource;

import org.jppf.utils.*;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * A persistence implementation which stores data in a single database table.
 * @param <I> the type of data to persist.
 * @author Laurent Cohen
 * @since 6.0
 */
public abstract class AbstractDatabasePersistence<I> {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(AbstractDatabasePersistence.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Determines whether the trace level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean traceEnabled = log.isTraceEnabled();
  /**
   * Constant for an empty array of strings.
   * @exclude
   */
  protected static final String[] EMPTY_STRINGS = new String[0];
  /**
   * The default persistence table name.
   * @exclude
   */
  protected final String defaultTable;
  /**
   * The default persistence table name.
   * @exclude
   */
  protected final String defaultDatasource;
  /**
   * The porperty for the persistence table name.
   */
  protected static final String TABLE_PROP = "table";
  /**
   * The JDBC datasource.
   * @exclude
   */
  protected final DataSource dataSource;
  /**
   * The SQL statements used by this persistence implementation, loaded from a properties file in the classpath.
   * @exclude
   */
  protected TypedProperties sqlStatements;
  /**
   * The name of the DB table to use.
   * @exclude
   */
  protected String tableName;
  /**
   * The name of the datasource to use.
   * @exclude
   */
  protected String dataSourceName;
  /**
   * The property from which to get the location of the DDL file.
   * @exclude
   */
  protected final JPPFProperty<String> ddlProp;

  /**
   * Intialize this persistence with a table name specified in the first string parameter.
   * @param defaultTable the default table name.
   * @param defaultDatasource the default datasource name.
   * @param ddlProp the property from which to get the location of the DDL file.
   * @param params if parameters are provided, they have this meaning:
   * <ul style="margin-top: 0px">
   * <li>params[0] is the table name, which defaults to 'JOB_PERSISTENCE'</li>
   * <li>params[1] is the name of a datasource defined in the configuration, and defaults to 'job_persistence'</li>
   * </ul>
   * @throws Exception if any error occurs.
   * @exclude
   */
  public AbstractDatabasePersistence(final String defaultTable, final String defaultDatasource, final JPPFProperty<String> ddlProp, final String...params) throws Exception {
    this.defaultTable = defaultTable;
    this.defaultDatasource = defaultDatasource;
    this.ddlProp = ddlProp;
    if ((params == null) || (params.length < 1) || (params[0] == null)) tableName = defaultTable;
    else tableName = params[0];
    if ((params == null) || (params.length < 2) || (params[1] == null)) dataSourceName = defaultDatasource;
    else dataSourceName = params[1];
    this.dataSource = JPPFDatasourceFactory.getInstance().getDataSource(dataSourceName);
    if (dataSource == null) throw new IllegalArgumentException("datasource '" + dataSourceName + "' is undefined");
    checkTable(tableName);
    final TypedProperties props = new TypedProperties();
    final String path = getClass().getPackage().getName().replace('.', '/') + "/sql_statements.properties";
    final ClassLoader cl = getClass().getClassLoader();
    if (debugEnabled) log.debug("loading SQL statements from path={}, with classloader={}", path, cl);
    try (final Reader reader = new InputStreamReader(cl.getResourceAsStream(path), "utf-8")) {
      props.load(reader);
    }
    props.setString(TABLE_PROP, tableName);
    sqlStatements = new TypedProperties();
    try (final Reader reader = new StringReader(props.asString())) {
      sqlStatements.loadAndResolve(reader);
    }
  }

  /**
   * Lock the specified row for update, if it exists
   * @param connection the JDBC connection with which to create an dexecute the query.
   * @param info the job element corresponding to the SQL row to lock.
   * @return {@code true} if the row already exists (and therefore a lock is acquired), {@code false} otherwise.
   * @throws Exception if any error occurs.
   * @exclude
   */
  protected abstract boolean lockForUpdate(final Connection connection, final I info) throws Exception;

  /**
   * Create a prepared statement which will insert or update an object in the database.
   * @param connection the JDBC connection with which to create an dexecute the statement.
   * @param info the information on the object to persist.
   * @param bytes the serialized data, may be null.
   * @throws Exception if any error occurs.
   * @exclude
   */
  protected void storeElement(final Connection connection, final I info, final byte[] bytes) throws Exception {
    if (lockForUpdate(connection, info)) {
      updateElement(connection, info, bytes);
    } else {
      try {
        insertElement(connection, info, bytes);
      } catch (final SQLException e) {
        if ((e instanceof SQLIntegrityConstraintViolationException) || ((e.getMessage() != null) && e.getMessage().toLowerCase(Locale.US).contains("violation"))) {
          if (traceEnabled) log.trace("Insert of element failed with constraint violation, attempting update instead, element={}", info);
          updateElement(connection, info, bytes);
        } else throw e;
      }
    }
  }

  /**
   * Create a prepared statement which will insert or update an object in the database.
   * @param connection the JDBC connection with which to create an dexecute the statement.
   * @param info the information on the object to persist.
   * @param bytes the serialized object to persist.
   * @throws Exception if any error occurs.
   * @exclude
   */
  protected abstract void insertElement(final Connection connection, final I info, final byte[] bytes) throws Exception;
    
  /**
   * Create a prepared statement which will insert or update an object in the database.
   * @param connection the JDBC connection with which to create an dexecute the statement.
   * @param info the information on the object to persist.
   * @param bytes the serialized object to persist.
   * @throws Exception if any error occurs.
   * @exclude
   */
  protected abstract void updateElement(final Connection connection, final I info, final byte[] bytes) throws Exception;

  /**
   * Get the SQL statement or query for the specified key.
   * @param key the key for the sqkl to retrieve.
   * @return a string containing an SQL statement or query, opr {@code null} if the key could not be found.
   * @exclude
   */
  protected String getSQL(final String key) {
    return sqlStatements.getString(key, null);
  }

  /**
   * Check whether the persistence table exists and create it if it doesn't.
   * @param tableName the name of the persistence table.
   * @throws Exception if any error occurs.
   */
  private void checkTable(final String tableName) throws Exception {
    if (debugEnabled) log.debug("checking table {}", tableName);
    try (final Connection connection = dataSource.getConnection()) {
      final boolean autoCommit = connection.getAutoCommit();
      final int isolation  = connection.getTransactionIsolation();
      try {
        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM " + tableName)) {
          try (ResultSet rs = ps.executeQuery()) {
            connection.commit();
            return;
          }
        } catch(@SuppressWarnings("unused") final SQLException e) {
        }
        if (debugEnabled) log.debug(String.format("table '%s' does not exist in the database, creating it", tableName));
        
        try (PreparedStatement ps = connection.prepareStatement(getTableDDL(tableName))) {
          ps.executeUpdate();
        }
        connection.commit();
      } catch(final Exception e) {
        log.warn("failed to create table '{}', load-balancer persistence may not work: {}", tableName, ExceptionUtils.getMessage(e));
        connection.rollback();
      } finally {
        connection.setTransactionIsolation(isolation);
        connection.setAutoCommit(autoCommit);
      }
    }
  }

  /**
   * Get the DDL to create the table for jobs persistence.
   * @param tableName the name of the persistence table.
   * @return a (set of) DDL statement(s) to create the DDL as a single string.
   * @throws Exception if any error occurs.
   * load_balancer_persistence.sql
   */
  private String getTableDDL(final String tableName) throws Exception {
    final String path = JPPFConfiguration.get(ddlProp);
    final String ddl = FileUtils.readTextFile(path).replace("${" + TABLE_PROP + "}", tableName);
    if (debugEnabled) log.debug("Read DDL file from {} :\n{}", path, ddl);
    return ddl;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append("[tableName=").append(tableName).append(", dataSourceName=").append(dataSourceName).append(']').toString();
  }
}
