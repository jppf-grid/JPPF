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

package org.jppf.example.gridfailover;

import java.io.IOException;
import java.io.InputStream;

import org.jppf.utils.FileUtils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

/**
 * Utility methods for parsing the yaml file describing the drivers to connect to.
 * @author Laurent Cohen
 */
public class Utils {
  /**
   * Create a yaml object mapper with lax parsing, allowing to deserialize
   * the same yaml into connection info objects on the node and client side.
   * @return a new {@code ObjectMapper}.
   */
  static ObjectMapper createObjectMapper() {
    final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.disable(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    return mapper;
  }

  /**
   * Parse the specified yaml resource into an instancce of the specified class.
   * @param <T> the type of object to deserialize into.
   * @param path the path to the yaml file, as a classpath or file system resource.
   * @param clazz the class to deserialize into.
   * @return an instance of the class to desrialize into.
   * @throws IOException if any error occurs while reading or parsing the yaml file.
   */
  static <T> T parseYaml(final String path, final Class<T> clazz) throws IOException {
    final ObjectMapper mapper = Utils.createObjectMapper();
    final InputStream is = FileUtils.getFileInputStream(path);
    return mapper.readerFor(clazz).readValue(is);
  }
}
