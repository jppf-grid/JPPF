/*
 * JPPF.
 * Copyright (C) 2005-2018 JPPF Team.
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

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This location provides a way to download artifacts from Maven Central, using a maven-like artifact location specification.
 * <br><div class="note_tip">
 * <b>Note</b>: <i>instances of this class allow access to a single artifact and do not in any way handle Maven transitive dependencies.</i>
 * </div>
 * @author Laurent Cohen
 * @since 6.0
 */
public class MavenCentralLocation extends URLLocation {
  /**
   * Explicit serialVersionUID.
   */
  private static final long serialVersionUID = 1L;
  /**
   * The group id.
   */
  private String groupId;
  /**
   * The artifact id.
   */
  private final String artifactId;
  /**
   * The version string.
   */
  private final String version;
  /**
   * The type of packaging.
   */
  private final String packaging;

  /**
   * Create this maven central location with the specified GAV string and packaging.
   * <p>Example:
   * <pre>Location&lt;URL&gt; location = new MavenCentralLocation("org.jppf:jppf-admin:5.2.9", "jar");</pre>
   * @param gav the maven identifier in format "groupId:artifactId:version".
   * @param packaging the type of packaging of the artifact: jar, war, etc.
   * @throws MalformedURLException if resulting URL is incorrect.
   */
  public MavenCentralLocation(final String gav, final String packaging) throws MalformedURLException {
    super(convertToURL(gav, packaging));
    final String[] tokens = gav.split(":");
    this.groupId = tokens[0];
    this.artifactId = tokens[1];
    this.version = tokens[2];
    this.packaging = packaging;
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

  /**
   * Convert a maven artifact specification into a downloadable URL.
   * @param gav the maven identifier in format "groupId:artifactId:version".
   * @param packaging the type of packaging of the artifact: jar, war, etc.
   * @return an URL to a downloadable Maven artifact on Maven Central.
   * @throws MalformedURLException if resulting URL is incorrect.
   */
  private static URL convertToURL(final String gav, final String packaging) throws MalformedURLException {
    final String[] tokens = gav.split(":");
    if ((tokens == null) || (tokens.length != 3)) throw new IllegalArgumentException("malformed gav '" + gav + "'");
    final String path = tokens[0].replace(".", "/");
    return new URL(String.format("http://repo.maven.apache.org/maven2/%s/%s/%s/%s-%s.%s", path, tokens[1], tokens[2], tokens[1], tokens[2], packaging));
  }

  /**
   * Since it is not possible to upload data directly to Maven central, this method will always throw an {@code UnsupportedOperationException}.
   */
  @Override
  public OutputStream getOutputStream() throws Exception {
    throw new UnsupportedOperationException("Uploads to Maven central locations are not supported");
  }

  /**
   * Get the group id of the correpsonding Maven artifact.
   * @return the groupId string.
   */
  public String getGroupId() {
    return groupId;
  }

  /**
   * Get the artifact id of the correpsonding Maven artifact.
   * @return the artifactId string.
   */
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Get the version of the correpsonding Maven artifact.
   * @return the version string.
   */
  public String getVersion() {
    return version;
  }

  /**
   * Get the packaging of the correpsonding Maven artifact.
   * @return the packaging string.
   */
  public String getPackaging() {
    return packaging;
  }

  @Override
  public String toString() {
    return new StringBuilder(getClass().getSimpleName()).append('[')
      .append("gav=").append(groupId).append(':').append(artifactId).append(':').append(version)
      .append(", packaging =").append(packaging)
      .append(']').toString();
  }
}
