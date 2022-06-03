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

package test.org.jppf.persistence;

//import static org.junit.Assert.*;

import java.sql.*;

import javax.sql.DataSource;

import org.h2.tools.Script;
import org.jppf.node.protocol.AbstractTask;
import org.jppf.persistence.JPPFDatasourceFactory;

import com.zaxxer.hikari.HikariDataSource;

//import test.org.jppf.test.setup.common.BaseTestHelper;

/**
 * 
 * @author Laurent Cohen
 */
/**
 * A simple task that executes SQL statements and performs JUnit assertions.
 */
public class DBTask extends AbstractTask<String> {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * Name of the datasource to use.
   */
  private final String dsName;
  /**
   * Name of the generaed database dump file.
   */
  private final String dumpFileName;

  /**
   * 
   * @param dsName name of the datasource to use.
   * @param dumpFileName name of the generaed database dump file.
   */
  public DBTask(final String dsName, final String dumpFileName) {
    this.dsName = dsName;
    this.dumpFileName = dumpFileName;
  }

  @Override
  public void run() {
    final DataSource ds = JPPFDatasourceFactory.getInstance().getDataSource(dsName);
    try {
      checkHikariProperties(dsName, ds);
      try (Connection c = ds.getConnection()) {
        // insert records in the table
        final int nbRecords = 2 * 5; // we want a multiple of 2
        String sql = String.format("INSERT INTO %s (COL1, COL2, COL3) VALUES (? , ?, ?)", DataSourceConstants.TABLE_NAME);
        for (int i = 1; i <= nbRecords; i++) {
          try (final PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, i);
            ps.setString(2, "col2_" + i);
            ps.setString(3, "col3_" + i);
            ps.executeUpdate();
          }
        }
        Script.main("-url", DataSourceConstants.DB_URL, "-user", DataSourceConstants.DB_USER, "-password", DataSourceConstants.DB_PWD, "-script", dumpFileName);
        // fetch all records with an odd number for COL1
        sql = String.format("SELECT COL1, COL2, COL3 FROM %s WHERE COL1 %% 2 = 1 ORDER BY COL1", DataSourceConstants.TABLE_NAME);
        try (final PreparedStatement ps = c.prepareStatement(sql)) {
          try (final ResultSet rs = ps.executeQuery()) {
            int count = 0;
            while (rs.next()) {
              final int i = 2 * count + 1;
              assert i == rs.getInt(1);
              assert ("col2_" + i).equals(rs.getString(2));
              assert ("col3_" + i).equals(rs.getString(3));
              count++;
            }
            assert nbRecords / 2 == count;
          }
        }
        // delete all records from the table
        sql = String.format("DELETE FROM %s", DataSourceConstants.TABLE_NAME);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
          ps.executeUpdate();
        }
        // select all records and check that none is returned
        sql = String.format("SELECT * FROM %s", DataSourceConstants.TABLE_NAME);
        try (PreparedStatement ps = c.prepareStatement(sql)) {
          try (ResultSet rs = ps.executeQuery()) {
            assert !rs.next();
          }
        }
        setResult(DataSourceConstants.EXECUTION_SUCCESSFUL_MESSAGE);
      }
    } catch (final Exception e) {
      setThrowable(e);
    }
  }

  /**
   * Check the provided datasource is a HikariDataSource and that it has the proper configuration properties.
   * @param name the name of the datasource.
   * @param ds the datasource to check.
   * @throws Exception if any error occurs.
   */
  static void checkHikariProperties(final String name, final DataSource ds) throws Exception {
    assert ds != null;
    assert ds instanceof HikariDataSource;
    final HikariDataSource hds = (HikariDataSource) ds;
    assert name.equals(hds.getPoolName());
    assert DataSourceConstants.DB_DRIVER_CLASS.equals(hds.getDriverClassName());
    assert DataSourceConstants.DB_URL.equals(hds.getJdbcUrl());
    assert DataSourceConstants.DB_USER.equals(hds.getUsername());
    assert DataSourceConstants.DB_PWD.equals(hds.getPassword());
  }
}
