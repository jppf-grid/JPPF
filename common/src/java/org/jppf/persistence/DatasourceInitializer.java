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

import javax.sql.DataSource;

import org.jppf.utils.TypedProperties;

/**
 * 
 * @author Laurent Cohen
 * @exclude
 */
public interface DatasourceInitializer {
  /**
   * Create a datasource from the specified configuration proeprties. The configId is used to build a prefix for
   * the relevant property names, in the format {@code jppf.datasource.<configId>.<property_name> = <value>}.
   * If configId is {@code null}, then no prefix is applied.
   * @param props the datasource properties.
   * @param configId the identifier of the datasource in the configuration.
   * @return a {@link DataSource} instance.
   */
  DataSource createDataSource(final TypedProperties props, final String configId);

  /**
   * Close the specified datasource and release the resources it is using.
   * @param datasource the datasource to close.
   */
  void close(DataSource datasource);
}
