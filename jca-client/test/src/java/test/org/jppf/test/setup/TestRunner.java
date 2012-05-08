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
import java.util.*;

import org.jppf.utils.FileUtils;
import org.junit.runner.*;

/**
 * 
 * @author Laurent Cohen
 */
public class TestRunner
{
  /**
   * Run all the test classes specified in the file.
   * @param classNamesFile contains the names of test classes to run, one per line.
   * @return a <code>Result</code> object.
   */
  public static Result runTests(final String classNamesFile)
  {
    Result result = null;
    try
    {
      InputStream is = TestRunner.class.getClassLoader().getResourceAsStream(classNamesFile);
      if (is != null)
      {
        List<String> list = FileUtils.textFileAsLines(new InputStreamReader(is));
        List<Class<?>> classes = new ArrayList<Class<?>>(list.size());
        for (String name: list)
        {
          try
          {
            String s = name.trim();
            if ("".equals(s)) continue;
            Class<?> clazz = Class.forName(s);
            classes.add(clazz);
          }
          catch(Exception e)
          {
            e.printStackTrace();
          }
        }
        result = JUnitCore.runClasses(classes.toArray(new Class<?>[classes.size()]));
      }
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    return result;
  }
}
