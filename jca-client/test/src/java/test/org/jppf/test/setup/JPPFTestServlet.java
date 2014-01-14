/*
 * JPPF.
 * Copyright (C) 2005-2014 JPPF Team.
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

package test.org.jppf.test.setup;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.*;

import test.org.jppf.test.runner.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFTestServlet extends HttpServlet
{
  @Override
  protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
  {
    perform(request, response);
  }

  @Override
  protected void doPost(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
  {
    perform(request, response);
  }

  /**
   * Perform the JUnit tests and send the result to the client.
   * @param request the http request.
   * @param response the http respons.
   * @throws ServletException if a servlet error occurs.
   * @throws IOException if an I/O error occurs.
   */
  private void perform(final HttpServletRequest request, final HttpServletResponse response) throws ServletException, IOException
  {
    JPPFTestRunner runner = new JPPFTestRunner();
    ResultHolder result = runner.runTests("TestClasses.txt", null);
    try
    {
      String remoteClient = request.getParameter("remoteClient");
      if (remoteClient != null)
      {
        // send the results to the remote java client
        runner.sendResults(result, response.getOutputStream());
      }
      else
      {
        // render the results in a web page.
      }
    }
    catch (Exception e)
    {
      throw (e instanceof IOException) ? (IOException) e : new IOException(e);
    }
  }
}
