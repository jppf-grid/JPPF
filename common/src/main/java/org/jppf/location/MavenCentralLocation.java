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

package org.jppf.location;

import java.net.MalformedURLException;

/**
 * This location provides a way to download artifacts from Maven Central, using a maven-like artifact location specification.
 * <br><div class="note_tip">
 * <b>Note</b>: <i>instances of this class allow access to a single artifact and do not in any way handle Maven transitive dependencies.</i>
 * </div>
 * @author Laurent Cohen
 * @since 6.0
 */
public class MavenCentralLocation extends MavenLocation {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The URL for the Maven Central repository.
   */
  public static final String MAVEN_CENTRAL_URL = "https://repo1.maven.org/maven2";

  /**
   * Create this maven central location with the specified GAV string and packaging.
   * <p>Example:
   * <pre>Location&lt;URL&gt; location = new MavenCentralLocation("org.jppf:jppf-admin:5.2.9", "jar");</pre>
   * @param gav the maven identifier in format "groupId:artifactId:version".
   * @param packaging the type of packaging of the artifact: jar, war, etc.
   * @throws MalformedURLException if resulting URL is incorrect.
   */
  public MavenCentralLocation(final String gav, final String packaging) throws MalformedURLException {
    super(MAVEN_CENTRAL_URL, gav, packaging);
  }

  /**
   * Create this maven central location with the specified GAV string and default "jar" packaging.
   * <p>Example:
   * <pre>Location&lt;URL&gt; location = new MavenCentralLocation("org.jppf:jppf-admin:5.2.9");</pre>
   * @param gav the maven identifier in format "groupId:artifactId:version".
   * @throws MalformedURLException if resulting URL is incorrect.
   */
  public MavenCentralLocation(final String gav) throws MalformedURLException {
    this(gav, "jar");
  }

  /**
   * Create this maven central location with the specified group id, artifact id, version and default "jar" packaging.
   * <p>Example:
   * <pre>Location&lt;URL&gt; location = new MavenCentralLocation("org.jppf", "jppf-admin", "5.2.9");</pre>
   * @param groupId the maven group id.
   * @param artifactId the maven artifact id.
   * @param version the maven version of the artifact.
   * @throws MalformedURLException if resulting URL is incorrect.
   */
  public MavenCentralLocation(final String groupId, final String artifactId, final String version) throws MalformedURLException {
    this(groupId + ":" + artifactId + ":" + version, "jar");
  }

  /**
   * Create this maven central location with the specified group id, artifact id, version and packaging.
   * <p>Example:
   * <pre>Location&lt;URL&gt; location = new MavenCentralLocation("org.jppf", "jppf-admin", "5.2.9", "jar");</pre>
   * @param groupId the maven group id.
   * @param artifactId the maven artifact id.
   * @param version the maven version of the artifact.
   * @param packaging the type of packaging of the artifact: jar, war, etc.
   * @throws MalformedURLException if resulting URL is incoreect.
   */
  public MavenCentralLocation(final String groupId, final String artifactId, final String version, final String packaging) throws MalformedURLException {
    this(groupId + ":" + artifactId + ":" + version, packaging);
  }
}
