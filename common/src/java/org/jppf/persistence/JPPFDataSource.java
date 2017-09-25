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

import java.io.PrintWriter;
import java.sql.*;

import javax.sql.DataSource;

import org.jppf.utils.TypedProperties;
import org.jppf.utils.configuration.*;
import org.slf4j.*;

/**
 * A non-pooled, non-XA datasource that provides connections using the {@link DriverManager} API.
 * The JDBC driver and connection information are retrieved from the JPPF configuration, for example:
 * <pre style="padding: 5px 5px 5px 0px; display: inline-block; background-color: #E0E0F0">
 * <font color="green"># Datasource name</font>
 * jppf.datasource.&lt;configId&gt;.name = myDS
 * <font color="green"># JDBC driver class name</font>
 * jppf.datasource.&lt;configId&gt;.driver.class = com.mysql.jdbc.Driver
 * <font color="green"># Database connection user</font>
 * jppf.datasource.&lt;configId&gt;.user = testjppf
 * <font color="green"># Database connection password</font>
 * jppf.datasource.&lt;configId&gt;.password = testjppf
 * <font color="green"># Database connection url</font>
 * jppf.datasource.&lt;configId&gt;.url = jdbc:mysql://localhost:3306/testjppf</pre>
 * @author Laurent Cohen
 * @exclude
 */
public class JPPFDataSource implements DataSource {
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(JPPFDataSource.class);
  /**
   * Determines whether the debug level is enabled in the log configuration, without the cost of a method call.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * Default JDBC driver class name.
   */
  private static final JPPFProperty<String> DATASOURCE_DRIVER_CLASS = new StringProperty("driver.class", null, "driverClassName");
  /**
   * Default JDBC datasource connection user name.
   */
  private static final JPPFProperty<String> DATASOURCE_USER = new StringProperty("user", null, "username");
  /**
   * Default JDBC datasource connection password.
   */
  private static final JPPFProperty<String> DATASOURCE_PASSWORD = new StringProperty("password", null);
  /**
   * Default JDBC datasource connection url.
   */
  private static final JPPFProperty<String> DATASOURCE_URL = new StringProperty("url", null, "jdbcUrl");
  /**
   * Database user, password, connection url and driver class name.
   */
  private final String user, pwd, url, driverClass;

  /**
   * Initialize this datasource from the specified configuration properties.
   * @param config the configuration properties from which to get the connection information.
   * @throws SQLException if any error occurs.
   */
  public JPPFDataSource(final TypedProperties config) throws SQLException {
    try {
      this.driverClass = config.get(DATASOURCE_DRIVER_CLASS);
      ClassLoader cl = Thread.currentThread().getContextClassLoader();
      if (cl == null) cl = getClass().getClassLoader();
      Class.forName(driverClass, true, cl);
      this.user = config.get(DATASOURCE_USER);
      this.pwd = config.get(DATASOURCE_PASSWORD);
      this.url = config.get(DATASOURCE_URL);
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      throw new SQLException(e);
    }
  }

  /**
   * Initialize this datasource.
   * @param user database user.
   * @param pwd database user password.
   * @param url database connection url.
   * @param driverClass JDBC driver class name.
   * @throws SQLException if any error occurs.
   */
  public JPPFDataSource(final String user, final String pwd, final String url, final String driverClass) throws SQLException {
    try {
      this.driverClass = driverClass;
      Class.forName(driverClass);
      this.user = user;
      this.pwd = pwd;
      this.url = url;
    } catch (Exception e) {
      if (debugEnabled) log.debug(e.getMessage(), e);
      throw new SQLException(e);
    }
  }

  @Override
  public PrintWriter getLogWriter() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setLogWriter(final PrintWriter out) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public void setLoginTimeout(final int seconds) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public int getLoginTimeout() throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public <T> T unwrap(final Class<T> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public boolean isWrapperFor(final Class<?> iface) throws SQLException {
    throw new SQLFeatureNotSupportedException();
  }

  @Override
  public Connection getConnection() throws SQLException {
    return DriverManager.getConnection(url, user, pwd);
  }

  @Override
  public Connection getConnection(final String username, final String password) throws SQLException {
    return DriverManager.getConnection(url, username, password);
  }
}
