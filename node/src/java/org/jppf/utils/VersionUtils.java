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

import java.io.InputStream;
import java.net.*;

import org.jppf.utils.streams.StreamUtils;
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
   * The current JPPF build number.
   */
  private static int buildNumber = -1;
  /**
   * IP address of the current host.
   */
  private static String ip = getLocalIpAddress();
  /**
   * 
   */
  private static final String VERSION_INFO = readVersionInfo();

  /**
   * Instantiation of this class is not permitted.
   */
  private VersionUtils()
  {
  }

  /**
   * Determine the current JPPF build number.
   * @return the number found in the classpath, or 0 if it is not found.
   */
  public static int getBuildNumber()
  {
    if (buildNumber < 0)
    {
      InputStream is = null;
      try
      {
        is = VersionUtils.class.getClassLoader().getResourceAsStream("build.number");
        if (is == null) buildNumber = 0;
        else
        {
          TypedProperties props = new TypedProperties();
          props.load(is);
          buildNumber = props.getInt("build.number");
        }
      }
      catch(Exception ignored)
      {
        buildNumber = 0;
      }
      finally
      {
        StreamUtils.close(is, log);
      }
    }
    return buildNumber;
  }

  /**
   * Set the current JPPF build number.
   * @param buildNumber the build number to set.
   */
  public static void setBuildNumber(final int buildNumber)
  {
    VersionUtils.buildNumber = buildNumber;
  }

  /**
   * Get the IP address of the current host.<br>
   * @return the IP address as a string in the format &quot;a.b.c.d&quot;.
   */
  public static String getLocalIpAddress()
  {
    if (ip != null) return ip;
    try
    {
      InetAddress ip = InetAddress.getLocalHost();
      return ip.getHostAddress();
    }
    catch(UnknownHostException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Read the version information properties file and return the information in a single string.
   * @return a formatted string containing the JPPF version, build number and build date.
   */
  private static String readVersionInfo()
  {
    String result = null;
    TypedProperties props = new TypedProperties();
    try
    {
      InputStream is = VersionUtils.class.getClassLoader().getResourceAsStream("META-INF/jppf-version.properties");
      props.load(is);
      StringBuilder sb = new StringBuilder();
      sb.append("JPPF version information: ");
      sb.append("Version: ").append(props.getString("version.number", ""));
      sb.append(", Build number: ").append(props.getString("build.number", ""));
      sb.append(", Build date: ").append(props.getString("build.date", ""));
      result = sb.toString();
    }
    catch (Exception e)
    {
      String s = "JPPF version information could not be determined";
      if (debugEnabled) log.debug(s, e);
      else log.warn(s + ": " + ExceptionUtils.getMessage(e));
      result = s;
    }
    return result;
  }

  /**
   * Get the JPPF the version information.
   * @return a formatted string containing the JPPF version, build number and build date.
   */
  public static String getVersionInformation()
  {
    return VERSION_INFO;
  }

  /**
   * Log the process ID and version information for a JPPF node or driver.
   * @param component either "node" or "driver".
   * @param uuid the "node or driver uuid.
   */
  public static void printJPPFInformation(final String component, final String uuid)
  {
    int pid = SystemUtils.getPID();
    if (pid > 0) System.out.println(component + " process id: " + pid);
    String hrule = StringUtils.padRight("", '-', 80);
    log.info(hrule);
    log.info(VersionUtils.getVersionInformation());
    log.info("starting JPPF " + component + " with PID=" + pid + ", UUID=" + uuid);
    log.info(hrule);
  }
}
