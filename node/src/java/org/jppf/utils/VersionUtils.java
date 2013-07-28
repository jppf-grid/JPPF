/*
 * JPPF.
 * Copyright (C) 2005-2013 JPPF Team.
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
package org.jppf.utils;

import java.io.*;

import org.slf4j.*;

/**
 * This class provides a utility method to determine the JPPF build number available in the class path.<br>
 * It is used for the nodes to determine when their code is outdated, in which case they will automatically reload
 * their own code.
 * @author Laurent Cohen
 */
public final class VersionUtils
{
  /**
   * Logger for this class.
   */
  private static Logger log = LoggerFactory.getLogger(VersionUtils.class);
  /**
   * Determines whether debug log statements are enabled.
   */
  private static boolean debugEnabled = log.isDebugEnabled();
  /**
   * The singleton instance holding the version information.
   */
  private static final Version VERSION = createVersionInfo();

  /**
   * Instantiation of this class is not permitted.
   */
  private VersionUtils()
  {
  }

  /**
   * Read the version information properties file and return the information in a dedicated object.
   * @return a {@link Version} instance.
   */
  private static Version createVersionInfo()
  {
    String result = null;
    TypedProperties props = new TypedProperties();
    Version v = null;
    try
    {
      InputStream is = VersionUtils.class.getClassLoader().getResourceAsStream("META-INF/jppf-version.properties");
      props.load(is);
      v = new Version(props.getString("version.number", ""), props.getString("build.number", ""), props.getString("build.date", ""));
    }
    catch (Exception e)
    {
      String s = "JPPF version information could not be determined";
      if (debugEnabled) log.debug(s, e);
      else log.warn(s + ": " + ExceptionUtils.getMessage(e));
      v = new Version(s, "", "");
    }
    return v;
  }

  /**
   * Log the JPPF version information and process id.
   * @param component the JPPF component type: driver, node or client.
   * @param uuid the ocmponent uuuid.
   */
  public static void logVersionInformation(final String component, final String uuid)
  {
    String comp = component == null ? "<unknown component type>" : component;
    int pid = SystemUtils.getPID();
    if (pid > 0) System.out.println(comp + " process id: " + pid);
    String hrule = StringUtils.padRight("", '-', 80);
    log.info(hrule);
    log.info(VersionUtils.VERSION.toString());
    log.info("starting "+ comp + " with PID=" + pid + ", UUID=" + uuid);
    log.info(hrule);
  }

  /**
   * Return the singleton object which provides the JPPF version information. 
   * @return a {@link Version} instance.
   */
  public static Version getVersion()
  {
    return VERSION;
  }

  /**
   * Describes the available version information.
   */
  public static class Version implements Serializable
  {
    /**
     * The JPPF version number.
     */
    private final String versionNumber;
    /**
     * The JPPF build number.
     */
    private final String buildNumber;
    /**
     * The JPPF build date.
     */
    private final String buildDate;

    /**
     * Initialize this version object.
     * @param versionNumber the JPPF version number.
     * @param buildNumber the JPPF build number.
     * @param buildDate the JPPF build date.
     */
    public Version(final String versionNumber, final String buildNumber, final String buildDate)
    {
      super();
      this.versionNumber = versionNumber;
      this.buildNumber = buildNumber;
      this.buildDate = buildDate;
    }

    /**
     * Get the JPPF version number.
     * @return the version number as a string.
     */
    public String getVersionNumber()
    {
      return versionNumber;
    }

    /**
     * Get the JPPF build number.
     * @return the build number as a string.
     */
    public String getBuildNumber()
    {
      return buildNumber;
    }

    /**
     * Get the JPPF build date.
     * @return the build date as a string.
     */
    public String getBuildDate()
    {
      return buildDate;
    }

    @Override
    public String toString()
    {
      StringBuilder sb = new StringBuilder();
      sb.append("JPPF Version: ").append(versionNumber);
      sb.append(", Build number: ").append(buildNumber);
      sb.append(", Build date: ").append(buildDate);
      return sb.toString();
    }
  }
}
