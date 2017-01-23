/*
 * JPPF.
 * Copyright (C) 2005-2016 JPPF Team.
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

package org.jppf.admin.web.settings;

import java.io.*;

import org.jppf.utils.*;

/**
 * 
 * @author Laurent Cohen
 */
public abstract class AbstractPersistence implements Persistence {
  @Override
  public TypedProperties loadProperties(final String name) throws Exception {
    TypedProperties settings = new TypedProperties();
    String s = loadString(name);
    if (s != null) {
      try (Reader reader = new StringReader(s)) {
        settings.loadAndResolve(reader);
      }
    } else settings.putAll(JPPFConfiguration.getProperties());
    return settings;
  }

  @Override
  public void saveProperties(final String name, final TypedProperties settings) throws Exception {
    saveString(name, settings.asString());
  }

  @Override
  public void close() {
  }
}
