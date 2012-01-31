/*
 * JPPF.
 * Copyright (C) 2005-2012 JPPF Team.
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
   * The current JPPF build number.
   */
  private static int buildNumber = -1;
  /**
   * IP address of the current host.
   */
  private static String ip = getLocalIpAddress();

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
        TypedProperties props = new TypedProperties();
        props.load(is);
        buildNumber = props.getInt("build.number");
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
}
