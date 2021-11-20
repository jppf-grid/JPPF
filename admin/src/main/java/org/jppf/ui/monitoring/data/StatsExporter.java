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

package org.jppf.ui.monitoring.data;

/**
 * Interface for export of full statistics snapshots.
 * @author Laurent Cohen
 */
public interface StatsExporter {
  /**
   * Export in plain text format.
   */
  int TEXT = 1;
  /**
   * Export in csv format.
   */
  int CSV = 2;

  /**
   * Format all the fields in the server stats page.
   * @return a formatted plain text string containing all the fields and their values.
   */
  String formatAll();
}
