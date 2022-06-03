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

package org.jppf.example.nodelifecycle.client;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import org.jppf.utils.FileUtils;

/**
 * Create the H2 database.
 * @author Laurent Cohen
 */
public class CreateDB {
  /**
   * Create the H2 database.
   * @param args not used.
   */
  public static void main(final String[] args) {
    try {
      Class.forName("org.h2.Driver");
      final Connection c = DriverManager.getConnection("jdbc:h2:tcp://localhost:9092/./jppf_samples;SCHEMA=PUBLIC", "sa", "");
      final String sql = FileUtils.readTextFile("./db/jppf_samples-h2.sql");
      final Statement stmt = c.createStatement();
      stmt.executeUpdate(sql);
      stmt.close();
      c.close();
      System.out.println("database created successfully");
    } catch (final Exception e) {
      e.printStackTrace();
    }
  }
}
