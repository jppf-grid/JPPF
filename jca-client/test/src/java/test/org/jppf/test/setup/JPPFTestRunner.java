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

package test.org.jppf.test.setup;

import java.io.*;
import java.net.*;
import java.util.*;

import org.jppf.utils.FileUtils;
import org.junit.runner.*;

/**
 * 
 * @author Laurent Cohen
 */
public class JPPFTestRunner
{
  /**
   * Run all the test classes specified in the file.
   * @param classNamesFile contains the names of test classes to run, one per line.
   * @return a <code>ResultHolder</code> object.
   */
  public ResultHolder runTests(final String classNamesFile)
  {
    ResultHolder result = new ResultHolder();
    InputStream is = null;
    try
    {
      is = JPPFTestRunner.class.getClassLoader().getResourceAsStream(classNamesFile);
      if (is != null)
      {
        List<String> list = FileUtils.textFileAsLines(new InputStreamReader(is));
        List<Class<?>> classes = new ArrayList<Class<?>>(list.size());
        for (String name: list)
        {
          String s = name.trim();
          if ("".equals(s)) continue;
          try
          {
            System.out.println("instantiating " + s);
            Class<?> clazz = Class.forName(s);
            classes.add(clazz);
          }
          catch(Exception e)
          {
            result.addException(new ExceptionHolder(s, e));
          }
        }
        System.out.println("test classes " + classes);
        JUnitCore core = new JUnitCore();
        TestRunListener listener = new TestRunListener(result);
        core.addListener(listener);
        core.run(classes.toArray(new Class<?>[classes.size()]));
      }
      else result.addException(new ExceptionHolder(classNamesFile, new IllegalArgumentException("class names file not found")));
    }
    catch(Exception e)
    {
      result.addException(new ExceptionHolder("Exception while attempting to run the tests", e));
    }
    return result;
  }

  /**
   * Serialize the test results to an output stream.
   * @param result the result of the tests execution.
   * @param out the output stream to serialize the rssults to.
   * @throws Exception if any error occurs.
   */
  public void sendResults(final ResultHolder result, final OutputStream out) throws Exception
  {
    ObjectOutputStream oos = new ObjectOutputStream(out);
    oos.writeObject(result);
    oos.flush();
  }

  /**
   * Send a request to run the unit tezsts to the specified web application.
   * @param webAppUrl the URL for the web application.
   * @return a {@link ResultHolder} object holding the test results.
   * @throws Exception if any error occurs.
   */
  public ResultHolder sendTestRequest(final URL webAppUrl) throws Exception
  {
    HttpURLConnection conn = (HttpURLConnection) webAppUrl.openConnection();
    conn.setDoOutput(true);
    conn.setRequestMethod("GET");
    conn.connect();
    InputStream in = conn.getInputStream();
    ObjectInputStream ois = new ObjectInputStream(in);
    Object o = ois.readObject();
    return (ResultHolder) o;
  }

  /**
   * Run the tests on a remote application server and display the results.
   * @param args args[0] must be the URL of the remote web application.
   */
  public static void main(final String[] args)
  {
    try
    {
      System.out.println("Running tests at " + args[0]);
      URL url = new URL(args[0]);
      ResultHolder result = new JPPFTestRunner().sendTestRequest(url);
      TestResultRenderer renderer = new TextResultRenderer(result);
      renderer.render();
      //System.out.println("test results: " + result);
      System.out.println(renderer.getHeader() + renderer.getBody());
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
  }
}
