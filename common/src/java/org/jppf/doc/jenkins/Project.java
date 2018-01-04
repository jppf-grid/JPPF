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

package org.jppf.doc.jenkins;

import java.util.*;

/**
 * 
 * @author Laurent Cohen
 */
public class Project {
  /**
   * The project name.
   */
  private final String name;
  /**
   * The list of retained builds.
   */
  private final List<Build> builds = new ArrayList<>();

  /**
   * Iitialize this project with the specified name.
   * @param name the project name.
   */
  public Project(final String name) {
    this.name = name;
  }

  /**
   * Get the name of this project.
   * @return the project name.
   */
  public String getName() {
    return name;
  }

  /**
   * Get the available builds.
   * @return a list of {@link Build} instances.
   */
  public List<Build> getBuilds() {
    return builds;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append('[').append("name=").append(name);
    for (final Build build: builds) sb.append("\n  ").append(build);
    sb.append("\n]");
    return sb.toString();
  }
}
