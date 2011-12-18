/*
 * JPPF.
 * Copyright (C) 2005-2011 JPPF Team.
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
package test.niochecks;

import org.jppf.client.JPPFClient;

/**
 * This is a template JPPF application runner.
 * @author Laurent Cohen
 */
public class OpenCloseClient
{
  /**
   * The JPPF client, handles all communications with the server.
   */
  private static JPPFClient jppfClient =  null;

  /**
   * The entry point for this application runner to be run from a Java command line.
   * @param args by default, we do not use the command line arguments.
   */
  public static void main(final String...args)
  {
    try
    {
      System.out.println("initial start and close");
      jppfClient = new JPPFClient("Always the same UUID");
      Thread.sleep(2000L);
    }
    catch(Exception e)
    {
      e.printStackTrace();
    }
    finally
    {
      if (jppfClient != null) jppfClient.close();
    }
  }
}
